package com.lovenote.app.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class GameCard(
    val title: String,
    val subtitle: String,
    val icon: String,
    val gradient: List<Color>,
    val onClick: () -> Unit,
)

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesHubScreen(
    onBack: () -> Unit,
    onSendNote: () -> Unit,
    onDrawNote: () -> Unit,
    onNoteHistory: () -> Unit,
    onTicTacToe: () -> Unit,
    onLudo: () -> Unit,
    onTrivia: () -> Unit,
    onTruthOrDare: () -> Unit,
    onWordGame: () -> Unit,
    myName: String,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Couple Hub") },
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
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Welcome back, $myName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "What would you like to do today?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            // ── Notes section ──
            SectionHeader("Notes")
            Spacer(Modifier.height(12.dp))

            val noteActions = listOf(
                QuickAction("Send Note", Icons.Filled.Edit, onSendNote),
                QuickAction("Draw Note", Icons.Filled.Edit, onDrawNote),
                QuickAction("History", Icons.Filled.Favorite, onNoteHistory),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                noteActions.forEach { action ->
                    QuickActionCard(
                        action = action,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Games section ──
            SectionHeader("Games")
            Spacer(Modifier.height(12.dp))

            val games = listOf(
                GameCard(
                    title = "Tic Tac Toe",
                    subtitle = "Classic strategy game",
                    icon = "X O",
                    gradient = listOf(Color(0xFF6C63FF), Color(0xFF3F51B5)),
                    onClick = onTicTacToe,
                ),
                GameCard(
                    title = "Ludo",
                    subtitle = "Race to home",
                    icon = "🎲",
                    gradient = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24)),
                    onClick = onLudo,
                ),
                GameCard(
                    title = "Couple Trivia",
                    subtitle = "How well do you know each other?",
                    icon = "?",
                    gradient = listOf(Color(0xFF00B894), Color(0xFF00A19A)),
                    onClick = onTrivia,
                ),
                GameCard(
                    title = "Truth or Dare",
                    subtitle = "Spice things up",
                    icon = "!",
                    gradient = listOf(Color(0xFFE84393), Color(0xFFD63031)),
                    onClick = onTruthOrDare,
                ),
                GameCard(
                    title = "Word Game",
                    subtitle = "How connected are you?",
                    icon = "Aa",
                    gradient = listOf(Color(0xFF0984E3), Color(0xFF6C5CE7)),
                    onClick = onWordGame,
                ),
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(games) { game ->
                    GameFeatureCard(
                        title = game.title,
                        subtitle = game.subtitle,
                        icon = game.icon,
                        gradient = game.gradient,
                        onClick = game.onClick,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Game grid (compact view) ──
            Text(
                text = "All Games",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                games.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { game ->
                            CompactGameCard(
                                title = game.title,
                                subtitle = game.subtitle,
                                icon = game.icon,
                                gradient = game.gradient,
                                onClick = game.onClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun QuickActionCard(action: QuickAction, modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "press",
    )

    Card(
        modifier = modifier
            .height(80.dp)
            .scale(scale)
            .clickable {
                pressed = true
                action.onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GameFeatureCard(
    title: String,
    subtitle: String,
    icon: String,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "press",
    )

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(150.dp)
            .scale(scale)
            .clickable {
                pressed = true
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = icon,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactGameCard(
    title: String,
    subtitle: String,
    icon: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = icon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
