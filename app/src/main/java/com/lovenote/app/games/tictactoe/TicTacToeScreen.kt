package com.lovenote.app.games.tictactoe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.ui.Avatar

// ─── Data ────────────────────────────────────────────────────────────────────

private enum class CellValue { Empty, X, O }

private data class PlayerInfo(
    val name: String,
    val photoUrl: String,
    val piece: CellValue,
    val label: String,
)

private data class GameScores(var p1Wins: Int = 0, var p2Wins: Int = 0, var draws: Int = 0)

private val WIN_LINES = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
    listOf(0, 4, 8), listOf(2, 4, 6),
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
    myPhotoUrl: String,
    partnerPhotoUrl: String,
) {
    val player1 = remember(myName, myPhotoUrl) {
        PlayerInfo(myName, myPhotoUrl, CellValue.X, "You")
    }
    val player2 = remember(partnerName, partnerPhotoUrl) {
        PlayerInfo(partnerName, partnerPhotoUrl, CellValue.O, "Them \u2764")
    }

    var board by remember { mutableStateOf(List(9) { CellValue.Empty }) }
    var currentPlayer by remember { mutableStateOf(player1) }
    var winner by remember { mutableStateOf<PlayerInfo?>(null) }
    var winningLine by remember { mutableStateOf<List<Int>?>(null) }
    var isDraw by remember { mutableStateOf(false) }
    var scores by remember { mutableStateOf(GameScores()) }
    var moveCount by remember { mutableIntStateOf(0) }
    var showGameOverDialog by remember { mutableStateOf(false) }

    val gameOver = winner != null || isDraw

    fun resetGame() {
        board = List(9) { CellValue.Empty }
        currentPlayer = player1
        winner = null
        winningLine = null
        isDraw = false
        moveCount = 0
        showGameOverDialog = false
    }

    fun handleCellClick(index: Int) {
        if (board[index] != CellValue.Empty || gameOver) return
        val newBoard = board.toMutableList()
        newBoard[index] = currentPlayer.piece
        board = newBoard
        moveCount++

        // Check win
        for (line in WIN_LINES) {
            val (a, b, c) = line
            if (newBoard[a] != CellValue.Empty &&
                newBoard[a] == newBoard[b] && newBoard[b] == newBoard[c]
            ) {
                winner = currentPlayer
                winningLine = line
                if (currentPlayer == player1) scores = scores.copy(p1Wins = scores.p1Wins + 1)
                else scores = scores.copy(p2Wins = scores.p2Wins + 1)
                showGameOverDialog = true
                return
            }
        }

        // Check draw
        if (moveCount >= 9) {
            isDraw = true
            scores = scores.copy(draws = scores.draws + 1)
            showGameOverDialog = true
            return
        }

        currentPlayer = if (currentPlayer == player1) player2 else player1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tic Tac Toe") },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Scoreboard ──
            Scoreboard(
                player1 = player1,
                player2 = player2,
                scores = scores,
            )

            Spacer(Modifier.height(20.dp))

            // ── Turn indicator ──
            TurnIndicator(
                player = currentPlayer,
                isGameOver = gameOver,
            )

            Spacer(Modifier.height(24.dp))

            // ── Board ──
            Board(
                board = board,
                winningLine = winningLine,
                gameOver = gameOver,
                onCellClick = ::handleCellClick,
            )

            Spacer(Modifier.weight(1f))

            // ── Rematch button ──
            if (gameOver) {
                Button(
                    onClick = ::resetGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Rematch!", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(52.dp + 24.dp))
            }
        }
    }

    // ── Game-over dialog ──
    if (showGameOverDialog) {
        GameOverDialog(
            winner = winner,
            isDraw = isDraw,
            onDismiss = { showGameOverDialog = false },
            onRematch = ::resetGame,
        )
    }
}

// ─── Scoreboard ──────────────────────────────────────────────────────────────

@Composable
private fun Scoreboard(
    player1: PlayerInfo,
    player2: PlayerInfo,
    scores: GameScores,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player 1
            PlayerScoreColumn(player = player1, score = scores.p1Wins, alignment = Alignment.Start)

            // Center – Draws
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    "Draws",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${scores.draws}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Player 2
            PlayerScoreColumn(player = player2, score = scores.p2Wins, alignment = Alignment.End)
        }
    }
}

