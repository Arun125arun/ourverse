package com.lovenote.app.us

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.ui.Avatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val MOOD_EMOJIS = listOf("🥰", "😊", "😐", "😔", "😤", "😴")

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

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMemoriesClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("📖 Our story")
                }
                OutlinedButton(
                    onClick = onTodosClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("📋 To-dos")
                }
            }

            Spacer(Modifier.height(24.dp))

            MoodSection(
                myMood = moods[repository.myUid]?.takeIf { it.dateKey == today },
                partnerMood = moods.entries
                    .firstOrNull { it.key != repository.myUid }
                    ?.value
                    ?.takeIf { it.dateKey == today },
                onPick = { emoji -> scope.launch { runCatching { repository.setMood(emoji) } } },
            )

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
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\u2764",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = "Add birthdays, anniversaries, or your next date night \u2014 " +
                            "and count down together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
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

@Composable
private fun MoodSection(
    myMood: Mood?,
    partnerMood: Mood?,
    onPick: (String) -> Unit,
) {
    Text(
        text = "How are you feeling today?",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MOOD_EMOJIS.forEach { emoji ->
            val selected = myMood?.emoji == emoji
            Column(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        CircleShape,
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape,
                    )
                    .clickable { onPick(emoji) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(emoji, fontSize = 22.sp)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = partnerMood?.let { "They're feeling ${it.emoji} today" }
            ?: "They haven't checked in yet today",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
    )
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
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's question",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
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
