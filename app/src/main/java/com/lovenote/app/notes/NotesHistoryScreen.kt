package com.lovenote.app.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHistoryScreen(
    repository: NoteRepository,
    onBack: () -> Unit,
) {
    val notes by repository.history().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Our notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                val beat by rememberInfiniteTransition(label = "empty")
                    .animateFloat(
                        initialValue = 1f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "beat",
                    )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\u2764",
                        fontSize = 42.sp,
                        modifier = Modifier
                            .scale(beat)
                            .padding(vertical = 0.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "No notes yet — send the first one ❤",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note, mine = note.senderUid == repository.myUid)
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, mine: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                noteStyleColors[note.style] ?: noteStyleColors.getValue(Note.DEFAULT_STYLE),
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        // Decode off the main thread so scrolling past doodles never janks.
        val doodle = note.doodle?.let { encoded ->
            androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                initialValue = null,
                key1 = note.id,
            ) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    runCatching {
                        val bytes =
                            android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?.asImageBitmap()
                    }.getOrNull()
                }
            }.value
        }
        if (doodle != null) {
            androidx.compose.foundation.Image(
                bitmap = doodle,
                contentDescription = "Doodle",
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(8.dp))
        val time = note.sentAt?.toDate()?.let {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(it)
        } ?: ""
        Text(
            text = (if (mine) "You" else "Them ❤") + (if (time.isNotEmpty()) " · $time" else ""),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