@Composable
private fun PlayerScoreColumn(
    player: PlayerInfo,
    score: Int,
    alignment: Alignment.Horizontal,
) {
    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.width(100.dp),
    ) {
        Avatar(
            name = player.name,
            photoUrl = player.photoUrl,
            size = 40.dp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            player.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            player.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$score",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ─── Turn Indicator ──────────────────────────────────────────────────────────

@Composable
private fun TurnIndicator(player: PlayerInfo, isGameOver: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (isGameOver) 0.4f else 1f,
        animationSpec = tween(300),
        label = "turnAlpha",
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Avatar(
                name = player.name,
                photoUrl = player.photoUrl,
                size = 28.dp,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (isGameOver) "Game Over" else "${player.label}'s turn",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ─── Board ───────────────────────────────────────────────────────────────────

@Composable
private fun Board(
    board: List<CellValue>,
    winningLine: List<Int>?,
    gameOver: Boolean,
    onCellClick: (Int) -> Unit,
) {
    // Board entry animation
    val entryProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = entryProgress.value
                scaleY = entryProgress.value
                alpha = entryProgress.value
            }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        val isWinning = winningLine?.contains(index) == true
                        Cell(
                            value = board[index],
                            isWinning = isWinning,
                            isGameOver = gameOver,
                            onClick = { onCellClick(index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

// ─── Cell ────────────────────────────────────────────────────────────────────

@Composable
private fun Cell(
    value: CellValue,
    isWinning: Boolean,
    isGameOver: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellBg by animateColorAsState(
        targetValue = when {
            isWinning -> MaterialTheme.colorScheme.primaryContainer
            value != CellValue.Empty -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        },
        animationSpec = tween(350),
        label = "cellBg",
    )

    // Win glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "winGlow")
    val glowAlpha by if (isWinning) {
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowAlpha",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Piece entry scale
    val pieceScale = remember { Animatable(0f) }
    LaunchedEffect(value) {
        if (value != CellValue.Empty) {
            pieceScale.snapTo(0f)
            pieceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    val cellShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(
                elevation = if (isWinning) 4.dp else 2.dp,
                shape = cellShape,
                ambientColor = if (isWinning) MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha) else MaterialTheme.colorScheme.scrim.copy(alpha = 0.05f),
            )
            .clip(cellShape)
            .background(cellBg)
            .then(
                if (isWinning) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = cellShape,
                ) else Modifier
            )
            .clickable(enabled = value == CellValue.Empty && !isGameOver) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (value != CellValue.Empty) {
            GamePiece(
                piece = value,
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .graphicsLayer {
                        scaleX = pieceScale.value
                        scaleY = pieceScale.value
                    },
            )
        }
    }
}

// ─── Game Pieces (X and O) ──────────────────────────────────────────────────

@Composable
private fun GamePiece(piece: CellValue, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.1f
        val padding = size.minDimension * 0.15f
        val cap = StrokeCap.Round

        when (piece) {
            CellValue.X -> {
                // Draw X as two crossing lines
                drawLine(
                    color = primary,
                    start = Offset(padding, padding),
                    end = Offset(size.width - padding, size.height - padding),
                    strokeWidth = strokeWidth,
                    cap = cap,
                )
                drawLine(
                    color = primary,
                    start = Offset(size.width - padding, padding),
                    end = Offset(padding, size.height - padding),
                    strokeWidth = strokeWidth,
                    cap = cap,
                )
            }
            CellValue.O -> {
                // Draw O as a circle
                drawOval(
                    color = tertiary,
                    style = Stroke(width = strokeWidth, cap = cap),
                    topLeft = Offset(padding, padding),
                    size = Size(
                        size.width - padding * 2,
                        size.height - padding * 2,
                    ),
                )
            }
            CellValue.Empty -> {}
        }
    }
}

// ─── Game Over Dialog ────────────────────────────────────────────────────────

@Composable
private fun GameOverDialog(
    winner: PlayerInfo?,
    isDraw: Boolean,
    onDismiss: () -> Unit,
    onRematch: () -> Unit,
) {
    val title = when {
        winner != null -> "${winner.label} won!"
        else -> "It's a draw!"
    }
    val message = when {
        winner != null -> "${winner.name} takes this round. Ready for another?"
        else -> "Nobody wins this time. Try again?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = when {
                    winner != null -> "\uD83C\uDFC6"
                    else -> "\uD83E\uDD1D"
                },
                fontSize = 40.sp,
            )
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onRematch()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Rematch!")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("View Board")
            }
        },
    )
}
