package com.lovenote.app.us

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lovenote.app.ui.Avatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MOOD_EMOJIS = listOf("🥰", "😊", "😐", "😔", "😤", "😴")

private val PULSE_WORDS = listOf(
    "cozy", "grateful", "excited", "tired", "stressed",
    "happy", "creative", "calm", "lonely", "loved",
    "anxious", "peaceful", "hungry", "productive", "silly",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsScreen(
    repository: UsRepository,
    onMemoriesClick: () -> Unit,
    onTodosClick: () -> Unit,
    onPingClick: () -> Unit,
) {
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
                    val audio = pendingAudio!!
                    repository.sendVoiceLetter(audio.first, audio.second, caption)
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

@Composable
private fun CoupleHero(
    me: Profile?,
    partner: Profile?,
    anniversaryMillis: Long?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(name = me?.name ?: "", photoUrl = me?.photoUrl ?: "", size = 64.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = me?.name?.substringBefore(' ') ?: "",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = "\u2764",
                fontSize = 26.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(name = partner?.name ?: "", photoUrl = partner?.photoUrl ?: "", size = 64.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = partner?.name?.substringBefore(' ') ?: "",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        anniversaryMillis?.let {
            Spacer(Modifier.height(12.dp))
            val days = (System.currentTimeMillis() - it) / 86_400_000L + 1
            Text(
                text = stringResource(R.string.days_together, days),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            MILESTONES.firstOrNull { milestone -> milestone > days }?.let { next ->
                val remaining = next - days
                Text(
                    text = if (remaining == 0L) {
                        stringResource(R.string.milestone_today, next)
                    } else {
                        stringResource(R.string.milestone_upcoming, next, remaining, stringResource(if (remaining == 1L) R.string.milestone_day else R.string.milestone_days))
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

private val MILESTONES =
    listOf(100L, 200L, 300L, 365L, 500L, 730L, 1000L, 1461L, 1825L, 2000L, 3650L)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodSection(
    myMood: Mood?,
    partnerMood: Mood?,
    onPick: (String, String?) -> Unit,
) {
    var showWords by remember { mutableStateOf(false) }
    var selectedEmoji by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.daily_pulse),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.how_are_you_feeling),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))

            // Emoji row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MOOD_EMOJIS.forEach { emoji ->
                    val selected = myMood?.emoji == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent,
                                CircleShape,
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape,
                            )
                            .clickable {
                                selectedEmoji = emoji
                                if (myMood?.statusWord == null) {
                                    showWords = true
                                } else {
                                    onPick(emoji, myMood.statusWord)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }

            // Current pulse display
            Spacer(Modifier.height(10.dp))
            if (myMood != null && myMood.statusWord != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.mood_status_you, myMood.emoji, myMood.statusWord),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else if (myMood != null) {
                Text(
                    text = stringResource(R.string.tap_word_to_add_status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                )
            }

            // Status word grid (shown after picking emoji or on tap)
            if (showWords || (myMood?.emoji != null && myMood.statusWord == null)) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PULSE_WORDS.forEach { word ->
                        val isSelected = myMood?.statusWord == word
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.clickable {
                                onPick(selectedEmoji ?: myMood?.emoji ?: "😊", word)
                                showWords = false
                            },
                        ) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Partner pulse
            val partnerText = partnerMood?.let { mood ->
                val word = mood.statusWord
                if (word != null) stringResource(R.string.mood_status_partner_with_word, mood.emoji, word) else stringResource(R.string.mood_status_partner_emoji, mood.emoji)
            } ?: stringResource(R.string.partner_not_checked_in)
            Text(
                text = partnerText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun DailyQuestionCard(
    question: String,
    myAnswer: String?,
    partnerAnswer: String?,
    onSubmit: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.todays_question),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(14.dp))

            when {
                myAnswer == null -> {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(stringResource(R.string.your_answer_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onSubmit(draft) },
                        enabled = draft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.answer_button))
                    }
                    Text(
                        text = stringResource(R.string.partner_answer_unlocks),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                else -> {
                    AnswerBlock(label = stringResource(R.string.label_you), text = myAnswer)
                    Spacer(Modifier.height(10.dp))
                    if (partnerAnswer != null) {
                        AnswerBlock(label = stringResource(R.string.label_them_heart), text = partnerAnswer)
                    } else {
                        Text(
                            text = stringResource(R.string.waiting_for_answer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerBlock(label: String, text: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun QuizCard(
    question: QuizQuestion,
    partnerName: String,
    mine: QuizEntry?,
    theirs: QuizEntry?,
    onSubmit: (answer: Int, guess: Int) -> Unit,
) {
    var myPick by remember(question.prompt) { mutableStateOf<Int?>(null) }
    var myGuess by remember(question.prompt) { mutableStateOf<Int?>(null) }
    // Guard against out-of-range indexes (e.g. decks out of sync across versions).
    fun opt(index: Int): String = question.options.getOrNull(index) ?: "?"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.couple_quiz_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(question.prompt, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            when {
                mine == null -> {
                    Text(stringResource(R.string.your_pick), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    OptionGrid(question.options, myPick) { myPick = it }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.what_did_partner_pick, partnerName),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    OptionGrid(question.options, myGuess) { myGuess = it }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onSubmit(myPick!!, myGuess!!) },
                        enabled = myPick != null && myGuess != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.lock_it_in))
                    }
                }
                theirs == null -> {
                    Text(
                        stringResource(R.string.quiz_picked_and_guessed, opt(mine.answer), opt(mine.guess)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.quiz_waiting_for_partner, partnerName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                else -> {
                    val iWasRight = mine.guess == theirs.answer
                    val theyWereRight = theirs.guess == mine.answer
                    Text(
                        stringResource(R.string.quiz_they_picked, opt(theirs.answer)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(if (iWasRight) R.string.quiz_guess_right else R.string.quiz_guess_wrong, opt(mine.guess)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(if (theyWereRight) R.string.quiz_partner_guessed_right else R.string.quiz_partner_guessed_wrong, partnerName, opt(theirs.guess)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionGrid(
    options: List<String>,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEachIndexed { rowIndex, rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowOptions.forEachIndexed { colIndex, option ->
                    val index = rowIndex * 2 + colIndex
                    val isSelected = selected == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(index) },
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPingRow(
    onPingClick: () -> Unit,
    onSendPing: (PingType) -> Unit,
) {
    val quickPings = listOf(PingType.HEART, PingType.MISS, PingType.THINKING, PingType.STAR)
    var animPhase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { delay(100); animPhase = 1 }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\u26A1",
                fontSize = 18.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Quick ping",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.weight(1f))

            quickPings.forEachIndexed { index, type ->
                AnimatedVisibility(
                    visible = animPhase > 0,
                    enter = fadeIn(),
                ) {
                    var bounced by remember { mutableStateOf(false) }
                    val bounceScale by animateFloatAsState(
                        targetValue = if (bounced) 1.3f else 1f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
                        label = "pingBounce",
                    )
                    LaunchedEffect(bounced) {
                        if (bounced) { delay(250); bounced = false }
                    }
                    Text(
                        text = type.emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .scale(bounceScale)
                            .clickable {
                                bounced = true
                                onSendPing(type)
                            },
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.clickable(onClick = onPingClick),
            ) {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
