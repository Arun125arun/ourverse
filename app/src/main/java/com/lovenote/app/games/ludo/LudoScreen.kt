package com.lovenote.app.games.ludo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.games.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─── Constants ────────────────────────────────────────────────────────────────

private const val GRID = 15
private const val CELLS = 52

// ─── Classic Ludo Colors ─────────────────────────────────────────────────────

private val ClassicRed = Color(0xFFD32F2F)
private val ClassicRedLight = Color(0xFFFFCDD2)
private val ClassicRedDark = Color(0xFFB71C1C)

private val ClassicGreen = Color(0xFF388E3C)
private val ClassicGreenLight = Color(0xFFC8E6C9)
private val ClassicGreenDark = Color(0xFF1B5E20)

private val ClassicBlue = Color(0xFF1565C0)
private val ClassicBlueLight = Color(0xFFBBDEFB)
private val ClassicBlueDark = Color(0xFF0D47A1)

private val ClassicYellow = Color(0xFFF9A825)
private val ClassicYellowLight = Color(0xFFFFF9C4)
private val ClassicYellowDark = Color(0xFFF57F17)

private val BoardWhite = Color(0xFFF5F0E8)
private val TrackCell = Color(0xFFFAF8F2)
private val TrackBorder = Color(0xFFD5D0C8)
private val SafeGold = Color(0xFFFFCA28)
private val HomeCenterBg = Color(0xFFEFEBE9)

// ─── Track coordinates (52 cells, clockwise) ──────────────────────────────────
private val TRACK = listOf(
    intArrayOf(6, 1), intArrayOf(6, 2), intArrayOf(6, 3),
    intArrayOf(6, 4), intArrayOf(6, 5),
    intArrayOf(5, 6), intArrayOf(4, 6), intArrayOf(3, 6),
    intArrayOf(2, 6), intArrayOf(1, 6),
    intArrayOf(1, 7), intArrayOf(1, 8),
    intArrayOf(1, 9), intArrayOf(1, 10), intArrayOf(1, 11),
    intArrayOf(1, 12), intArrayOf(1, 13),
    intArrayOf(2, 13), intArrayOf(3, 13),
    intArrayOf(4, 13), intArrayOf(5, 13),
    intArrayOf(6, 13), intArrayOf(6, 14),
    intArrayOf(7, 14), intArrayOf(7, 13),
    intArrayOf(7, 12), intArrayOf(7, 11),
    intArrayOf(7, 10), intArrayOf(7, 9),
    intArrayOf(7, 8),
    intArrayOf(8, 8), intArrayOf(8, 7),
    intArrayOf(8, 6), intArrayOf(8, 5),
    intArrayOf(8, 4), intArrayOf(8, 3),
    intArrayOf(8, 2), intArrayOf(8, 1),
    intArrayOf(9, 1),
    intArrayOf(9, 2), intArrayOf(9, 3),
    intArrayOf(9, 4), intArrayOf(9, 5),
    intArrayOf(9, 6), intArrayOf(10, 6),
    intArrayOf(11, 6), intArrayOf(12, 6),
    intArrayOf(13, 6),
    intArrayOf(14, 6), intArrayOf(14, 7),
    intArrayOf(13, 8), intArrayOf(12, 8),
    intArrayOf(11, 8), intArrayOf(10, 8),
    intArrayOf(9, 8),
)

// ─── Home columns (Red: col 7 going down from row 1→6, Green: col 7 going up from row 13→8) ──
private val RED_HOME = listOf(
    intArrayOf(1, 7), intArrayOf(2, 7), intArrayOf(3, 7),
    intArrayOf(4, 7), intArrayOf(5, 7), intArrayOf(6, 7),
)
private val GREEN_HOME = listOf(
    intArrayOf(13, 7), intArrayOf(12, 7), intArrayOf(11, 7),
    intArrayOf(10, 7), intArrayOf(9, 7), intArrayOf(8, 7),
)

private const val RED_ENTRY = 0
private const val GREEN_ENTRY = 26

private val RED_BASE = listOf(
    intArrayOf(2, 2), intArrayOf(2, 4), intArrayOf(4, 2), intArrayOf(4, 4),
)
private val GREEN_BASE = listOf(
    intArrayOf(10, 10), intArrayOf(10, 12), intArrayOf(12, 10), intArrayOf(12, 12),
)

