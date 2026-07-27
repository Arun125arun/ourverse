package com.lovenote.app.games.trivia

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class TriviaQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
)

private val QUESTIONS = listOf(
    TriviaQuestion("What's my favorite food?", listOf("Pizza", "Sushi", "Pasta", "Tacos"), 0),
    TriviaQuestion("My dream vacation?", listOf("Beach", "Mountain", "City trip", "Countryside"), 0),
    TriviaQuestion("Favorite movie genre?", listOf("Romance", "Comedy", "Action", "Horror"), 0),
    TriviaQuestion("How do I drink my coffee?", listOf("Black", "Latte", "Cappuccino", "Don't drink"), 1),
    TriviaQuestion("My love language?", listOf("Words", "Touch", "Gifts", "Quality Time"), 3),
    TriviaQuestion("My favorite season?", listOf("Spring", "Summer", "Fall", "Winter"), 1),
    TriviaQuestion("Ideal date night?", listOf("Dinner out", "Movie at home", "Walk in park", "Game night"), 0),
    TriviaQuestion("My morning routine?", listOf("Early bird", "Snooze 5x", "Coffee first", "Hit the gym"), 2),
    TriviaQuestion("Favorite music genre?", listOf("Pop", "R&B", "Indie", "Hip Hop"), 0),
    TriviaQuestion("My biggest fear?", listOf("Heights", "Spiders", "Dark", "Public speaking"), 3),
    TriviaQuestion("My go-to comfort show?", listOf("Friends", "The Office", "Grey's", "Breaking Bad"), 1),
    TriviaQuestion("How I take my pizza?", listOf("Pepperoni", "Margherita", "Hawaiian", "Veggie"), 0),
    TriviaQuestion("My shoe size approximate?", listOf("7-8", "9-10", "11-12", "13+"), 1),
    TriviaQuestion("Favorite holiday?", listOf("Christmas", "Birthday", "New Year", "Valentine's"), 0),
    TriviaQuestion("My pet peeve?", listOf("Lateness", "Mess", "Loud chewing", "Slow WiFi"), 1),
    TriviaQuestion("Dream car?", listOf("Tesla", "Porsche", "BMW", "Jeep"), 0),
    TriviaQuestion("My hidden talent?", listOf("Cooking", "Singing", "Drawing", "None"), 1),
    TriviaQuestion("Favorite dessert?", listOf("Cake", "Ice cream", "Chocolate", "Cheesecake"), 2),
    TriviaQuestion("My bedtime?", listOf("9 PM", "10 PM", "11 PM", "12+ AM"), 2),
    TriviaQuestion("My zodiac sign element?", listOf("Fire", "Earth", "Air", "Water"), 0),
    TriviaQuestion("Morning or night person?", listOf("Morning", "Night", "Both", "Neither"), 1),
    TriviaQuestion("My favorite color?", listOf("Blue", "Pink", "Green", "Black"), 0),
    TriviaQuestion("Ideal weekend?", listOf("Sleep in", "Adventure", "Netflix", "Friends"), 0),
    TriviaQuestion("My childhood nickname?", listOf("Buddy", "Sweetie", "Shorty", "Don't have one"), 1),
    TriviaQuestion("Favorite sport to watch?", listOf("Football", "Basketball", "Soccer", "None"), 0),
    TriviaQuestion("My signature dish?", listOf("Pasta", "Tacos", "Stir fry", "Can't cook"), 0),
    TriviaQuestion("How I say I love you?", listOf("Words", "Hugs", "Gifts", "Actions"), 3),
    TriviaQuestion("My ideal superpower?", listOf("Fly", "Teleport", "Read minds", "Time travel"), 1),
    TriviaQuestion("My phone screen time?", listOf("<2 hrs", "2-4 hrs", "4-6 hrs", "6+ hrs"), 2),
    TriviaQuestion("My love for you (1-10)?", listOf("10", "10", "10", "10"), 0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoupleTriviaScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
) {
    var phase by remember { mutableStateOf("player1_intro") }
    var questionIndex by remember { mutableIntStateOf(0) }
    var p1Answers by remember { mutableStateOf(List(QUESTIONS.size) { -1 }) }
    var p2Answers by remember { mutableStateOf(List(QUESTIONS.size) { -1 }) }
    var currentQuestion by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }

    val selectedQuestions = remember { QUESTIONS.shuffled().take(10) }
    val totalQ = selectedQuestions.size

    fun p1Score(): Int = selectedQuestions.indices.count {
        p1Answers[it] == selectedQuestions[it].correctIndex
    }
    fun p2Score(): Int = selectedQuestions.indices.count {
        p2Answers[it] == selectedQuestions[it].correctIndex
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Couple Trivia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "phase",
            modifier = Modifier.padding(padding),
        ) { currentPhase ->
            when (currentPhase) {
                "player1_intro" -> IntroCard(
                    title = "$myName's Turn",
                    subtitle = "Answer questions about yourself",
                    buttonLabel = "Start",
                    onClick = { phase = "player1" },
                )
                "player1" -> QuestionCard(
                    question = selectedQuestions[currentQuestion],
                    questionNum = currentQuestion + 1,
                    totalQuestions = totalQ,
                    selected = selectedOption,
                    onSelect = { selectedOption = it },
                    onSubmit = {
                        p1Answers = p1Answers.toMutableList().also { it[currentQuestion] = selectedOption }
                        selectedOption = -1
                        if (currentQuestion < totalQ - 1) {
                            currentQuestion++
                        } else {
                            currentQuestion = 0
                            phase = "player2_intro"
                        }
                    },
                )
                "player2_intro" -> IntroCard(
                    title = "$partnerName's Turn",
                    subtitle = "Answer questions about yourself",
                    buttonLabel = "Start",
                    onClick = { phase = "player2" },
                )
                "player2" -> QuestionCard(
                    question = selectedQuestions[currentQuestion],
                    questionNum = currentQuestion + 1,
                    totalQuestions = totalQ,
                    selected = selectedOption,
                    onSelect = { selectedOption = it },
                    onSubmit = {
                        p2Answers = p2Answers.toMutableList().also { it[currentQuestion] = selectedOption }
                        selectedOption = -1
                        if (currentQuestion < totalQ - 1) {
                            currentQuestion++
                        } else {
                            phase = "results"
                        }
                    },
                )
                "results" -> ResultsCard(
                    myName = myName,
                    partnerName = partnerName,
                    questions = selectedQuestions,
                    p1Answers = p1Answers,
                    p2Answers = p2Answers,
                    onRematch = {
                        p1Answers = List(QUESTIONS.size) { -1 }
                        p2Answers = List(QUESTIONS.size) { -1 }
                        currentQuestion = 0
                        selectedOption = -1
                        phase = "player1_intro"
                    },
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun IntroCard(title: String, subtitle: String, buttonLabel: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "💕",
            fontSize = 56.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClick) { Text(buttonLabel) }
    }
}

@Composable
private fun QuestionCard(
    question: TriviaQuestion,
    questionNum: Int,
    totalQuestions: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        LinearProgressIndicator(
            progress = { questionNum.toFloat() / totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Question $questionNum of $totalQuestions",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(20.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(20.dp),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.options.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { colIndex, option ->
                        val index = rowIndex * 2 + colIndex
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (selected == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelect(index) },
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected == index) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = selected >= 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (questionNum < totalQuestions) "Next" else "See Results")
        }
    }
}

