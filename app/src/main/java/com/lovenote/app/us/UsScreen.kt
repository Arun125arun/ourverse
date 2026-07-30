package com.lovenote.app.us

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.R
import com.lovenote.app.chat.VoicePlayer
import com.lovenote.app.chat.VoiceRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsScreen(
    vm: UsViewModel,
    onMemoriesClick: () -> Unit,
    onTodosClick: () -> Unit,
    onPingClick: () -> Unit,
) {
    val repository = vm.repository
    val scope = rememberCoroutineScope()
    val today = Questions.dateKey()
    val moods by repository.moods().collectAsState(initial = emptyMap())
    val answers by repository.answers(today).collectAsState(initial = emptyMap())
    val events by repository.events().collectAsState(initial = emptyList())
    val me by repository.myProfile().collectAsState(initial = null)
    val partner by repository.partnerProfile().collectAsState(initial = null)
    val quizEntries by repository.quizEntries(today).collectAsState(initial = emptyMap())
    val anniversary by repository.anniversaryMillis().collectAsState(initial = null)
    var showAddEvent by remember { mutableStateOf(false) }
    var memoryLane by remember { mutableStateOf<Memory?>(null) }
    LaunchedEffect(Unit) {
        memoryLane = repository.randomMemoryForToday()
    }
    val context = LocalContext.current
    val voiceLetters by repository.voiceLetters().collectAsState(initial = emptyList())
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    var showCaptionDialog by remember { mutableStateOf(false) }
    var pendingAudio by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val recorder = remember { VoiceRecorder(context) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    val rouletteState by repository.rouletteState().collectAsState(initial = emptyMap())
    val countdown by repository.countdownEvent().collectAsState(initial = null)
    var countdownTick by remember { mutableIntStateOf(0) }
    val currentCountdown = countdown
    LaunchedEffect(currentCountdown?.targetMillis) {
        while (currentCountdown != null && currentCountdown.targetMillis > System.currentTimeMillis()) {
            countdownTick++
            delay(60_000L)
        }
    }
    var showRouletteAnswer by remember { mutableStateOf(false) }
    var rouletteQuestion by remember { mutableStateOf<String?>(null) }
    var rouletteAnswer by remember { mutableStateOf("") }
    var showCountdownPicker by remember { mutableStateOf(false) }
    var countdownTitle by remember { mutableStateOf("") }
    val coupleColorName by repository.coupleColorName().collectAsState(initial = null)
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching { recorder.start() }.onSuccess {
                recording = true
                recordSeconds = 0
            }
        }
    }
    LaunchedEffect(recording) {
        while (recording) {
            delay(1000L)
            recordSeconds++
            if (recordSeconds >= VoiceRecorder.MAX_SECONDS) {
                val result = runCatching { recorder.stop() }.getOrNull()
                recording = false
                if (result != null) {
                    pendingAudio = result
                    showCaptionDialog = true
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.us_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            CoupleHero(
                me = me,
                partner = partner,
                anniversaryMillis = anniversary,
            )

            Spacer(Modifier.height(12.dp))

            // Quick ping row
            QuickPingRow(
                onPingClick = onPingClick,
                onSendPing = { type ->
                    scope.launch { runCatching { repository.sendPing(type) } }
                },
            )

            // Shared Color Theme picker
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.our_color_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(Modifier.width(8.dp))
                repository.colorOptions.forEach { opt ->
                    val selected = coupleColorName == opt.name
                    val dotScale by animateFloatAsState(
                        targetValue = if (selected) 1.25f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                        label = "dot",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .scale(dotScale)
                            .size(22.dp)
                            .border(
                                width = if (selected) 2.5.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else Color.Transparent,
                                shape = CircleShape,
                            )
                            .background(Color(opt.primary), CircleShape)
                            .clickable {
                                scope.launch { repository.setCoupleColor(opt.name) }
                            },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Streak celebration
            val streak by repository.connectionStreak().collectAsState(initial = 0)
            if (streak >= 2) {
                val streakScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
                    label = "streakScale",
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(streakScale),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when {
                                streak >= 365 -> "\uD83D\uDC51"
                                streak >= 100 -> "\uD83D\uDD25"
                                streak >= 30 -> "\u2728"
                                else -> "\uD83D\uDCAB"
                            },
                            fontSize = 32.sp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.streak_day, streak),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(when {
                                    streak >= 365 -> R.string.streak_365
                                    streak >= 100 -> R.string.streak_100
                                    streak >= 30 -> R.string.streak_30
                                    streak >= 7 -> R.string.streak_7
                                    else -> R.string.streak_default
                                }),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.65f),
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMemoriesClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.our_story_button))
                }
                OutlinedButton(
                    onClick = onTodosClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.todos_button))
                }
            }


            Spacer(Modifier.height(24.dp))

            MoodSection(
                myMood = moods[repository.myUid]?.takeIf { it.dateKey == today },
                partnerMood = moods.entries
                    .firstOrNull { it.key != repository.myUid }
                    ?.value
                    ?.takeIf { it.dateKey == today },
                onPick = { emoji, word -> scope.launch { runCatching { repository.setMood(emoji, word) } } },
            )

            // Memory Lane
            memoryLane?.let { memory ->
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\uD83D\uDCF8", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.memory_lane),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = memory.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                                .format(java.util.Date(memory.dateMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            DailyQuestionCard(
                question = Questions.forDate(today),
                myAnswer = answers[repository.myUid],
                partnerAnswer = answers.entries
                    .firstOrNull { it.key != repository.myUid }
                    ?.value,
                onSubmit = { text ->
                    scope.launch { runCatching { repository.submitAnswer(today, text) } }
                },
            )

            Spacer(Modifier.height(24.dp))

            QuizCard(
                question = QuizDeck.forDate(today),
                partnerName = partner?.name?.substringBefore(' ') ?: "them",
                mine = quizEntries[repository.myUid],
                theirs = quizEntries.entries
                    .firstOrNull { it.key != repository.myUid }
                    ?.value,
                onSubmit = { answer, guess ->
                    scope.launch { runCatching { repository.submitQuiz(today, answer, guess) } }
                },
            )

            Spacer(Modifier.height(24.dp))

            VoiceLettersSection(
                voiceLetters = voiceLetters,
                myUid = repository.myUid,
                me = me,
                partner = partner,
                recording = recording,
                recordSeconds = recordSeconds,
                playingVoiceId = playingVoiceId,
                showCaptionDialog = showCaptionDialog,
                pendingAudio = pendingAudio,
                context = context,
                onToggleRecording = {
                    if (recording) {
                        val result = runCatching { recorder.stop() }.getOrNull()
                        recording = false
                        if (result != null) {
                            pendingAudio = result
                            showCaptionDialog = true
                        }
                    } else {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onPlayVoice = { letter ->
                    val nowPlaying = VoicePlayer.toggle(
                        context, letter.id, letter.audioBase64,
                    ) { playingVoiceId = null }
                    playingVoiceId = if (nowPlaying) letter.id else null
                },
                onDismissCaption = {
                    showCaptionDialog = false
                    pendingAudio = null
                },
                onConfirmCaption = { caption ->
                    pendingAudio?.let { audio ->
                        repository.sendVoiceLetter(audio.first, audio.second, caption)
                    }
                },
            )

            Spacer(Modifier.height(24.dp))

            RouletteSection(
                rouletteState = rouletteState,
                myUid = repository.myUid,
                me = me,
                partner = partner,
                showAnswer = showRouletteAnswer,
                question = rouletteQuestion,
                answer = rouletteAnswer,
                onSpin = {
                    scope.launch {
                        val q = repository.nextRouletteQuestion()
                        rouletteQuestion = q
                        rouletteAnswer = ""
                        showRouletteAnswer = true
                    }
                },
                onAnswerChange = { rouletteAnswer = it },
                onSubmitAnswer = {
                    val q = rouletteQuestion
                    if (q != null) {
                        val idx = repository.rouletteQuestions.indexOf(q)
                        scope.launch {
                            runCatching { repository.submitRouletteAnswer(idx, rouletteAnswer) }
                        }
                    }
                    showRouletteAnswer = false
                },
                onDismissAnswer = { showRouletteAnswer = false },
            )

            Spacer(Modifier.height(24.dp))

            CountdownSection(
                countdown = countdown,
                countdownTick = countdownTick,
                showPicker = showCountdownPicker,
                pickerTitle = countdownTitle,
                onClearCountdown = { scope.launch { repository.clearCountdown() } },
                onShowPicker = { countdownTitle = ""; showCountdownPicker = true },
                onDismissPicker = { showCountdownPicker = false },
                onPickerTitleChange = { countdownTitle = it },
                onStartCountdown = { millis ->
                    showCountdownPicker = false
                    scope.launch { runCatching { repository.setCountdown(countdownTitle, millis) } }
                },
            )

            Spacer(Modifier.height(24.dp))

            TimeCapsuleSection(repository = repository)

            Spacer(Modifier.height(24.dp))

            SpecialDatesSection(
                events = events,
                showAddEvent = showAddEvent,
                onAddClick = { showAddEvent = true },
                onDeleteEvent = { id -> scope.launch { runCatching { repository.deleteEvent(id) } } },
                onDismissAddEvent = { showAddEvent = false },
                onConfirmAddEvent = { title, millis ->
                    showAddEvent = false
                    scope.launch { runCatching { repository.addEvent(title, millis) } }
                },
            )
        }
    }
}
