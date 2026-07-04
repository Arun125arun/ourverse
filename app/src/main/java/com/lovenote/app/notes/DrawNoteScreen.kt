package com.lovenote.app.notes

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class DrawStroke(val color: Color) {
    val points = mutableStateListOf<Offset>()
}

private val PEN_COLORS = listOf(
    Color(0xFF3E2723), // ink
    Color(0xFFE91E63), // pink
    Color(0xFF6A4FDB), // purple
    Color(0xFF1976D2), // blue
    Color(0xFF2E7D32), // green
    Color.White,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawNoteScreen(
    repository: NoteRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var style by remember { mutableStateOf(Note.DEFAULT_STYLE) }
    var pen by remember { mutableStateOf(PEN_COLORS.first()) }
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Draw a note 🎨") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { strokes.clear() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear drawing")
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
        ) {
            // The drawing card — same color card the widget will show
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        noteStyleColors[style] ?: noteStyleColors.getValue(Note.DEFAULT_STYLE),
                    )
                    .onSizeChanged { canvasSize = it },
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pen) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    strokes.add(DrawStroke(pen).also { it.points.add(offset) })
                                },
                                onDrag = { change, _ ->
                                    strokes.lastOrNull()?.points?.add(change.position)
                                },
                            )
                        },
                ) {
                    strokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val path = Path()
                            stroke.points.forEachIndexed { i, p ->
                                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                            }
                            drawPath(
                                path = path,
                                color = stroke.color,
                                style = Stroke(
                                    width = 11f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )
                        } else if (stroke.points.size == 1) {
                            drawCircle(stroke.color, radius = 5.5f, center = stroke.points[0])
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Pen", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PEN_COLORS.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (pen == color) 3.dp else 1.dp,
                                color = if (pen == color) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0x33888888)
                                },
                                shape = CircleShape,
                            )
                            .clickable { pen = color },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Card color", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Note.STYLES.forEach { name ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(noteStyleColors.getValue(name), CircleShape)
                            .border(
                                width = if (style == name) 3.dp else 1.dp,
                                color = if (style == name) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0x33888888)
                                },
                                shape = CircleShape,
                            )
                            .clickable { style = name },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    sending = true
                    error = null
                    scope.launch {
                        val doodle = renderDoodle(strokes.toList(), style, canvasSize)
                        val result = runCatching { repository.send("", style, doodle) }
                        sending = false
                        if (result.isSuccess) {
                            onBack()
                        } else {
                            error = "Couldn't send — check your connection and try again"
                        }
                    }
                },
                enabled = strokes.isNotEmpty() && !sending && canvasSize.width > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (sending) "Sending…" else "Send to their home screen 🎨")
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Renders the strokes onto a bitmap of the note card and encodes it. */
private suspend fun renderDoodle(
    strokes: List<DrawStroke>,
    style: String,
    canvasSize: IntSize,
): String = withContext(Dispatchers.Default) {
    val width = 700
    val height = (width * canvasSize.height / canvasSize.width.coerceAtLeast(1))
        .coerceIn(100, 1400)
    val scale = width.toFloat() / canvasSize.width
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val background = noteStyleColors[style] ?: noteStyleColors.getValue(Note.DEFAULT_STYLE)
    canvas.drawColor(background.toArgb())
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.style = android.graphics.Paint.Style.STROKE
        strokeWidth = 11f * scale
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    for (stroke in strokes) {
        paint.color = stroke.color.toArgb()
        val path = android.graphics.Path()
        stroke.points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x * scale, p.y * scale)
            else path.lineTo(p.x * scale, p.y * scale)
        }
        if (stroke.points.size == 1) {
            canvas.drawCircle(
                stroke.points[0].x * scale,
                stroke.points[0].y * scale,
                5.5f * scale,
                android.graphics.Paint(paint).apply {
                    this.style = android.graphics.Paint.Style.FILL
                },
            )
        } else {
            canvas.drawPath(path, paint)
        }
    }
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
    Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}
