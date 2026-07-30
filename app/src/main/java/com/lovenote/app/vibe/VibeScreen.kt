package com.lovenote.app.vibe

import android.content.Intent
import android.net.Uri
import com.lovenote.app.R
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.lovenote.app.rituals.ritualTemplates
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VibeScreen(
    vm: VibeViewModel,
    onRitualDetail: (String) -> Unit,
    onShareSong: (uri: String, source: String, title: String, artist: String, albumArtUrl: String?, audioUrl: String?) -> Unit = { _, _, _, _, _, _ -> },
) {
    val repository = vm.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val songs by repository.songs().collectAsState(initial = emptyList())
    val rituals by repository.rituals().collectAsState(initial = emptyList())
    val partner by repository.partnerProfile().collectAsState(initial = null)
    var showShareSheet by remember { mutableStateOf(false) }
    var showCreateRitual by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }

    val player = remember { ExoPlayer.Builder(context).build() }
    var playingSong by remember { mutableStateOf<SharedSong?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(playingSong) {
        val url = playingSong?.audioUrl
        if (url != null) {
            player.apply {
                stop()
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
                play()
            }
        } else {
            player.stop()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    playingSong = null
                    isPlaying = false
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    if (showShareSheet) {
        ShareSongSheet(
            onDismiss = { showShareSheet = false },
            onShare = { uri, source, title, artist, albumArt, audioUrl ->
                onShareSong(uri, source, title, artist, albumArt, audioUrl)
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

    Box {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.vibe_title)) })
            },
            floatingActionButton = {
                FilledIconButton(
                    onClick = { showShareSheet = true },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.vibe_share_song_description))
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = if (playingSong != null) 56.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.vibe_our_soundtrack),
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
                            title = stringResource(R.string.vibe_no_songs_title),
                            subtitle = stringResource(R.string.vibe_no_songs_subtitle),
                            actionLabel = stringResource(R.string.vibe_share_first_song),
                            onAction = { showShareSheet = true },
                        )
                    }
                } else {
                    items(songs.take(5)) { song ->
                        SongCard(
                            song = song,
                            isMine = song.sharedBy == repository.myUid,
                            partnerName = partner?.name,
                            isNowPlaying = playingSong?.id == song.id,
                            onPlay = {
                                if (song.audioUrl != null) {
                                    if (playingSong?.id == song.id) {
                                        if (isPlaying) player.pause() else player.play()
                                    } else {
                                        playingSong = song
                                    }
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(song.uri))
                                    runCatching<Unit> { context.startActivity(intent) }
                                }
                            },
                            onReact = { emoji ->
                                val newReaction = if (song.reaction == emoji) null else emoji
                                scope.launch { runCatching { repository.reactToSong(song.id, newReaction) } }
                            },
                            onDelete = {
                                if (playingSong?.id == song.id) {
                                    player.stop()
                                    playingSong = null
                                }
                                scope.launch { runCatching { repository.deleteSong(song.id) } }
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
                            text = stringResource(R.string.vibe_our_rituals),
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
                                Text(stringResource(R.string.vibe_templates_button), style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { showCreateRitual = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text(stringResource(R.string.vibe_new_ritual_button), style = MaterialTheme.typography.labelSmall)
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
                            title = stringResource(R.string.vibe_no_rituals_title),
                            subtitle = stringResource(R.string.vibe_no_rituals_subtitle),
                            actionLabel = stringResource(R.string.vibe_browse_templates),
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

        playingSong?.let { song ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!song.albumArtUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = song.albumArtUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = {
                        if (isPlaying) player.pause() else player.play()
                    }) {
                        Text(
                            text = if (isPlaying) "\u23F8" else "\u25B6",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {
                        player.stop()
                        playingSong = null
                    }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.vibe_stop_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SongCard(
    song: SharedSong,
    isMine: Boolean,
    partnerName: String?,
    isNowPlaying: Boolean,
    onPlay: () -> Unit,
    onReact: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val bgColor = if (isNowPlaying) {
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
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center,
            ) {
                if (!song.albumArtUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = song.albumArtUrl,
                        contentDescription = stringResource(R.string.vibe_album_art_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPlay() },
            ) {
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
                    text = if (isMine) stringResource(R.string.vibe_you) else (partnerName ?: stringResource(R.string.vibe_partner)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            val emojiOptions = listOf("\u2764\uFE0F", "\uD83C\uDF89", "\uD83D\uDE22", "\uD83D\uDD25")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width(70.dp),
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

            if (isMine) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.vibe_remove_song_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onDelete() },
                )
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
                        if (lastDay == today) stringResource(R.string.vibe_done_today) else stringResource(R.string.vibe_due)
                    } else stringResource(R.string.vibe_due)
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
        title = { Text(stringResource(R.string.vibe_ritual_templates_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.vibe_ritual_templates_description),
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.vibe_cancel)) }
        },
    )
}