private val SAFE_POSITIONS = setOf(0, 8, 13, 21, 26, 34, 39, 47)

// ─── Token states ─────────────────────────────────────────────────────────────

private enum class TokenPlace { BASE, TRACK, HOME_COL, FINISHED }

private data class Token(
    var place: TokenPlace = TokenPlace.BASE,
    var trackPos: Int = -1,
    var homeStep: Int = -1,
)

private data class PlayerData(
    val name: String,
    val color: Color,
    val colorLight: Color,
    val colorDark: Color,
    val tokens: MutableList<Token> = MutableList(4) { Token() },
    val baseSlots: List<IntArray>,
    val entry: Int,
    val homeCol: List<IntArray>,
    var wins: Int = 0,
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoScreen(
    onBack: () -> Unit,
    myName: String,
    partnerName: String,
    gameId: String? = null,
    gameRepository: GameRepository? = null,
    myUid: String = "",
) {
    val p1 = remember { PlayerData(myName, ClassicRed, ClassicRedLight, ClassicRedDark, baseSlots = RED_BASE, entry = RED_ENTRY, homeCol = RED_HOME) }
    val p2 = remember { PlayerData(partnerName, ClassicGreen, ClassicGreenLight, ClassicGreenDark, baseSlots = GREEN_BASE, entry = GREEN_ENTRY, homeCol = GREEN_HOME) }

    var turn by remember { mutableIntStateOf(1) }
    var diceValue by remember { mutableIntStateOf(1) }
    var phase by remember { mutableStateOf("roll") }
    var selectedToken by remember { mutableIntStateOf(-1) }
    var winner by remember { mutableStateOf<PlayerData?>(null) }
    val diceHistory = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()
    val animProgress = remember { Animatable(0f) }
    var isRolling by remember { mutableStateOf(false) }
    var diceDisplay by remember { mutableIntStateOf(1) }

    fun current() = if (turn == 1) p1 else p2
    fun allFinished(p: PlayerData) = p.tokens.count { it.place == TokenPlace.FINISHED } == 4

    fun availableMoves(p: PlayerData, dice: Int): List<Int> {
        val moves = mutableListOf<Int>()
        for (i in 0..3) {
            val t = p.tokens[i]
            when (t.place) {
                TokenPlace.BASE -> { if (dice == 6) moves.add(i) }
                TokenPlace.TRACK -> {
                    val dest = t.trackPos + dice
                    if (dest in 0..57) moves.add(i)
                }
                TokenPlace.HOME_COL -> {
                    val dest = t.homeStep + dice
                    if (dest <= 6) moves.add(i)
                }
                TokenPlace.FINISHED -> {}
            }
        }
        return moves
    }

    fun findCaptures(p: PlayerData, pos: Int): List<Int> {
        val captured = mutableListOf<Int>()
        val opp = if (p == p1) p2 else p1
        for (i in 0..3) {
            val t = opp.tokens[i]
            if (t.place == TokenPlace.TRACK && t.trackPos == pos && pos !in SAFE_POSITIONS) {
                captured.add(i)
            }
        }
        return captured
    }

    fun executeMove(tokenIdx: Int) {
        val p = current()
        val t = p.tokens[tokenIdx]
        when (t.place) {
            TokenPlace.BASE -> {
                t.place = TokenPlace.TRACK
                t.trackPos = p.entry
                for (ci in findCaptures(p, p.entry)) {
                    (if (p == p1) p2 else p1).tokens[ci] = Token()
                }
            }
            TokenPlace.TRACK -> {
                val dest = t.trackPos + diceValue
                if (dest <= 51) {
                    t.trackPos = dest
                    for (ci in findCaptures(p, dest)) {
                        (if (p == p1) p2 else p1).tokens[ci] = Token()
                    }
                } else {
                    t.place = TokenPlace.HOME_COL
                    t.trackPos = -1
                    t.homeStep = dest - 52
                }
            }
            TokenPlace.HOME_COL -> {
                val dest = t.homeStep + diceValue
                if (dest == 6) { t.place = TokenPlace.FINISHED; t.homeStep = -1 }
                else t.homeStep = dest
            }
            TokenPlace.FINISHED -> {}
        }
        if (allFinished(p)) { p.wins++; winner = p; phase = "gameover" }
        else if (diceValue == 6) phase = "roll"
        else { turn = if (turn == 1) 2 else 1; phase = "roll" }
    }

    fun doRollDice() {
        if (phase != "roll" || isRolling) return
        isRolling = true
        diceHistory.add(0) // placeholder
        scope.launch {
            // Dice rolling animation - cycle through random faces
            val finalValue = Random.nextInt(1, 7)
            val rollDuration = 800L
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < rollDuration) {
                diceDisplay = Random.nextInt(1, 7)
                delay(60)
            }
            diceDisplay = finalValue
            diceValue = finalValue
            diceHistory[diceHistory.lastIndex] = finalValue
            isRolling = false

            val p = current()
            val moves = availableMoves(p, finalValue)
            if (moves.isEmpty()) {
                turn = if (turn == 1) 2 else 1
                phase = "roll"
            } else if (moves.size == 1) {
                selectedToken = moves[0]
                phase = "animate"
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, animationSpec = tween(350 * finalValue, easing = EaseOutCubic))
                executeMove(moves[0])
            } else {
                phase = "select"
            }
        }
    }

    fun selectToken(idx: Int) {
        selectedToken = idx
        phase = "animate"
        scope.launch {
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, animationSpec = tween(350 * diceValue, easing = EaseOutCubic))
            executeMove(idx)
        }
    }

    fun resetGame() {
        p1.tokens.forEachIndexed { i, _ -> p1.tokens[i] = Token() }
        p2.tokens.forEachIndexed { i, _ -> p2.tokens[i] = Token() }
        turn = 1; diceValue = 1; phase = "roll"; selectedToken = -1; winner = null
        diceHistory.clear(); isRolling = false; diceDisplay = 1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ludo", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Player info pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlayerPill(name = p1.name, color = p1.color, wins = p1.wins, isTurn = turn == 1 && phase != "gameover")
                PlayerPill(name = p2.name, color = p2.color, wins = p2.wins, isTurn = turn == 2 && phase != "gameover")
            }

            Spacer(Modifier.height(6.dp))

            // Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                LudoBoardCanvas(
                    p1 = p1, p2 = p2,
                    turn = turn,
                    selectedIdx = if (phase == "select") -1 else selectedToken,
                    animProgress = animProgress.value,
                )
            }

            Spacer(Modifier.height(6.dp))

            // Bottom area
            when (phase) {
                "roll" -> {
                    DiceButton(
                        displayValue = diceDisplay,
                        isRolling = isRolling,
                        onClick = ::doRollDice,
                        enabled = !isRolling,
                        playerColor = current().color,
                    )
                    Spacer(Modifier.height(6.dp))
                    val cur = current()
                    Text(
                        text = if (isRolling) "Rolling..." else "${cur.name}'s turn",
                        style = MaterialTheme.typography.titleSmall,
                        color = cur.color,
                        fontWeight = FontWeight.Bold,
                    )
                }
                "select" -> {
                    val cur = current()
                    Text(
                        text = "Rolled $diceValue — Tap a token:",
                        style = MaterialTheme.typography.titleSmall,
                        color = cur.color,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (idx in availableMoves(cur, diceValue)) {
                            val t = cur.tokens[idx]
                            val label = when (t.place) {
                                TokenPlace.BASE -> "Out"
                                TokenPlace.TRACK -> "T${idx + 1}"
                                TokenPlace.HOME_COL -> "H${idx + 1}"
                                else -> "?"
                            }
                            Button(
                                onClick = { selectToken(idx) },
                                colors = ButtonDefaults.buttonColors(containerColor = cur.color),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) { Text(label, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                "animate" -> {
                    Text(
                        text = "Moving...",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                "gameover" -> {
                    val w = winner!!
                    Text(
                        text = "${w.name} wins!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = w.color,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = ::resetGame, colors = ButtonDefaults.buttonColors(containerColor = w.color)) { Text("Rematch") }
                        OutlinedButton(onClick = onBack) { Text("Back") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (winner != null && phase == "gameover") {
        AlertDialog(
            onDismissRequest = { },
            icon = { Text("\uD83C\uDFC6", fontSize = 40.sp) },
            title = { Text("${winner!!.name} wins!") },
            text = { Text("Amazing game! Ready for another round?") },
            confirmButton = {
                Button(onClick = { resetGame(); winner = null }, colors = ButtonDefaults.buttonColors(containerColor = winner!!.color)) { Text("Rematch!") }
            },
            dismissButton = { TextButton(onClick = onBack) { Text("Back to Hub") } },
        )
    }
}

// ─── Player Pill ──────────────────────────────────────────────────────────────

@Composable
private fun PlayerPill(name: String, color: Color, wins: Int, isTurn: Boolean) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTurn) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        border = if (isTurn) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTurn) 2.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text("$wins", style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Dice Button (with rolling animation) ────────────────────────────────────

@Composable
private fun DiceButton(
    displayValue: Int,
    isRolling: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    playerColor: Color,
) {
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            while (true) {
                shakeX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(50, easing = LinearEasing),
                )
                shakeX.animateTo(
                    targetValue = Random.nextFloat() * 6f - 3f,
                    animationSpec = tween(40, easing = LinearEasing),
                )
                shakeY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(50, easing = LinearEasing),
                )
                shakeY.animateTo(
                    targetValue = Random.nextFloat() * 6f - 3f,
                    animationSpec = tween(40, easing = LinearEasing),
                )
            }
        } else {
            shakeX.snapTo(0f)
            shakeY.snapTo(0f)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isRolling) 0.9f else 1f,
        animationSpec = spring(stiffness = 300f),
        label = "diceScale",
    )

    val rotation by animateFloatAsState(
        targetValue = if (isRolling) Random.nextFloat() * 30f - 15f else 0f,
        animationSpec = tween(80),
        label = "diceRot",
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = shakeX.value
                translationY = shakeY.value
                this.rotationZ = rotation
            }
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFBF5))
            .border(2.5.dp, playerColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .then(if (enabled && !isRolling) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val dotR = size.minDimension / 10f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val off = size.minDimension / 4f

            val dotColor = android.graphics.Color.parseColor("#2D2D2D")

            fun dot(x: Float, y: Float) {
                drawContext.canvas.nativeCanvas.drawCircle(
                    x, y, dotR,
                    android.graphics.Paint().apply {
                        color = dotColor
                        isAntiAlias = true
                        setShadowLayer(3f, 1f, 1f, android.graphics.Color.argb(40, 0, 0, 0))
                    },
                )
            }

            when (displayValue) {
                1 -> dot(cx, cy)
                2 -> { dot(cx - off, cy - off); dot(cx + off, cy + off) }
                3 -> { dot(cx - off, cy - off); dot(cx, cy); dot(cx + off, cy + off) }
                4 -> {
                    dot(cx - off, cy - off); dot(cx + off, cy - off)
                    dot(cx - off, cy + off); dot(cx + off, cy + off)
                }
                5 -> {
                    dot(cx - off, cy - off); dot(cx + off, cy - off)
                    dot(cx, cy)
                    dot(cx - off, cy + off); dot(cx + off, cy + off)
                }
                6 -> {
                    dot(cx - off, cy - off); dot(cx + off, cy - off)
                    dot(cx - off, cy); dot(cx + off, cy)
                    dot(cx - off, cy + off); dot(cx + off, cy + off)
                }
            }
        }
    }
}

// ─── Board Canvas ─────────────────────────────────────────────────────────────

@Composable
private fun LudoBoardCanvas(
    p1: PlayerData,
    p2: PlayerData,
    turn: Int,
    selectedIdx: Int,
    animProgress: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cw = size.width / GRID
        val ch = size.height / GRID

        fun cellCenter(r: Int, c: Int) = Offset(c * cw + cw / 2, r * ch + ch / 2)
        fun cellRect(r: Int, c: Int) = Rect(c * cw, r * ch, (c + 1) * cw, (r + 1) * ch)

        // ── Board background ──
        drawRoundRect(BoardWhite, Offset.Zero, Size(size.width, size.height), CornerRadius(12f))

        // ── Outer border ──
        drawRoundRect(Color(0xFFBDB6AB), Offset.Zero, Size(size.width, size.height), CornerRadius(12f), style = Stroke(3f))

        // ── Red home base (top-left) ──
        drawRoundRect(ClassicRedLight, Offset(0f, 0f), Size(6 * cw, 6 * ch), CornerRadius(8f))
        // Inner white yard
        drawRoundRect(Color.White, Offset(cw, ch), Size(4 * cw, 4 * ch), CornerRadius(8f))
        // Red base circles
        RED_BASE.forEach { (r, c) ->
            drawCircle(ClassicRedLight.copy(alpha = 0.5f), cw * 0.4f, cellCenter(r, c))
        }

        // ── Green home base (bottom-right) ──
        drawRoundRect(ClassicGreenLight, Offset(9 * cw, 9 * ch), Size(6 * cw, 6 * ch), CornerRadius(8f))
        drawRoundRect(Color.White, Offset(10 * cw, 10 * ch), Size(4 * cw, 4 * ch), CornerRadius(8f))
        GREEN_BASE.forEach { (r, c) ->
            drawCircle(ClassicGreenLight.copy(alpha = 0.5f), cw * 0.4f, cellCenter(r, c))
        }

        // ── Empty corners (top-right & bottom-left) ──
        val emptyColor = Color(0xFFE8E4DC)
        drawRoundRect(emptyColor, Offset(9 * cw, 0f), Size(6 * cw, 6 * ch), CornerRadius(8f))
        drawRoundRect(emptyColor, Offset(0f, 9 * ch), Size(6 * cw, 6 * ch), CornerRadius(8f))

        // ── Track cells ──
        TRACK.forEachIndexed { idx, cell ->
            val (r, c) = cell
            val isSafe = idx in SAFE_POSITIONS
            val bg = if (isSafe) SafeGold.copy(alpha = 0.3f) else TrackCell
            drawRoundRect(bg, cellRect(r, c).topLeft.toOffset(), Size(cw, ch), CornerRadius(1f))
            drawRoundRect(TrackBorder.copy(alpha = 0.4f), cellRect(r, c).topLeft.toOffset(), Size(cw, ch), CornerRadius(1f), style = Stroke(0.8f))
        }

        // ── Home columns ──
        RED_HOME.forEach { (r, c) ->
            drawRoundRect(ClassicRed.copy(alpha = 0.35f), cellRect(r, c).topLeft.toOffset(), Size(cw, ch), CornerRadius(2f))
            drawRoundRect(ClassicRed.copy(alpha = 0.15f), cellRect(r, c).topLeft.toOffset(), Size(cw, ch), CornerRadius(2f), style = Stroke(1f))
        }
        GREEN_HOME.forEach { (r, c) ->
            drawRoundRect(ClassicGreen.copy(alpha = 0.35f), cellRect(r, c).topLeft.toOffset(), Size(cw, ch), CornerRadius(2f))
            drawRoundRect(ClassicGreen.copy(alpha = 0.15f), cellRect(r, c).topLeft.toOffset(), Size(cw, ch), CornerRadius(2f), style = Stroke(1f))
        }

        // ── Home center triangle area ──
        val centerLeft = 7 * cw
        val centerTop = 7 * ch
        // Draw the four triangles pointing to center
        // Red triangle (top)
        val redTri = android.graphics.Path().apply {
            moveTo(centerLeft, centerTop)
            lineTo(centerLeft + cw, centerTop)
            lineTo(centerLeft + cw / 2, centerTop + ch / 2)
            close()
        }
        drawContext.canvas.nativeCanvas.drawPath(redTri, android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#D32F2F")
            alpha = 60
            isAntiAlias = true
        })

        // Green triangle (bottom)
        val greenTri = android.graphics.Path().apply {
            moveTo(centerLeft, centerTop + ch)
            lineTo(centerLeft + cw, centerTop + ch)
            lineTo(centerLeft + cw / 2, centerTop + ch / 2)
            close()
        }
        drawContext.canvas.nativeCanvas.drawPath(greenTri, android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#388E3C")
            alpha = 60
            isAntiAlias = true
        })

        // Center star
        val centerCx = centerLeft + cw / 2
        val centerCy = centerTop + ch / 2
        drawCircle(Color(0xFFFFF3E0), cw * 0.35f, Offset(centerCx, centerCy))
        drawCircle(SafeGold.copy(alpha = 0.6f), cw * 0.25f, Offset(centerCx, centerCy))
        drawContext.canvas.nativeCanvas.drawText(
            "\u2605",
            centerCx,
            centerCy + cw * 0.15f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F57F17")
                textSize = cw * 0.5f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            },
        )

        // ── Safe star markers ──
        SAFE_POSITIONS.forEach { pos ->
            val (r, c) = TRACK[pos]
            val center = cellCenter(r, c)
            drawContext.canvas.nativeCanvas.drawText(
                "\u2605",
                center.x,
                center.y + cw * 0.15f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#F9A825")
                    textSize = cw * 0.45f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                },
            )
        }

        // ── Entry arrow markers ──
        // Red entry at track 0 (6,1)
        val redEntryCenter = cellCenter(TRACK[RED_ENTRY][0], TRACK[RED_ENTRY][1])
        drawCircle(ClassicRed.copy(alpha = 0.3f), cw * 0.2f, redEntryCenter)
        // Green entry at track 26 (7,8)
        val greenEntryCenter = cellCenter(TRACK[GREEN_ENTRY][0], TRACK[GREEN_ENTRY][1])
        drawCircle(ClassicGreen.copy(alpha = 0.3f), cw * 0.2f, greenEntryCenter)

        // ── Grid lines (subtle) ──
        for (i in 0..GRID) {
            drawLine(TrackBorder.copy(alpha = 0.2f), Offset(i * cw, 0f), Offset(i * cw, size.height), 0.5f)
            drawLine(TrackBorder.copy(alpha = 0.2f), Offset(0f, i * ch), Offset(size.width, i * ch), 0.5f)
        }

        // ── Draw tokens ──
        fun drawToken(r: Int, c: Int, color: Color, tokenNum: Int, isOnBoard: Boolean) {
            val center = cellCenter(r, c)
            // Shadow
            drawCircle(Color.Black.copy(alpha = 0.12f), cw * 0.32f, Offset(center.x + 1.5f, center.y + 1.5f))
            // Outer ring
            drawCircle(color.copy(alpha = 0.25f), cw * 0.38f, center)
            // Main body
            drawCircle(color, cw * 0.3f, center)
            // Shine
            drawCircle(Color.White.copy(alpha = 0.45f), cw * 0.12f, Offset(center.x - cw * 0.07f, center.y - cw * 0.07f))
            // Inner highlight
            drawCircle(color.copy(alpha = 0.3f), cw * 0.15f, center)
            // Token number
            drawContext.canvas.nativeCanvas.drawText(
                "$tokenNum",
                center.x,
                center.y + cw * 0.1f,
                android.graphics.Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = cw * 0.24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                },
            )
        }

        // ── Base tokens ──
        fun drawBaseTokens(p: PlayerData, baseSlots: List<IntArray>, color: Color) {
            for (i in 0..3) {
                if (p.tokens[i].place == TokenPlace.BASE) {
                    val (r, c) = baseSlots[i]
                    drawToken(r, c, color, i + 1, false)
                }
            }
        }
        drawBaseTokens(p1, RED_BASE, p1.color)
        drawBaseTokens(p2, GREEN_BASE, p2.color)

        // ── Track tokens ──
        fun drawTrackTokens(p: PlayerData, color: Color) {
            for (i in 0..3) {
                val t = p.tokens[i]
                if (t.place == TokenPlace.TRACK && t.trackPos in 0..51) {
                    val (r, c) = TRACK[t.trackPos]
                    drawToken(r, c, color, i + 1, true)
                }
            }
        }
        drawTrackTokens(p1, p1.color)
        drawTrackTokens(p2, p2.color)

        // ── Home column tokens ──
        fun drawHomeTokens(p: PlayerData, homeCol: List<IntArray>, color: Color) {
            for (i in 0..3) {
                val t = p.tokens[i]
                if (t.place == TokenPlace.HOME_COL && t.homeStep in 0..5) {
                    val (r, c) = homeCol[t.homeStep]
                    drawToken(r, c, color, i + 1, true)
                }
            }
        }
        drawHomeTokens(p1, RED_HOME, p1.color)
        drawHomeTokens(p2, GREEN_HOME, p2.color)

        // ── Finished tokens (in center) ──
        for (i in 0..3) {
            if (p1.tokens[i].place == TokenPlace.FINISHED) {
                val cx = 7 * cw + cw * (0.25f + (i % 2) * 0.5f)
                val cy = 7 * ch + ch * (0.25f + (i / 2) * 0.5f)
                drawToken((cy / ch).toInt(), (cx / cw).toInt(), p1.color, i + 1, true)
            }
            if (p2.tokens[i].place == TokenPlace.FINISHED) {
                val cx = 7 * cw + cw * (0.25f + (i % 2) * 0.5f)
                val cy = 7 * ch + ch * (0.25f + (i / 2) * 0.5f)
                drawToken((cy / ch).toInt(), (cx / cw).toInt(), p2.color, i + 1, true)
            }
        }
    }
}

private fun Offset.toOffset() = this
