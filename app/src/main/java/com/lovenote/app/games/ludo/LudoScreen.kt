package com.lovenote.app.games.ludo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.random.Random

private const val TRACK_LENGTH = 24
private const val HOME_ZONE = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
) {
    var player1Pos by remember { mutableIntStateOf(-1) }
    var player2Pos by remember { mutableIntStateOf(-1) }
    var currentTurn by remember { mutableIntStateOf(1) }
    var diceValue by remember { mutableIntStateOf(1) }
    var rolling by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf("") }
    var score1 by remember { mutableIntStateOf(0) }
    var score2 by remember { mutableIntStateOf(0) }
    val moveHistory = remember { mutableStateListOf<Pair<Int, Int>>() }

    fun rollDice() {
        if (rolling || gameOver) return
        rolling = true
        diceValue = Random.nextInt(1, 7)
        rolling = false

        if (currentTurn == 1) {
            val newPos = player1Pos + diceValue
            if (newPos == TRACK_LENGTH) {
                player1Pos = newPos
                score1++
                winner = myName
                gameOver = true
            } else if (newPos < TRACK_LENGTH) {
                player1Pos = newPos
            }
            moveHistory.add(1 to player1Pos)
            currentTurn = 2
        } else {
            val newPos = player2Pos + diceValue
            if (newPos == TRACK_LENGTH) {
                player2Pos = newPos
                score2++
                winner = partnerName
                gameOver = true
            } else if (newPos < TRACK_LENGTH) {
                player2Pos = newPos
            }
            moveHistory.add(2 to player2Pos)
            currentTurn = 1
        }
    }

    fun resetGame() {
        player1Pos = -1
        player2Pos = -1
        currentTurn = 1
        diceValue = 1
        gameOver = false
        winner = ""
        moveHistory.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Race to Home") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlayerScoreCard(
                    name = myName,
                    score = score1,
                    isTurn = currentTurn == 1 && !gameOver,
                    color = MaterialTheme.colorScheme.primary,
                )
                PlayerScoreCard(
                    name = partnerName,
                    score = score2,
                    isTurn = currentTurn == 2 && !gameOver,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Spacer(Modifier.height(20.dp))

            GameBoard(
                player1Pos = player1Pos,
                player2Pos = player2Pos,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f),
            )

            Spacer(Modifier.height(20.dp))

            DiceButton(
                value = diceValue,
                rolling = rolling,
                enabled = !rolling && !gameOver,
                onClick = { rollDice() },
            )

            Spacer(Modifier.height(12.dp))

            if (gameOver) {
                Text(
                    text = "$winner wins! 🎉",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { resetGame() }) { Text("Rematch") }
                    OutlinedButton(onClick = onBack) { Text("Back") }
                }
            } else {
                Text(
                    text = if (currentTurn == 1) "$myName's turn" else "$partnerName's turn",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun PlayerScoreCard(
    name: String,
    score: Int,
    isTurn: Boolean,
    color: Color,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTurn) color.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        border = if (isTurn) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isTurn) color else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GameBoard(
    player1Pos: Int,
    player2Pos: Int,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val homeColor = MaterialTheme.colorScheme.primaryContainer
    val p1Color = MaterialTheme.colorScheme.primary
    val p2Color = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            PlayerTrack(
                label = "You",
                position = player1Pos,
                trackColor = trackColor,
                homeColor = homeColor,
                tokenColor = p1Color,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            )
            PlayerTrack(
                label = "Them",
                position = player2Pos,
                trackColor = trackColor,
                homeColor = homeColor,
                tokenColor = p2Color,
            )
        }
    }
}

@Composable
private fun PlayerTrack(
    label: String,
    position: Int,
    trackColor: Color,
    homeColor: Color,
    tokenColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(trackColor.copy(alpha = 0.3f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (i in 0 until TRACK_LENGTH) {
                val isHome = i >= TRACK_LENGTH - HOME_ZONE
                val isPlayerHere = position == i
                val isPastHome = position >= TRACK_LENGTH

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isPastHome && i == TRACK_LENGTH - 1 -> tokenColor.copy(alpha = 0.3f)
                                isHome -> homeColor
                                else -> trackColor
                            },
                        )
                        .then(
                            if (isPlayerHere) {
                                Modifier.border(2.dp, tokenColor, RoundedCornerShape(4.dp))
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isPlayerHere) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.7f)
                                .clip(CircleShape)
                                .background(tokenColor),
                        )
                    } else if (isHome) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.4f)
                                .clip(CircleShape)
                                .background(tokenColor.copy(alpha = 0.15f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiceButton(
    value: Int,
    rolling: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (rolling) 0.8f else 1f,
        animationSpec = tween(100),
        label = "dice",
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(6.dp, CircleShape)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val dotRadius = size.minDimension / 10
            val cx = size.width / 2
            val cy = size.height / 2
            val offset = size.minDimension / 4

            fun drawDot(x: Float, y: Float) {
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.Unspecified,
                    radius = dotRadius,
                    center = Offset(x, y),
                )
                drawContext.canvas.nativeCanvas.drawCircle(
                    x, y, dotRadius,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        isAntiAlias = true
                    },
                )
            }

            val dots = when (value) {
                1 -> listOf(cx to cy)
                2 -> listOf(cx - offset to cy - offset, cx + offset to cy + offset)
                3 -> listOf(cx - offset to cy - offset, cx to cy, cx + offset to cy + offset)
                4 -> listOf(
                    cx - offset to cy - offset, cx + offset to cy - offset,
                    cx - offset to cy + offset, cx + offset to cy + offset,
                )
                5 -> listOf(
                    cx - offset to cy - offset, cx + offset to cy - offset,
                    cx to cy,
                    cx - offset to cy + offset, cx + offset to cy + offset,
                )
                6 -> listOf(
                    cx - offset to cy - offset, cx + offset to cy - offset,
                    cx - offset to cy, cx + offset to cy,
                    cx - offset to cy + offset, cx + offset to cy + offset,
                )
                else -> emptyList()
            }
            dots.forEach { (x, y) -> drawDot(x, y) }
        }
    }
}
