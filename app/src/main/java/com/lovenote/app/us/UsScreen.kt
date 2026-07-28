package com.lovenote.app.us

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.chat.VoicePlayer
import com.lovenote.app.chat.VoiceRecorder
import com.lovenote.app.ui.Avatar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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
    LaunchedEffect(countdown?.targetMillis) {
        while (countdown != null && countdown!!.targetMillis > System.currentTimeMillis()) {
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
        topBar = { TopAppBar(title = { Text("Us ❤") }) },
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

            // Shared Color Theme picker
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Our color:",
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
                                text = "$streak day streak!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = when {
                                    streak >= 365 -> "A year of connection! You two are unstoppable."
                                    streak >= 100 -> "100 days! Your bond is incredible."
                                    streak >= 30 -> "A whole month! Keep it going."
                                    streak >= 7 -> "A whole week! You're building something beautiful."
                                    else -> "Keep checking in daily to grow your streak."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.65f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMemoriesClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("\uD83D\uDCD6 Our story")
                }
                OutlinedButton(
                    onClick = onTodosClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("\uD83D\uDCCB To-dos")
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
                                text = "Memory Lane",
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

            // Voice Letters section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\uD83D\uDC8C Voice Letters",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
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
                }) {
                    if (recording) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Stop recording",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = "${VoiceRecorder.MAX_SECONDS - recordSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Record a voice letter",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (recording) {
                val infiniteTransition = rememberInfiniteTransition(label = "rec")
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.8f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "pulse",
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(pulse)
                                .background(MaterialTheme.colorScheme.error, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recording\u2026",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = "${recordSeconds}s / ${VoiceRecorder.MAX_SECONDS}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                            )
                        }
                        // Mini waveform bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(5) { i ->
                                val barHeight by infiniteTransition.animateFloat(
                                    initialValue = 6f,
                                    targetValue = 18f + (i * 3),
                                    animationSpec = infiniteRepeatable(
                                        tween(300 + i * 100),
                                        RepeatMode.Reverse,
                                    ),
                                    label = "bar$i",
                                )
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight.dp)
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                            RoundedCornerShape(2.dp),
                                        ),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (voiceLetters.isEmpty() && !recording) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("\uD83C\uDF99\uFE0F", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                        Text(
                            text = "Record a voice letter to send love.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            voiceLetters.forEach { letter ->
                val isMine = letter.senderUid == repository.myUid
                val senderName = if (isMine) me?.name?.substringBefore(' ') ?: "Me" else partner?.name?.substringBefore(' ') ?: "Partner"
                val timeAgo = voiceLetterTimeAgo(letter.createdAtMillis)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            val nowPlaying = VoicePlayer.toggle(
                                context, letter.id, letter.audioBase64,
                            ) { playingVoiceId = null }
                            playingVoiceId = if (nowPlaying) letter.id else null
                        }, modifier = Modifier.size(36.dp)) {
                            Text(
                                text = if (playingVoiceId == letter.id) "\u23F8" else "\u25B6",
                                fontSize = 18.sp,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = senderName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (letter.caption.isNotBlank()) {
                                Text(
                                    text = letter.caption,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                            Text(
                                text = "${letter.durationSec}s \u00B7 $timeAgo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Question Roulette
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\uD83E\uDE86 Question Roulette",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    scope.launch {
                        val q = repository.nextRouletteQuestion()
                        rouletteQuestion = q
                        rouletteAnswer = ""
                        showRouletteAnswer = true
                    }
                }) {
                    Text("\uD83C\uDFB2", fontSize = 26.sp)
                }
            }
            val rouletteAnswers = (rouletteState["answers"] as? Map<*, *>).orEmpty()
            if (rouletteAnswers.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Last answers:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(6.dp))
                        rouletteAnswers.entries.forEach { (uid, value) ->
                            val entry = value as? Map<*, *> ?: return@forEach
                            val ans = entry["answer"] as? String ?: ""
                            val isMine = uid == repository.myUid
                            val name = if (isMine) me?.name?.substringBefore(' ') ?: "Me" else partner?.name?.substringBefore(' ') ?: "Partner"
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = if (isMine) "\uD83D\uDC64" else "\uD83D\uDC65",
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                    Text(
                                        text = ans.take(100),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Shared Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\u23F0 Countdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    if (countdown != null) {
                        scope.launch { repository.clearCountdown() }
                    } else {
                        countdownTitle = ""
                        showCountdownPicker = true
                    }
                }) {
                    if (countdown != null) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear countdown")
                    } else {
                        Icon(Icons.Filled.Add, contentDescription = "Set a countdown")
                    }
                }
            }
            countdown?.let { cd ->
                countdownTick // read to trigger recomposition every minute
                val remainingMs = cd.targetMillis - System.currentTimeMillis()
                if (remainingMs > 0) {
                    val days = remainingMs / (1000L * 60 * 60 * 24)
                    val hours = (remainingMs % (1000L * 60 * 60 * 24)) / (1000L * 60 * 60)
                    val minutes = (remainingMs % (1000L * 60 * 60)) / (1000L * 60)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = cd.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CountdownUnit(value = days, label = "days", highlight = true)
                                CountdownUnit(value = hours, label = "hrs", highlight = false)
                                CountdownUnit(value = minutes, label = "min", highlight = false)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "\u2764\uFE0F",
                                fontSize = 20.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Special dates",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showAddEvent = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add a special date")
                }
            }
            if (events.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "\u2764\uFE0F",
                            fontSize = 22.sp,
                            modifier = Modifier.padding(end = 10.dp),
                        )
                        Column {
                            Text(
                                text = "No dates yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Add birthdays, anniversaries, or your next date night.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            } else {
                events.forEach { event ->
                    EventRow(
                        event = event,
                        onDelete = {
                            scope.launch { runCatching { repository.deleteEvent(event.id) } }
                        },
                    )
                }
            }
        }
    }

    if (showAddEvent) {
        AddEventDialog(
            onDismiss = { showAddEvent = false },
            onAdd = { title, millis ->
                showAddEvent = false
                scope.launch { runCatching { repository.addEvent(title, millis) } }
            },
        )
    }
    if (showCaptionDialog && pendingAudio != null) {
        var captionText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showCaptionDialog = false
                pendingAudio = null
            },
            title = { Text("Add a caption (optional)") },
            text = {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("What's this letter about?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val audio = pendingAudio!!
                    showCaptionDialog = false
                    pendingAudio = null
                    scope.launch {
                        runCatching { repository.sendVoiceLetter(audio.first, audio.second, captionText) }
                    }
                }) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCaptionDialog = false
                    pendingAudio = null
                }) { Text("Cancel") }
            },
        )
    }
    if (showRouletteAnswer) {
        AlertDialog(
            onDismissRequest = { showRouletteAnswer = false },
            title = { Text(rouletteQuestion ?: "Question") },
            text = {
                OutlinedTextField(
                    value = rouletteAnswer,
                    onValueChange = { rouletteAnswer = it },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val q = rouletteQuestion
                    if (q != null) {
                        val idx = repository.rouletteQuestions.indexOf(q)
                        scope.launch {
                            runCatching { repository.submitRouletteAnswer(idx, rouletteAnswer) }
                        }
                    }
                    showRouletteAnswer = false
                }) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { showRouletteAnswer = false }) { Text("Skip") }
            },
        )
    }
    if (showCountdownPicker) {
        var cdPickDate by remember { mutableStateOf(false) }
        val cdDateState = rememberDatePickerState()
        val cdDateLabel = cdDateState.selectedDateMillis?.let {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "Pick a date"
        AlertDialog(
            onDismissRequest = { showCountdownPicker = false },
            title = { Text("Set a countdown") },
            text = {
                Column {
                    OutlinedTextField(
                        value = countdownTitle,
                        onValueChange = { countdownTitle = it },
                        label = { Text("What are we counting down to?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { cdPickDate = true }) { Text(cdDateLabel) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cdDateState.selectedDateMillis?.let { millis ->
                            showCountdownPicker = false
                            scope.launch {
                                runCatching { repository.setCountdown(countdownTitle, millis) }
                            }
                        }
                    },
                    enabled = countdownTitle.isNotBlank() && cdDateState.selectedDateMillis != null,
                ) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showCountdownPicker = false }) { Text("Cancel") }
            },
        )
        if (cdPickDate) {
            DatePickerDialog(
                onDismissRequest = { cdPickDate = false },
                confirmButton = {
                    TextButton(onClick = { cdPickDate = false }) { Text("OK") }
                },
            ) {
                DatePicker(state = cdDateState)
            }
        }
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
                text = "$days days together",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            MILESTONES.firstOrNull { milestone -> milestone > days }?.let { next ->
                val remaining = next - days
                Text(
                    text = if (remaining == 0L) {
                        "🎉 Today is day $next!"
                    } else {
                        "🎉 $next days in $remaining ${if (remaining == 1L) "day" else "days"}"
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
                text = "Daily Pulse",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "How are you feeling?",
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
                        text = "You're ${myMood.emoji} and ${myMood.statusWord}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else if (myMood != null) {
                Text(
                    text = "Tap a word below to add your status",
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

            Spacer(Modifier.height(8.dp))

            // Partner pulse
            val partnerText = partnerMood?.let { mood ->
                val word = mood.statusWord
                if (word != null) "They're ${mood.emoji} and $word" else "They're ${mood.emoji}"
            } ?: "They haven't checked in yet"
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
                text = "Today's question",
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
                        placeholder = { Text("Your answer…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onSubmit(draft) },
                        enabled = draft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Answer")
                    }
                    Text(
                        text = "Their answer unlocks when you've answered too.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                else -> {
                    AnswerBlock(label = "You", text = myAnswer)
                    Spacer(Modifier.height(10.dp))
                    if (partnerAnswer != null) {
                        AnswerBlock(label = "Them ❤", text = partnerAnswer)
                    } else {
                        Text(
                            text = "Waiting for their answer…",
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
                text = "Couple quiz 🎯",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(question.prompt, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            when {
                mine == null -> {
                    Text("Your pick:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    OptionGrid(question.options, myPick) { myPick = it }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "What did $partnerName pick?",
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
                        Text("Lock it in")
                    }
                }
                theirs == null -> {
                    Text(
                        "You picked “${opt(mine.answer)}” and guessed " +
                            "they'd pick “${opt(mine.guess)}”.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Waiting for $partnerName to play…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                else -> {
                    val iWasRight = mine.guess == theirs.answer
                    val theyWereRight = theirs.guess == mine.answer
                    Text(
                        "They picked: “${opt(theirs.answer)}”",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        if (iWasRight) {
                            "Your guess was right! 🎉"
                        } else {
                            "You guessed “${opt(mine.guess)}” — not quite 😅"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (theyWereRight) {
                            "$partnerName guessed your pick correctly too ❤"
                        } else {
                            "$partnerName thought you'd pick " +
                                "“${opt(theirs.guess)}” 😄"
                        },
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
private fun EventRow(event: CoupleEvent, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(Date(event.dateMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = countdownLabel(event.dateMillis),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

private fun countdownLabel(dateMillis: Long): String {
    val days = ((dateMillis - System.currentTimeMillis()) / 86_400_000.0)
    return when {
        days in -1.0..0.0 -> "today ❤"
        days > 0 -> "in ${days.toInt() + 1} days"
        else -> "${-days.toInt()} days ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()
    val dateLabel = dateState.selectedDateMillis?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
    } ?: "Pick a date"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a special date") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What is it? (e.g. Date night)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { pickDate = true }) { Text(dateLabel) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dateState.selectedDateMillis?.let { onAdd(title, it) }
                },
                enabled = title.isNotBlank() && dateState.selectedDateMillis != null,
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (pickDate) {
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = { pickDate = false }) { Text("OK") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
}

@Composable
private fun CountdownUnit(value: Long, label: String, highlight: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(
                if (highlight) Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                else Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            ),
    ) {
        Text(
            text = "%02d".format(value),
            style = if (highlight) MaterialTheme.typography.headlineLarge
            else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
        )
    }
}

private fun voiceLetterTimeAgo(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 172_800_000 -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}
