package com.lovenote.app.games.ludo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.random.Random

// ─── Constants ────────────────────────────────────────────────────────────────

private const val GRID = 15
private const val CELLS = 52
private const val HOME_COL_LEN = 6

// ─── Colors ───────────────────────────────────────────────────────────────────

private val RedColor = Color(0xFFE53935)
private val RedLight = Color(0xFFFFCDD2)
private val RedDark = Color(0xFFC62828)
private val GreenColor = Color(0xFF43A047)
private val GreenLight = Color(0xFFC8E6C9)
private val GreenDark = Color(0xFF2E7D32)
private val BoardBg = Color(0xFFF5F5F0)
private val TrackCell = Color(0xFFEEEEEE)
private val SafeColor = Color(0xFFFFD54F)
private val HomeCenter = Color(0xFFFFF9C4)

// ─── Track coordinates (52 cells, clockwise) ──────────────────────────────────
private val TRACK = listOf(
    // Left side going up (Red side)
    intArrayOf(6, 1), intArrayOf(6, 2), intArrayOf(6, 3),
    intArrayOf(6, 4), intArrayOf(6, 5),
    intArrayOf(5, 6), intArrayOf(4, 6), intArrayOf(3, 6),
    intArrayOf(2, 6), intArrayOf(1, 6),
    // Top side going right
    intArrayOf(1, 7), intArrayOf(1, 8),
    intArrayOf(1, 9), intArrayOf(1, 10), intArrayOf(1, 11),
    intArrayOf(1, 12), intArrayOf(1, 13),
    intArrayOf(2, 13), intArrayOf(3, 13),
    // Right side going down (Green side)
    intArrayOf(4, 13), intArrayOf(5, 13),
    intArrayOf(6, 13), intArrayOf(6, 14),
    intArrayOf(7, 14), intArrayOf(7, 13),
    intArrayOf(7, 12), intArrayOf(7, 11),
    intArrayOf(7, 10), intArrayOf(7, 9),
    intArrayOf(7, 8),
    // Bottom side going left
    intArrayOf(8, 8), intArrayOf(8, 7),
    intArrayOf(8, 6), intArrayOf(8, 5),
    intArrayOf(8, 4), intArrayOf(8, 3),
    intArrayOf(8, 2), intArrayOf(8, 1),
    intArrayOf(9, 1),
    // Left side going down (bottom)
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

// ─── Home columns (6 cells each, leading to center) ──────────────────────────
private val RED_HOME = listOf(
    intArrayOf(1, 7), intArrayOf(2, 7), intArrayOf(3, 7),
    intArrayOf(4, 7), intArrayOf(5, 7), intArrayOf(6, 7),
)
private val GREEN_HOME = listOf(
    intArrayOf(13, 7), intArrayOf(12, 7), intArrayOf(11, 7),
    intArrayOf(10, 7), intArrayOf(9, 7), intArrayOf(8, 7),
)

// ─── Entry positions on the main track ────────────────────────────────────────
private const val RED_ENTRY = 0
private const val GREEN_ENTRY = 26

// ─── Base positions (where tokens sit before entering) ────────────────────────
private val RED_BASE = listOf(
    intArrayOf(2, 2), intArrayOf(2, 4), intArrayOf(4, 2), intArrayOf(4, 4),
)
private val GREEN_BASE = listOf(
    intArrayOf(10, 10), intArrayOf(10, 12), intArrayOf(12, 10), intArrayOf(12, 12),
)

// ─── Safe positions (tokens can't be captured here) ───────────────────────────
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
) {
    val p1 = remember { PlayerData(myName, RedColor, RedLight, RedDark, baseSlots = RED_BASE, entry = RED_ENTRY, homeCol = RED_HOME) }
    val p2 = remember { PlayerData(partnerName, GreenColor, GreenLight, GreenDark, baseSlots = GREEN_BASE, entry = GREEN_ENTRY, homeCol = GREEN_HOME) }

    var turn by remember { mutableIntStateOf(1) }
    var diceValue by remember { mutableIntStateOf(1) }
    var phase by remember { mutableStateOf("roll") } // roll | select | animate | bonus | gameover
    var selectedToken by remember { mutableIntStateOf(-1) }
    var winner by remember { mutableStateOf<PlayerData?>(null) }
    val diceHistory = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()
    val animProgress = remember { Animatable(0f) }

    fun current() = if (turn == 1) p1 else p2
    fun opponent() = if (turn == 1) p2 else p1

    fun finishedCount(p: PlayerData) = p.tokens.count { it.place == TokenPlace.FINISHED }

    fun allFinished(p: PlayerData) = finishedCount(p) == 4

    fun baseTokens(p: PlayerData) = p.tokens.withIndex().filter { it.value.place == TokenPlace.BASE }.map { it.index }

    fun trackTokens(p: PlayerData) = p.tokens.withIndex().filter { it.value.place == TokenPlace.TRACK }.map { it.index }

    fun homeTokens(p: PlayerData) = p.tokens.withIndex().filter { it.value.place == TokenPlace.HOME_COL }.map { it.index }

    fun availableMoves(p: PlayerData, dice: Int): List<Int> {
        val moves = mutableListOf<Int>()
        for (i in 0..3) {
            val t = p.tokens[i]
            when (t.place) {
                TokenPlace.BASE -> {
                    if (dice == 6) moves.add(i)
                }
                TokenPlace.TRACK -> {
                    val dest = t.trackPos + dice
                    if (dest <= 51) {
                        moves.add(i)
                    } else if (dest in 52..57) {
                        moves.add(i)
                    }
                }
                TokenPlace.HOME_COL -> {
                    val dest = t.homeStep + dice
                    if (dest <= 5) moves.add(i)
                    else if (dest == 6) moves.add(i)
                }
                TokenPlace.FINISHED -> {}
            }
        }
        return moves
    }

    fun findCaptures(p: PlayerData, pos: Int): List<Int> {
        val captured = mutableListOf<Int>()
        for (i in 0..3) {
            val opp = if (p == p1) p2 else p1
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
                val caps = findCaptures(p, p.entry)
                for (ci in caps) {
                    val opp = if (p == p1) p2 else p1
                    opp.tokens[ci] = Token()
                }
            }
            TokenPlace.TRACK -> {
                val dest = t.trackPos + diceValue
                if (dest <= 51) {
                    t.trackPos = dest
                    val caps = findCaptures(p, dest)
                    for (ci in caps) {
                        val opp = if (p == p1) p2 else p1
                        opp.tokens[ci] = Token()
                    }
                } else {
                    val homeSteps = dest - 52
                    t.place = TokenPlace.HOME_COL
                    t.trackPos = -1
                    t.homeStep = homeSteps
                }
            }
            TokenPlace.HOME_COL -> {
                val dest = t.homeStep + diceValue
                if (dest == 6) {
                    t.place = TokenPlace.FINISHED
                    t.homeStep = -1
                } else {
                    t.homeStep = dest
                }
            }
            TokenPlace.FINISHED -> {}
        }
        if (allFinished(p)) {
            p.wins++
            winner = p
            phase = "gameover"
        } else if (diceValue == 6) {
            phase = "roll"
        } else {
            turn = if (turn == 1) 2 else 1
            phase = "roll"
        }
    }

    fun rollDice() {
        if (phase != "roll") return
        diceValue = Random.nextInt(1, 7)
        diceHistory.add(diceValue)
        val p = current()
        val moves = availableMoves(p, diceValue)
        if (moves.isEmpty()) {
            turn = if (turn == 1) 2 else 1
            phase = "roll"
        } else if (moves.size == 1) {
            selectedToken = moves[0]
            phase = "animate"
            scope.launch {
                val t = p.tokens[moves[0]]
                val startTrack = t.trackPos
                val startHome = t.homeStep
                val startPlace = t.place
                animProgress.snapTo(0f)
                animProgress.animateTo(1f, animationSpec = tween(300 * diceValue, easing = LinearEasing))
                executeMove(moves[0])
                phase = "roll"
            }
        } else {
            phase = "select"
        }
    }

    fun selectToken(idx: Int) {
        val p = current()
        selectedToken = idx
        phase = "animate"
        scope.launch {
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, animationSpec = tween(300 * diceValue, easing = LinearEasing))
            executeMove(idx)
            phase = "roll"
        }
    }

    fun resetGame() {
        p1.tokens.forEachIndexed { i, _ -> p1.tokens[i] = Token() }
        p2.tokens.forEachIndexed { i, _ -> p2.tokens[i] = Token() }
        turn = 1
        diceValue = 1
        phase = "roll"
        selectedToken = -1
        winner = null
        diceHistory.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ludo") },
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
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Player scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlayerPill(name = p1.name, color = p1.color, wins = p1.wins, isTurn = turn == 1 && phase != "gameover")
                PlayerPill(name = p2.name, color = p2.color, wins = p2.wins, isTurn = turn == 2 && phase != "gameover")
            }

            Spacer(Modifier.height(8.dp))

            // Board
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                LudoBoardCanvas(
                    p1 = p1,
                    p2 = p2,
                    diceValue = diceValue,
                    turn = turn,
                    selectedIdx = if (phase == "select") -1 else selectedToken,
                    animProgress = animProgress.value,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Bottom area
            when (phase) {
                "roll" -> {
                    DiceRollButton(
                        value = diceValue,
                        onClick = ::rollDice,
                        enabled = true,
                        isSix = diceHistory.lastOrNull() == 6,
                    )
                    Spacer(Modifier.height(8.dp))
                    val cur = current()
                    Text(
                        text = "${cur.name}'s turn — Roll the dice!",
                        style = MaterialTheme.typography.titleSmall,
                        color = cur.color,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                "select" -> {
                    val cur = current()
                    val moves = availableMoves(cur, diceValue)
                    Text(
                        text = "${cur.name} rolled $diceValue — Tap a token to move:",
                        style = MaterialTheme.typography.titleSmall,
                        color = cur.color,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (idx in moves) {
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
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = ::resetGame) { Text("Rematch") }
                        OutlinedButton(onClick = onBack) { Text("Back") }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (winner != null && phase == "gameover") {
        AlertDialog(
            onDismissRequest = { },
            icon = { Text("🏆", fontSize = 40.sp) },
            title = { Text("${winner!!.name} wins!") },
            text = { Text("Amazing game! Ready for another round?") },
            confirmButton = {
                Button(onClick = { resetGame(); winner = null }) { Text("Rematch!") }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text("Back to Hub") }
            },
        )
    }
}

// ─── Player Pill ──────────────────────────────────────────────────────────────

@Composable
private fun PlayerPill(name: String, color: Color, wins: Int, isTurn: Boolean) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTurn) color.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        border = if (isTurn) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text(
                "$wins",
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─── Dice Button ──────────────────────────────────────────────────────────────

@Composable
private fun DiceRollButton(value: Int, onClick: () -> Unit, enabled: Boolean, isSix: Boolean) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(6.dp, CircleShape)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSix) SafeColor else MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val dotRadius = size.minDimension / 10
            val cx = size.width / 2
            val cy = size.height / 2
            val off = size.minDimension / 4

            fun dot(x: Float, y: Float) {
                drawContext.canvas.nativeCanvas.drawCircle(
                    x, y, dotRadius,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        isAntiAlias = true
                    },
                )
            }

            when (value) {
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
    diceValue: Int,
    turn: Int,
    selectedIdx: Int,
    animProgress: Float,
) {
    val p1TokenColor = p1.color
    val p2TokenColor = p2.color
    val p1BaseColor = p1.colorLight
    val p2BaseColor = p2.colorLight

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellW = size.width / GRID
        val cellH = size.height / GRID

        fun cellCenter(r: Int, c: Int) = Offset(c * cellW + cellW / 2, r * cellH + cellH / 2)
        fun cellRect(r: Int, c: Int) = Rect(c * cellW, r * cellH, (c + 1) * cellW, (r + 1) * cellH)

        // ── Background ──
        drawRect(BoardBg)

        // ── Base areas (6x6 corners) ──
        // Red base (top-left)
        drawRoundRect(RedLight, Offset(0f, 0f), Size(6 * cellW, 6 * cellH), CornerRadius(8f))
        drawRoundRect(RedDark.copy(alpha = 0.15f), Offset(cellW, cellH), Size(4 * cellW, 4 * cellH), CornerRadius(6f))

        // Green base (bottom-right)
        drawRoundRect(GreenLight, Offset(9 * cellW, 9 * cellH), Size(6 * cellW, 6 * cellH), CornerRadius(8f))
        drawRoundRect(GreenDark.copy(alpha = 0.15f), Offset(10 * cellW, 10 * cellH), Size(4 * cellW, 4 * cellH), CornerRadius(6f))

        // Empty corners (top-right, bottom-left)
        drawRoundRect(Color(0xFFE0E0E0).copy(alpha = 0.3f), Offset(9 * cellW, 0f), Size(6 * cellW, 6 * cellH), CornerRadius(8f))
        drawRoundRect(Color(0xFFE0E0E0).copy(alpha = 0.3f), Offset(0f, 9 * cellW), Size(6 * cellW, 6 * cellH), CornerRadius(8f))

        // ── Home columns ──
        RED_HOME.forEach { (r, c) ->
            drawRoundRect(RedColor.copy(alpha = 0.25f), cellRect(r, c).topLeft.toOffset(), Size(cellW, cellH), CornerRadius(2f))
        }
        GREEN_HOME.forEach { (r, c) ->
            drawRoundRect(GreenColor.copy(alpha = 0.25f), cellRect(r, c).topLeft.toOffset(), Size(cellW, cellH), CornerRadius(2f))
        }

        // ── Home center ──
        drawRoundRect(
            HomeCenter,
            Offset(7 * cellW, 7 * cellH),
            Size(cellW, cellH),
            CornerRadius(4f),
        )
        // Center triangle indicators
        val centerCx = 7 * cellW + cellW / 2
        val centerCy = 7 * cellH + cellH / 2
        drawContext.canvas.nativeCanvas.drawText(
            "HOME",
            centerCx,
            centerCy + 4,
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(180, 100, 100, 100)
                textSize = cellW * 0.3f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            },
        )

        // ── Track cells ──
        TRACK.forEachIndexed { idx, cell ->
            val (r, c) = cell
            val bg = if (idx in SAFE_POSITIONS) SafeColor.copy(alpha = 0.4f) else TrackCell
            drawRoundRect(bg, cellRect(r, c).topLeft.toOffset(), Size(cellW, cellH), CornerRadius(1f))
            drawRoundRect(
                Color.LightGray.copy(alpha = 0.5f),
                cellRect(r, c).topLeft.toOffset(),
                Size(cellW, cellH),
                CornerRadius(1f),
                style = Stroke(width = 0.5f),
            )
        }

        // ── Grid lines ──
        for (i in 0..GRID) {
            drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(i * cellW, 0f), Offset(i * cellW, size.height), 0.5f)
            drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(0f, i * cellH), Offset(size.width, i * cellH), 0.5f)
        }

        // ── Safe stars ──
        SAFE_POSITIONS.forEach { pos ->
            val (r, c) = TRACK[pos]
            val center = cellCenter(r, c)
            drawContext.canvas.nativeCanvas.drawText(
                "★",
                center.x,
                center.y + cellW * 0.15f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(160, 200, 150, 0)
                    textSize = cellW * 0.5f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                },
            )
        }

        // ── Base tokens (not yet on track) ──
        fun drawBaseTokens(p: PlayerData, baseSlots: List<IntArray>, color: Color) {
            for (i in 0..3) {
                if (p.tokens[i].place == TokenPlace.BASE) {
                    val (r, c) = baseSlots[i]
                    val center = cellCenter(r, c)
                    drawCircle(color.copy(alpha = 0.3f), cellW * 0.38f, center)
                    drawCircle(color, cellW * 0.3f, center)
                    drawCircle(Color.White.copy(alpha = 0.4f), cellW * 0.12f, Offset(center.x - cellW * 0.08f, center.y - cellW * 0.08f))
                }
            }
        }

        drawBaseTokens(p1, RED_BASE, p1TokenColor)
        drawBaseTokens(p2, GREEN_BASE, p2TokenColor)

        // ── Track tokens ──
        fun drawTrackToken(p: PlayerData, color: Color, label: String) {
            for (i in 0..3) {
                val t = p.tokens[i]
                if (t.place == TokenPlace.TRACK) {
                    val pos = t.trackPos
                    if (pos in 0..51) {
                        val (r, c) = TRACK[pos]
                        val center = cellCenter(r, c)
                        val isCap = p == p1 && p2.tokens.any { it.place == TokenPlace.TRACK && it.trackPos == pos }
                            || p == p2 && p1.tokens.any { it.place == TokenPlace.TRACK && it.trackPos == pos }
                        drawCircle(color.copy(alpha = 0.25f), cellW * 0.42f, center)
                        drawCircle(color, cellW * 0.34f, center)
                        drawCircle(Color.White.copy(alpha = 0.3f), cellW * 0.1f, Offset(center.x - cellW * 0.08f, center.y - cellW * 0.08f))
                        // Token label
                        drawContext.canvas.nativeCanvas.drawText(
                            "${i + 1}",
                            center.x,
                            center.y + cellW * 0.12f,
                            android.graphics.Paint().apply {
                                this.color = android.graphics.Color.WHITE
                                textSize = cellW * 0.28f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                isFakeBoldText = true
                            },
                        )
                    }
                }
            }
        }

        drawTrackToken(p1, p1TokenColor, "1")
        drawTrackToken(p2, p2TokenColor, "2")

        // ── Home column tokens ──
        fun drawHomeTokens(p: PlayerData, homeCol: List<IntArray>, color: Color) {
            for (i in 0..3) {
                val t = p.tokens[i]
                if (t.place == TokenPlace.HOME_COL && t.homeStep in 0..5) {
                    val (r, c) = homeCol[t.homeStep]
                    val center = cellCenter(r, c)
                    drawCircle(color.copy(alpha = 0.3f), cellW * 0.4f, center)
                    drawCircle(color, cellW * 0.32f, center)
                    drawCircle(Color.White.copy(alpha = 0.3f), cellW * 0.1f, Offset(center.x - cellW * 0.08f, center.y - cellW * 0.08f))
                }
            }
        }

        drawHomeTokens(p1, RED_HOME, p1TokenColor)
        drawHomeTokens(p2, GREEN_HOME, p2TokenColor)

        // ── Finished tokens (center) ──
        for (i in 0..3) {
            if (p1.tokens[i].place == TokenPlace.FINISHED) {
                val cx = 7 * cellW + cellW * (0.25f + (i % 2) * 0.5f)
                val cy = 7 * cellH + cellH * (0.25f + (i / 2) * 0.5f)
                drawCircle(p1TokenColor, cellW * 0.2f, Offset(cx, cy))
            }
            if (p2.tokens[i].place == TokenPlace.FINISHED) {
                val cx = 7 * cellW + cellW * (0.25f + (i % 2) * 0.5f)
                val cy = 7 * cellH + cellH * (0.25f + (i / 2) * 0.5f)
                drawCircle(p2TokenColor, cellW * 0.15f, Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = 0.3f), cellW * 0.06f, Offset(cx - cellW * 0.04f, cy - cellW * 0.04f))
            }
        }
    }
}

private fun Offset.toOffset() = this
