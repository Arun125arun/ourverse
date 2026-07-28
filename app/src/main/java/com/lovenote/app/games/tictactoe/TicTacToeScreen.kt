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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.lovenote.app.chat.ChatRepository
import com.lovenote.app.games.GameRepository
import com.lovenote.app.ui.Avatar
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private enum class GameMode { None, Local, Online }

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

private fun cellValueFromRaw(raw: Any?): CellValue = when (raw) {
    "X" -> CellValue.X
    "O" -> CellValue.O
    else -> CellValue.Empty
}

private fun checkWinner(board: List<CellValue>): Pair<PlayerInfo?, List<Int>?> {
    for (line in WIN_LINES) {
        val (a, b, c) = line
        if (board[a] != CellValue.Empty && board[a] == board[b] && board[b] == board[c]) {
            return null to line
        }
    }
    return null to null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
    myPhotoUrl: String,
    partnerPhotoUrl: String,
    gameId: String? = null,
    gameRepository: GameRepository? = null,
    myUid: String = "",
    chatRepository: ChatRepository? = null,
    onInvitePartner: (suspend () -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val repo = gameRepository
    val gid = gameId

    var gameMode by remember { mutableStateOf(GameMode.None) }

    val isOnline = gameMode == GameMode.Online && repo != null && gid != null
    val isLocal = gameMode == GameMode.Local

    if (gameId == null && gameMode == GameMode.None) {
        ModeChoiceDialog(
            onSendInvitation = {
                gameMode = GameMode.Online
            },
            onPlayLocally = {
                gameMode = GameMode.Local
            },
            onDismiss = onBack,
        )
    }

    if (gameId != null && gameMode == GameMode.None) {
        gameMode = GameMode.Online
    }

    LaunchedEffect(gameMode) {
        if (gameMode == GameMode.Online && gameId == null && onInvitePartner != null) {
            runCatching { onInvitePartner() }
        }
    }

    val firestoreSession by remember(gid, repo) {
        if (repo != null && gid != null) repo.observeGame(gid)
        else flowOf(null)
    }.collectAsState(initial = null)

    val session = firestoreSession

    val myPieceOnline = if (isOnline && session != null) {
        if (session.p1Uid == myUid) CellValue.X else CellValue.O
    } else CellValue.X

    var currentPiece by remember { mutableStateOf(CellValue.X) }

    val player1 = remember(myName, myPhotoUrl, session, isLocal) {
        if (isLocal) {
            PlayerInfo(myName, myPhotoUrl, CellValue.X, "Player 1")
        } else {
            PlayerInfo(
                session?.p1Name?.takeIf { it.isNotBlank() } ?: myName,
                myPhotoUrl,
                CellValue.X,
                "P1",
            )
        }
    }
    val player2 = remember(partnerName, partnerPhotoUrl, session, isLocal) {
        if (isLocal) {
            PlayerInfo(partnerName, partnerPhotoUrl, CellValue.O, "Player 2")
        } else {
            PlayerInfo(
                session?.p2Name?.takeIf { it.isNotBlank() } ?: partnerName,
                partnerPhotoUrl,
                CellValue.O,
                "P2",
            )
        }
    }

    var board by remember { mutableStateOf(List(9) { CellValue.Empty }) }
    var winner by remember { mutableStateOf<PlayerInfo?>(null) }
    var winningLine by remember { mutableStateOf<List<Int>?>(null) }
    var isDraw by remember { mutableStateOf(false) }
    var scores by remember { mutableStateOf(GameScores()) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var gameEndSent by remember { mutableStateOf(false) }
    var waitingForPartner by remember { mutableStateOf(isOnline && session?.p2Uid.isNullOrEmpty()) }

    val gameOver = winner != null || isDraw

    LaunchedEffect(session) {
        if (session == null) return@LaunchedEffect
        val rawCells = session.board["cells"] as? List<*>
        if (rawCells != null && rawCells.size == 9) {
            board = rawCells.map { cellValueFromRaw(it) }
        }
        if (session.winner.isNotEmpty() && !gameOver) {
            val winnerPiece = if (session.winner == session.p1Uid) CellValue.X else CellValue.O
            val wp = if (winnerPiece == CellValue.X) player1 else player2
            winner = wp
            for (line in WIN_LINES) {
                val (a, b, c) = line
                if (board[a] != CellValue.Empty && board[a] == board[b] && board[b] == board[c]) {
                    winningLine = line
                    break
                }
            }
            if (winnerPiece == CellValue.X) scores = scores.copy(p1Wins = scores.p1Wins + 1)
            else scores = scores.copy(p2Wins = scores.p2Wins + 1)
            showGameOverDialog = true
        }
        if (session.winner.isEmpty() && session.board["cells"] != null && !gameOver) {
            val rawCells = session.board["cells"] as? List<*>
            if (rawCells != null && rawCells.all { it != "" && it != null }) {
                isDraw = true
                scores = scores.copy(draws = scores.draws + 1)
                showGameOverDialog = true
            }
        }
        waitingForPartner = session.p2Uid.isNullOrEmpty()
    }

    LaunchedEffect(session, isOnline) {
        if (!isOnline || session == null) return@LaunchedEffect
        if (session.p2Uid.isEmpty() && session.p1Uid != myUid && repo != null) {
            repo.joinGame(gid!!, myName)
        }
    }

    LaunchedEffect(showGameOverDialog) {
        if (showGameOverDialog && !gameEndSent && isOnline && chatRepository != null && gid != null) {
            gameEndSent = true
            val result = when {
                winner != null -> {
                    val winnerName = if (winner!!.piece == CellValue.X) session?.p1Name ?: "P1" else session?.p2Name ?: "P2"
                    val iWon = (winner!!.piece == myPieceOnline)
                    if (iWon) "You won! \uD83C\uDFC6" else "$winnerName won!"
                }
                isDraw -> "It's a draw! \uD83E\uDD1D"
                else -> "Game over"
            }
            scope.launch {
                runCatching { chatRepository.sendGameEnd(gid, "tictactoe", result) }
            }
        }
    }

    fun checkAndSetWinner(boardState: List<CellValue>) {
        for (line in WIN_LINES) {
            val (a, b, c) = line
            if (boardState[a] != CellValue.Empty &&
                boardState[a] == boardState[b] && boardState[b] == boardState[c]
            ) {
                val wp = if (boardState[a] == CellValue.X) player1 else player2
                winner = wp
                winningLine = line
                if (boardState[a] == CellValue.X) scores = scores.copy(p1Wins = scores.p1Wins + 1)
                else scores = scores.copy(p2Wins = scores.p2Wins + 1)
                showGameOverDialog = true
                return
            }
        }
        if (boardState.all { it != CellValue.Empty }) {
            isDraw = true
            scores = scores.copy(draws = scores.draws + 1)
            showGameOverDialog = true
        }
    }

    fun resetGame() {
        board = List(9) { CellValue.Empty }
        winner = null
        winningLine = null
        isDraw = false
        showGameOverDialog = false
        gameEndSent = false
        if (isLocal) {
            currentPiece = CellValue.X
        }
        if (isOnline && repo != null && gid != null) {
            scope.launch {
                runCatching {
                    repo.makeMove(
                        gid,
                        mapOf("cells" to List(9) { "" }),
                        session?.currentTurn ?: myUid,
                        mapOf("action" to "rematch", "by" to myUid),
                    )
                }
            }
        }
    }

    suspend fun handleCellClick(index: Int) {
        if (board[index] != CellValue.Empty || gameOver) return

        if (isLocal) {
            val newBoard = board.toMutableList()
            newBoard[index] = currentPiece
            board = newBoard
            checkAndSetWinner(newBoard)
            if (!gameOver) {
                currentPiece = if (currentPiece == CellValue.X) CellValue.O else CellValue.X
            }
            return
        }

        if (isOnline && session != null && !session.isMyTurn(myUid)) return

        val newBoard = board.toMutableList()
        newBoard[index] = myPieceOnline
        board = newBoard

        var localWinner = false
        for (line in WIN_LINES) {
            val (a, b, c) = line
            if (newBoard[a] != CellValue.Empty &&
                newBoard[a] == newBoard[b] && newBoard[b] == newBoard[c]
            ) {
                localWinner = true
                winner = if (myPieceOnline == CellValue.X) player1 else player2
                winningLine = line
                if (myPieceOnline == CellValue.X) scores = scores.copy(p1Wins = scores.p1Wins + 1)
                else scores = scores.copy(p2Wins = scores.p2Wins + 1)
                showGameOverDialog = true
                break
            }
        }

        val isDrawLocal = !localWinner && newBoard.all { it != CellValue.Empty }
        if (isDrawLocal) {
            isDraw = true
            scores = scores.copy(draws = scores.draws + 1)
            showGameOverDialog = true
        }

        if (isOnline && repo != null && gid != null) {
            val nextTurn = if (localWinner || isDrawLocal) "" else {
                if (myUid == session?.p1Uid) session?.p2Uid ?: "" else session?.p1Uid ?: ""
            }
            val winnerStr = when {
                localWinner -> myUid
                isDrawLocal -> "draw"
                else -> ""
            }
            repo.makeMove(
                gid,
                mapOf("cells" to newBoard.map { it.name }),
                nextTurn,
                mapOf("cell" to index, "piece" to myPieceOnline.name, "by" to myUid),
                winner = winnerStr,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tic Tac Toe")
                        if (gameMode == GameMode.None) {
                            // no subtitle while choosing
                        } else if (isOnline) {
                            Text(
                                text = if (waitingForPartner) "Waiting for partner to join..."
                                else "Online \u2022 Your piece: ${myPieceOnline.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        } else if (isLocal) {
                            Text(
                                text = "Local \u2022 Player 1: X \u2022 Player 2: O",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (gameMode == GameMode.None) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))

                if (isOnline && waitingForPartner) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Share the game link or have your partner tap the invite in chat.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Scoreboard(player1 = player1, player2 = player2, scores = scores)
                Spacer(Modifier.height(20.dp))

                if (isLocal) {
                    val localPlayer = if (currentPiece == CellValue.X) player1 else player2
                    TurnIndicator(player = localPlayer, isGameOver = gameOver)
                } else {
                    val currentPlayer = if (board.count { it != CellValue.Empty } % 2 == 0) player1 else player2
                    TurnIndicator(player = currentPlayer, isGameOver = gameOver)
                }
                Spacer(Modifier.height(24.dp))

                Board(
                    board = board,
                    winningLine = winningLine,
                    gameOver = gameOver,
                    onCellClick = { index -> scope.launch { handleCellClick(index) } },
                )

                Spacer(Modifier.weight(1f))

                if (isLocal && !gameOver) {
                    Surface(
                        onClick = {
                            currentPiece = if (currentPiece == CellValue.X) CellValue.O else CellValue.X
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = "Pass device to ${if (currentPiece == CellValue.X) "Player 2" else "Player 1"} \u2192",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }

                if (isOnline && !gameOver && !waitingForPartner) {
                    val isMyTurn = session?.isMyTurn(myUid) == true
                    Text(
                        text = if (isMyTurn) "Your turn \u2714" else "Partner's turn...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMyTurn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

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
    }

    if (showGameOverDialog) {
        GameOverDialog(
            winner = winner,
            isDraw = isDraw,
            onDismiss = { showGameOverDialog = false },
            onRematch = ::resetGame,
        )
    }
}

// ─── Mode Choice Dialog ─────────────────────────────────────────────────────

@Composable
private fun ModeChoiceDialog(
    onSendInvitation: () -> Unit,
    onPlayLocally: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "How to play?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeOption(
                    title = "Send Invitation",
                    subtitle = "Play with your partner online",
                    onClick = onSendInvitation,
                )
                ModeOption(
                    title = "Play Locally",
                    subtitle = "Two players, same device",
                    onClick = onPlayLocally,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ModeOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            PlayerScoreColumn(player = player1, score = scores.p1Wins, alignment = Alignment.Start)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text("Draws", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("${scores.draws}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PlayerScoreColumn(player = player2, score = scores.p2Wins, alignment = Alignment.End)
        }
    }
}

@Composable
private fun PlayerScoreColumn(player: PlayerInfo, score: Int, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment, modifier = Modifier.width(100.dp)) {
        Avatar(name = player.name, photoUrl = player.photoUrl, size = 40.dp)
        Spacer(Modifier.height(6.dp))
        Text(player.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(player.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text("$score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TurnIndicator(player: PlayerInfo, isGameOver: Boolean) {
    val alpha by animateFloatAsState(targetValue = if (isGameOver) 0.4f else 1f, animationSpec = tween(300), label = "turnAlpha")
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Avatar(name = player.name, photoUrl = player.photoUrl, size = 28.dp)
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

@Composable
private fun Board(board: List<CellValue>, winningLine: List<Int>?, gameOver: Boolean, onCellClick: (Int) -> Unit) {
    val entryProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryProgress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(1f)
            .graphicsLayer { scaleX = entryProgress.value; scaleY = entryProgress.value; alpha = entryProgress.value }
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            for (row in 0..2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        Cell(
                            value = board[index],
                            isWinning = winningLine?.contains(index) == true,
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

@Composable
private fun Cell(value: CellValue, isWinning: Boolean, isGameOver: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cellBg by animateColorAsState(
        targetValue = when {
            isWinning -> MaterialTheme.colorScheme.primaryContainer
            value != CellValue.Empty -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        },
        animationSpec = tween(350),
        label = "cellBg",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "winGlow")
    val glowAlpha by if (isWinning) {
        infiniteTransition.animateFloat(0.15f, 0.35f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glowAlpha")
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val pieceScale = remember { Animatable(0f) }
    LaunchedEffect(value) {
        if (value != CellValue.Empty) {
            pieceScale.snapTo(0f)
            pieceScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    val cellShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(if (isWinning) 4.dp else 2.dp, cellShape, ambientColor = if (isWinning) MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha) else MaterialTheme.colorScheme.scrim.copy(alpha = 0.05f))
            .clip(cellShape)
            .background(cellBg)
            .then(if (isWinning) Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), cellShape) else Modifier)
            .clickable(enabled = value == CellValue.Empty && !isGameOver) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (value != CellValue.Empty) {
            GamePiece(piece = value, modifier = Modifier.fillMaxSize(0.55f).graphicsLayer { scaleX = pieceScale.value; scaleY = pieceScale.value })
        }
    }
}

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
                drawLine(primary, Offset(padding, padding), Offset(size.width - padding, size.height - padding), strokeWidth, cap = cap)
                drawLine(primary, Offset(size.width - padding, padding), Offset(padding, size.height - padding), strokeWidth, cap = cap)
            }
            CellValue.O -> {
                drawOval(tertiary, style = Stroke(strokeWidth, cap = cap), topLeft = Offset(padding, padding), size = Size(size.width - padding * 2, size.height - padding * 2))
            }
            CellValue.Empty -> {}
        }
    }
}

@Composable
private fun GameOverDialog(winner: PlayerInfo?, isDraw: Boolean, onDismiss: () -> Unit, onRematch: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(if (winner != null) "\uD83C\uDFC6" else "\uD83E\uDD1D", fontSize = 40.sp) },
        title = { Text(if (winner != null) "${winner.label} won!" else "It's a draw!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Text(
                if (winner != null) "${winner.name} takes this round. Ready for another?"
                else "Nobody wins this time. Try again?",
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(onClick = { onRematch(); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Rematch!")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("View Board") } },
    )
}
