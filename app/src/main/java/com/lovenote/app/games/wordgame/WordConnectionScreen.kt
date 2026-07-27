package com.lovenote.app.games.wordgame

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.games.GameRepository

private data class WordRound(
    val prompt: String,
    val myWord: String = "",
    val theirWord: String = "",
) {
    val matched get() = myWord.equals(theirWord, ignoreCase = true) && myWord.isNotBlank()
}

private val PROMPTS = listOf(
    "A color", "An animal", "A food", "A city", "A movie",
    "A song", "A holiday destination", "A superpower", "A dessert", "A sport",
    "A flower", "A cartoon character", "A drink", "A season", "A musical instrument",
    "A hobby", "A book", "A TV show", "A vehicle", "A celebrity",
    "An emoji", "A feeling", "A place in our city", "A type of dance", "A breakfast item",
    "Something soft", "Something expensive", "Something funny", "A childhood toy", "A dream job",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordConnectionScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
    gameId: String? = null,
    gameRepository: GameRepository? = null,
    myUid: String = "",
) {
    val selectedPrompts = remember { PROMPTS.shuffled().take(8) }
    var phase by remember { mutableStateOf("intro") }
    var currentRound by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    val p1Answers = remember { mutableStateListOf<String>() }
    val p2Answers = remember { mutableStateListOf<String>() }

    fun nextRound() {
        if (currentRound < selectedPrompts.lastIndex) {
            currentRound++
            input = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Connection") },
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
                "intro" -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "🔤", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Word Connection", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "How well do you think alike? Both write the first word that comes to mind!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { phase = "p1" }) { Text("$myName goes first") }
                }

                "p1", "p2" -> {
                    val playerName = if (currentPhase == "p1") myName else partnerName
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(playerName + "'s turn", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(6.dp))
                        Text("${currentRound + 1} of ${selectedPrompts.size}", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(20.dp))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = selectedPrompts[currentRound],
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(24.dp),
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        TextField(
                            value = input,
                            onValueChange = { input = it.take(30) },
                            placeholder = { Text("Type your word...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (currentPhase == "p1") {
                                    p1Answers.add(input.trim())
                                    if (currentRound < selectedPrompts.lastIndex) {
                                        currentRound++
                                        input = ""
                                    } else {
                                        currentRound = 0
                                        input = ""
                                        phase = "p2"
                                    }
                                } else {
                                    p2Answers.add(input.trim())
                                    if (currentRound < selectedPrompts.lastIndex) {
                                        currentRound++
                                        input = ""
                                    } else {
                                        phase = "results"
                                    }
                                }
                            },
                            enabled = input.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Submit")
                        }
                    }
                }

                "results" -> {
                    val matches = selectedPrompts.indices.count {
                        it < p1Answers.size && it < p2Answers.size &&
                            p1Answers[it].equals(p2Answers[it], ignoreCase = true) &&
                            p1Answers[it].isNotBlank()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = "Results 🔤", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "$matches/${selectedPrompts.size}",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = if (matches >= 6) "Incredible connection! 💕"
                                    else if (matches >= 4) "Pretty in sync! 💕"
                                    else "Keep learning about each other! 💕",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        for (i in selectedPrompts.indices) {
                            val w1 = p1Answers.getOrNull(i) ?: ""
                            val w2 = p2Answers.getOrNull(i) ?: ""
                            val match = w1.equals(w2, ignoreCase = true) && w1.isNotBlank()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (match)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                ),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(selectedPrompts[i], style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("$myName: $w1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        Text("$partnerName: $w2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                p1Answers.clear()
                                p2Answers.clear()
                                currentRound = 0
                                input = ""
                                phase = "intro"
                            }) { Text("Play Again") }
                            OutlinedButton(onClick = onBack) { Text("Back") }
                        }
                    }
                }
            }
        }
    }
}
