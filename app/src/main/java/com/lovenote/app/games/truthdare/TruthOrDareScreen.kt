package com.lovenote.app.games.truthdare

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.games.GameRepository
import kotlin.random.Random

private data class TruthDareItem(val type: String, val text: String, val difficulty: String)

private val TRUTHS = listOf(
    TruthDareItem("Truth", "What's your most embarrassing moment with me?", "Medium"),
    TruthDareItem("Truth", "What's a secret you've never told me?", "Hard"),
    TruthDareItem("Truth", "When did you first know you loved me?", "Easy"),
    TruthDareItem("Truth", "What's your favorite memory of us?", "Easy"),
    TruthDareItem("Truth", "Have you ever checked my phone?", "Medium"),
    TruthDareItem("Truth", "What's something I do that annoys you?", "Medium"),
    TruthDareItem("Truth", "What's your biggest insecurity about us?", "Hard"),
    TruthDareItem("Truth", "If you could change one thing about me, what?", "Hard"),
    TruthDareItem("Truth", "What's the sweetest thing I've done for you?", "Easy"),
    TruthDareItem("Truth", "Have you ever pretended to like a gift?", "Medium"),
    TruthDareItem("Truth", "What's a fantasy you've never told me?", "Hard"),
    TruthDareItem("Truth", "When do you feel most loved by me?", "Easy"),
    TruthDareItem("Truth", "What song reminds you of me?", "Easy"),
    TruthDareItem("Truth", "Have you ever stalked my ex on social media?", "Medium"),
    TruthDareItem("Truth", "What's your dream proposal scenario?", "Easy"),
    TruthDareItem("Truth", "What's a dealbreaker you haven't told me about?", "Hard"),
    TruthDareItem("Truth", "Do you believe in soulmates?", "Easy"),
    TruthDareItem("Truth", "What's something you're glad I don't know?", "Hard"),
)

private val DARES = listOf(
    TruthDareItem("Dare", "Give me a 60-second hug", "Easy"),
    TruthDareItem("Dare", "Serenade me with any song", "Medium"),
    TruthDareItem("Dare", "Do 10 push-ups right now", "Medium"),
    TruthDareItem("Dare", "Send me a voice note saying why you love me", "Easy"),
    TruthDareItem("Dare", "Speak in an accent for the next 5 minutes", "Easy"),
    TruthDareItem("Dare", "Let me post a photo of you on my story", "Medium"),
    TruthDareItem("Dare", "Dance with me for one full song", "Easy"),
    TruthDareItem("Dare", "Write me a love poem in 2 minutes", "Medium"),
    TruthDareItem("Dare", "Give me a foot massage for 3 minutes", "Easy"),
    TruthDareItem("Dare", "Act like our first date for the next 5 minutes", "Medium"),
    TruthDareItem("Dare", "Screenshot your search history and show me", "Hard"),
    TruthDareItem("Dare", "Let me style your hair however I want", "Medium"),
    TruthDareItem("Dare", "Make me breakfast in bed tomorrow", "Easy"),
    TruthDareItem("Dare", "Record yourself saying 10 things you love about me", "Medium"),
    TruthDareItem("Dare", "Hold my hand in public for the next hour", "Easy"),
    TruthDareItem("Dare", "Imitate me for 1 minute", "Medium"),
    TruthDareItem("Dare", "Go a full hour without your phone", "Hard"),
    TruthDareItem("Dare", "Draw a portrait of me in 3 minutes", "Medium"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruthOrDareScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
    gameId: String? = null,
    gameRepository: GameRepository? = null,
    myUid: String = "",
) {
    var currentTurn by remember { mutableIntStateOf(1) }
    var currentItem by remember { mutableStateOf<TruthDareItem?>(null) }
    var history by remember { mutableStateOf(listOf<Pair<String, TruthDareItem>>()) }
    var spins by remember { mutableIntStateOf(0) }

    fun spinWheel(choice: String) {
        val item = if (choice == "Truth") {
            TRUTHS[Random.nextInt(TRUTHS.size)]
        } else {
            DARES[Random.nextInt(DARES.size)]
        }
        currentItem = item
        history = history + ((if (currentTurn == 1) myName else partnerName) to item)
        spins++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Truth or Dare") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (currentTurn == 1) "$myName's turn" else "$partnerName's turn",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Round $spins",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                },
                label = "card",
                modifier = Modifier.weight(1f),
            ) { item ->
                if (item != null) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.type == "Truth")
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (item.type == "Truth") "💕 TRUTH" else "🔥 DARE",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (item.type == "Truth")
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            ) {
                                Text(
                                    text = "Difficulty: ${item.difficulty}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = "💕", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Choose Truth or Dare!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentItem != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = {
                            currentTurn = if (currentTurn == 1) 2 else 1
                            currentItem = null
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Done ✓")
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = { spinWheel("Truth") },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("💕 Truth", fontSize = 18.sp)
                    }
                    Button(
                        onClick = { spinWheel("Dare") },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) {
                        Text("🔥 Dare", fontSize = 18.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (history.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("History", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(4.dp))
                        history.takeLast(3).forEach { (name, item) ->
                            Text(
                                text = "$name — ${item.type}: ${item.text.take(40)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
