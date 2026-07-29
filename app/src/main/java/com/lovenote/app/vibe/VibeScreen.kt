package com.lovenote.app.vibe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lovenote.app.rituals.ritualTemplates
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VibeScreen(
    repository: VibeRepository,
    onRitualDetail: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val songs by repository.songs().collectAsState(initial = emptyList())
    val rituals by repository.rituals().collectAsState(initial = emptyList())
    val partner by repository.partnerProfile().collectAsState(initial = null)
    var showShareSheet by remember { mutableStateOf(false) }
    var showCreateRitual by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var playingSongId by remember { mutableStateOf<String?>(null) }

    if (showShareSheet) {
        ShareSongSheet(
            onDismiss = { showShareSheet = false },
            onShare = { uri, source, title, artist, albumArt ->
                scope.launch { runCatching { repository.shareSong(uri, source, title, artist, albumArt) } }
                showShareSheet = false
            },
        )
    }

    if (showCreateRitual) {
        CreateRitualSheet(
            onDismiss = { showCreateRitual = false },
            onCreate = { name, desc, freq, action, time, days, prompt ->
                scope.launch { runCatching { repository.createRitual(name, desc, freq, action, time, days, prompt) } }
            },
        )
    }

    if (showTemplates) {
        TemplatePickerDialog(
            onDismiss = { showTemplates = false },
            onSelect = { template ->
                showTemplates = false
                scope.launch {
                    runCatching {
                        val days = when (template.frequency) {
                            "weekly" -> listOf(1, 3, 5)
                            "monthly" -> listOf(1)
                            else -> emptyList()
                        }
                        repository.createRitual(
                            template.name, template.description,
                            template.frequency, template.actionType,
                            "09:00", days, template.prompt,
                        )
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Vibe") })
        },
        floatingActionButton = {
            FilledIconButton(
                onClick = { showShareSheet = true },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Share a song")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "\uD83C\uDFB5 Our Soundtrack",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
            }

            if (songs.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "\uD83C\uDFB5",
                        title = "No songs yet",
                        subtitle = "Share a song with your partner \u2014 it appears here instantly.",
                        actionLabel = "Share your first song",
                        onAction = { showShareSheet = true },
                    )
                }
            } else {
                items(songs.take(5)) { song ->
                    SongCard(
                        song = song,
                        isPlaying = playingSongId == song.id,
                        isMine = song.sharedBy == repository.myUid,
                        partnerName = partner?.name,
                        onTap = {
                            playingSongId = if (playingSongId == song.id) null else song.id
                        },
                        onReact = { emoji ->
                            val newReaction = if (song.reaction == emoji) null else emoji
                            scope.launch { runCatching { repository.reactToSong(song.id, newReaction) } }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (songs.size > 5) {
                    item {
                        Text(
                            text = "+ ${songs.size - 5} more songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\uD83D\uDD01 Our Rituals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showTemplates = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("Templates", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { showCreateRitual = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("+ New", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val activeRituals = rituals.filter { it.active }
            if (activeRituals.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "\uD83D\uDD01",
                        title = "No rituals yet",
                        subtitle = "Create a custom ritual or pick from templates to build daily connection habits.",
                        actionLabel = "Browse templates",
                        onAction = { showTemplates = true },
                    )
                }
            } else {
                items(activeRituals) { ritual ->
                    RitualCard(ritual = ritual, onClick = { onRitualDetail(ritual.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SongCard(
    song: SharedSong,
    isPlaying: Boolean,
    isMine: Boolean,
    partnerName: String?,
    onTap: () -> Unit,
    onReact: (String) -> Unit,
) {
    val bgColor = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!song.albumArtUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = song.albumArtUrl,
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isMine) "You" else (partnerName ?: "Partner"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            if (isPlaying) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }

            val emojiOptions = listOf("\u2764\uFE0F", "\uD83C\uDF89", "\uD83D\uDE22", "\uD83D\uDD25")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width(80.dp),
            ) {
                emojiOptions.forEach { emoji ->
                    val isActive = song.reaction == emoji
                    Text(
                        text = emoji,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReact(emoji) }
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color(0x00000000),
                                CircleShape,
                            )
                            .padding(2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RitualCard(ritual: Ritual, onClick: () -> Unit) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ritual.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    Badge(ritual.frequency.replaceFirstChar { it.uppercase() })
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\uD83D\uDD25 ${ritual.streak} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    val status = if (ritual.lastCompletedAtMillis != null) {
                        val lastDay = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            .format(Date(ritual.lastCompletedAtMillis))
                        if (lastDay == today) "Done today" else "Due"
                    } else "Due"
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status == "Done today") MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(text = "\u2764\uFE0F", fontSize = 20.sp)
        }
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun EmptyStateCard(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun TemplatePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (com.lovenote.app.rituals.RitualTemplate) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ritual Templates") },
        text = {
            Column {
                Text(
                    text = "Choose a pre-built ritual inspired by relationship science.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ritualTemplates.forEach { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(template) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Badge(template.frequency.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