@Composable
private fun ResultsCard(
    myName: String,
    partnerName: String,
    questions: List<TriviaQuestion>,
    p1Answers: List<Int>,
    p2Answers: List<Int>,
    onRematch: () -> Unit,
    onBack: () -> Unit,
) {
    val s1 = questions.indices.count { p1Answers[it] == questions[it].correctIndex }
    val s2 = questions.indices.count { p2Answers[it] == questions[it].correctIndex }
    val matched = questions.indices.count {
        p1Answers[it] == p2Answers[it] && p1Answers[it] >= 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Results 💕", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ScoreColumn(name = myName, score = s1, total = questions.size, color = MaterialTheme.colorScheme.primary)
            ScoreColumn(name = "Matched", score = matched, total = questions.size, color = MaterialTheme.colorScheme.tertiary)
            ScoreColumn(name = partnerName, score = s2, total = questions.size, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(20.dp))

        questions.forEachIndexed { i, q ->
            val p1 = p1Answers.getOrNull(i) ?: -1
            val p2 = p2Answers.getOrNull(i) ?: -1
            val bothCorrect = p1 == q.correctIndex && p2 == q.correctIndex
            val matchedAnswer = p1 == p2 && p1 >= 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        bothCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        matchedAnswer -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(q.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "You: ${q.options.getOrNull(p1) ?: "?"}  •  Them: ${q.options.getOrNull(p2) ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRematch) { Text("Play Again") }
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
    }
}

@Composable
private fun ScoreColumn(name: String, score: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, style = MaterialTheme.typography.labelMedium, color = color)
        Text(text = "$score/$total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
    }
}
