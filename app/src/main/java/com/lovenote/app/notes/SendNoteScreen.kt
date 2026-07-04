package com.lovenote.app.notes

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.wallpaper.NoteWallpaperService
import kotlinx.coroutines.launch

/** Background colors for each note style; the widget uses these too. */
val noteStyleColors = mapOf(
    "peach" to Color(0xFFFFDAB9),
    "rose" to Color(0xFFF8BBD0),
    "sky" to Color(0xFFB3E5FC),
    "mint" to Color(0xFFC8E6C9),
    "lavender" to Color(0xFFD1C4E9),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNoteScreen(
    repository: NoteRepository,
    onBack: () -> Unit,
    onHistoryClick: () -> Unit,
    onDrawClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(Note.DEFAULT_STYLE) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send a note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Note history",
                        )
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
            // Live preview of how the note will look on the partner's widget
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        noteStyleColors[style] ?: noteStyleColors.getValue(Note.DEFAULT_STYLE),
                        RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text.ifBlank { "Your note will look like this ❤" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xB3000000),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Note.STYLES.forEach { name ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(noteStyleColors.getValue(name), CircleShape)
                            .border(
                                width = if (style == name) 3.dp else 1.dp,
                                color = if (style == name) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0x33000000)
                                },
                                shape = CircleShape,
                            )
                            .clickable { style = name },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(Note.MAX_LENGTH) },
                label = { Text("Your note (${text.length}/${Note.MAX_LENGTH})") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            repository.send(text, style)
                            onBack()
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't send — try again"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = text.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "Sending…" else "Send to their home screen ❤")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDrawClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("🎨 Or draw a note instead")
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, NoteWallpaperService::class.java),
                        )
                    }
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Show their notes as my wallpaper")
            }
        }
    }
}
