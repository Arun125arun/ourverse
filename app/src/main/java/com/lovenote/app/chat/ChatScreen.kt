package com.lovenote.app.chat

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.File
import com.google.firebase.Timestamp
import com.lovenote.app.R
import com.lovenote.app.notify.AppVisibility
import com.lovenote.app.notify.Notifier
import com.lovenote.app.ui.Avatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val REACTION_EMOJIS = listOf("❤", "😂", "😍", "😢", "👍")
private const val TYPING_VISIBLE_MILLIS = 6_000L
private const val TYPING_HEARTBEAT_MILLIS = 2_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    repository: ChatRepository,
    onSendNoteClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by repository.messages().collectAsState(initial = emptyList())
    val partnerTypingAt by repository.partnerTypingAt().collectAsState(initial = null)
    val anniversary by repository.anniversaryMillis().collectAsState(initial = null)
    val partner by repository.partnerProfile().collectAsState(initial = null)
    var viewingPhoto by remember { mutableStateOf<Message?>(null) }
    var input by remember { mutableStateOf("") }
    var reactingTo by remember { mutableStateOf<String?>(null) }
    var lastHeartbeat by remember { mutableStateOf(0L) }

    // Read receipts: stamp partner messages as seen while the chat is open.
    LaunchedEffect(messages) {
        runCatching { repository.markPartnerMessagesSeen(messages) }
    }

    DisposableEffect(Unit) {
        AppVisibility.chatVisible = true
        onDispose { AppVisibility.chatVisible = false }
    }

    // Typing indicator visibility with expiry.
    var partnerTyping by remember { mutableStateOf(false) }
    LaunchedEffect(partnerTypingAt) {
        val at = partnerTypingAt
        val remaining = if (at == null) 0 else at + TYPING_VISIBLE_MILLIS - System.currentTimeMillis()
        if (remaining > 0) {
            partnerTyping = true
            delay(remaining)
        }
        partnerTyping = false
    }

    var pendingPhoto by remember { mutableStateOf<String?>(null) }
    var pendingOnce by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableStateOf(0) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    val recorder = remember { VoiceRecorder(context) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                pendingPhoto = runCatching { PhotoEncoder.encode(context, uri) }.getOrNull()
            }
        }
    }

    var cameraTarget by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = cameraTarget
        if (saved && uri != null) {
            scope.launch {
                pendingPhoto = runCatching { PhotoEncoder.encode(context, uri) }.getOrNull()
            }
        }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "com.lovenote.app.fileprovider", file)
        cameraTarget = uri
        cameraLauncher.launch(uri)
    }

    fun stopAndSendVoice() {
        val result = runCatching { recorder.stop() }.getOrNull()
        recording = false
        result?.let { (audio, duration) ->
            scope.launch { runCatching { repository.sendVoice(audio, duration) } }
        }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            runCatching { recorder.start() }.onSuccess {
                recording = true
                recordSeconds = 0
            }
        }
    }

    // Recording timer with auto-stop at the length limit.
    LaunchedEffect(recording) {
        while (recording) {
            delay(1_000)
            recordSeconds++
            if (recordSeconds >= VoiceRecorder.MAX_SECONDS) stopAndSendVoice()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            name = partner?.name ?: "",
                            photoUrl = partner?.photoUrl ?: "",
                            size = 38.dp,
                        )
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = partner?.name?.takeIf { it.isNotBlank() } ?: "OurVerse ❤",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            val subtitle = when {
                                partnerTyping -> "typing…"
                                else -> presenceLabel(partner?.lastActiveMillis)
                                    ?: anniversary?.let { "Day ${daysTogether(it)} together ❤" }
                            }
                            subtitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (it == "Active now" || it == "typing…") {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            runCatching { repository.send("❤ Thinking of you") }
                            Notifier.vibrate(context)
                        }
                    }) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Send a nudge",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onSendNoteClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Send a note")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Say hi to your partner ❤",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            } else {
                val newestSeenMineId = messages
                    .firstOrNull { it.isMine(repository.myUid) && it.seen }
                    ?.id
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(messages, key = { _, m -> m.id }) { index, message ->
                        // reverseLayout: chip rendered above the bubble marks a new day
                        val startsNewDay = index == messages.lastIndex ||
                            !sameDay(message.sentAt, messages[index + 1].sentAt)
                        // reversed list: index-1 is the *next* (newer) message
                        val lastOfRun = index == 0 ||
                            messages[index - 1].senderUid != message.senderUid
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (startsNewDay) DayChip(dayLabel(message.sentAt))
                            MessageRow(
                                message = message,
                                mine = message.isMine(repository.myUid),
                                showCaption = lastOfRun,
                                showSeen = message.id == newestSeenMineId,
                                onPhotoClick = {
                                    if (!message.onceConsumed) viewingPhoto = message
                                },
                                playingVoice = playingVoiceId == message.id,
                                onVoiceToggle = {
                                    val nowPlaying = VoicePlayer.toggle(
                                        context,
                                        message.id,
                                        message.body,
                                    ) { playingVoiceId = null }
                                    playingVoiceId = if (nowPlaying) message.id else null
                                },
                                reactionPickerOpen = reactingTo == message.id,
                                onLongPress = { reactingTo = message.id },
                                onDismissPicker = { reactingTo = null },
                                onDelete = {
                                    reactingTo = null
                                    scope.launch { runCatching { repository.delete(message.id) } }
                                },
                                onReact = { emoji ->
                                    reactingTo = null
                                    scope.launch {
                                        val current = message.reactions[repository.myUid]
                                        runCatching {
                                            repository.react(
                                                message.id,
                                                if (current == emoji) null else emoji,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Photo preview strip with the view-once toggle.
            pendingPhoto?.let { encoded ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val thumb = remember(encoded) {
                            runCatching {
                                val bytes = Base64.decode(encoded, Base64.NO_WRAP)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    ?.asImageBitmap()
                            }.getOrNull()
                        }
                        thumb?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = pendingOnce,
                                    onCheckedChange = { pendingOnce = it },
                                )
                                Text(
                                    text = "View once 🔥",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                        IconButton(onClick = {
                            pendingPhoto = null
                            pendingOnce = false
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = {
                            val photo = encoded
                            val once = pendingOnce
                            pendingPhoto = null
                            pendingOnce = false
                            scope.launch { runCatching { repository.sendPhoto(photo, once) } }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send photo",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                ) {
                    if (recording) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "🔴 Recording… ${recordSeconds}s",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                            )
                            IconButton(onClick = {
                                recorder.cancel()
                                recording = false
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Cancel recording")
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = input,
                                onValueChange = {
                                    input = it
                                    val now = System.currentTimeMillis()
                                    if (it.isNotBlank() &&
                                        now - lastHeartbeat > TYPING_HEARTBEAT_MILLIS
                                    ) {
                                        lastHeartbeat = now
                                        scope.launch { runCatching { repository.setTyping() } }
                                    }
                                },
                                placeholder = { Text("Message") },
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                ),
                            )
                            IconButton(onClick = { launchCamera() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_camera),
                                    contentDescription = "Camera",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_gallery),
                                    contentDescription = "Gallery",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            when {
                                recording -> stopAndSendVoice()
                                input.isNotBlank() -> {
                                    val text = input
                                    input = ""
                                    scope.launch { repository.send(text) }
                                }
                                else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (recording || input.isNotBlank()) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_mic),
                            contentDescription = "Record a voice note",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }

    viewingPhoto?.let { photo ->
        FullscreenPhoto(
            message = photo,
            onDismiss = {
                viewingPhoto = null
                // View-once photos self-destruct after the partner sees them.
                if (photo.once && !photo.isMine(repository.myUid)) {
                    scope.launch { runCatching { repository.consumeOncePhoto(photo.id) } }
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: Message,
    mine: Boolean,
    showCaption: Boolean,
    showSeen: Boolean,
    reactionPickerOpen: Boolean,
    onLongPress: () -> Unit,
    onDismissPicker: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: () -> Unit,
    playingVoice: Boolean,
    onVoiceToggle: () -> Unit,
    onReact: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = if (mine) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (mine) 16.dp else 4.dp,
                            bottomEnd = if (mine) 4.dp else 16.dp,
                        ),
                    )
                    .combinedClickable(
                        onClick = {
                            when {
                                message.isPhoto && !message.once -> onPhotoClick()
                                message.isPhoto && message.once -> onPhotoClick()
                                message.isVoice -> onVoiceToggle()
                            }
                        },
                        onLongClick = onLongPress,
                    )
                    .padding(if (message.isPhoto && !message.once) 4.dp else 0.dp),
            ) {
                if (message.isVoice) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = if (playingVoice) {
                                painterResource(R.drawable.ic_pause)
                            } else {
                                rememberVectorPainter(Icons.Filled.PlayArrow)
                            },
                            contentDescription = if (playingVoice) "Pause" else "Play",
                            tint = if (mine) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = "  Voice note · ${message.durationSec ?: 0}s",
                            color = if (mine) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else if (message.isPhoto && message.once) {
                    Text(
                        text = if (message.onceConsumed) "🔥 Opened" else "🔥 One-time photo — tap to view",
                        color = if (mine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                } else if (message.isPhoto) {
                    PhotoBubble(message)
                } else {
                    Text(
                        text = message.body,
                        color = if (mine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = reactionPickerOpen,
                onDismissRequest = onDismissPicker,
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    REACTION_EMOJIS.forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .combinedClickable(onClick = { onReact(emoji) }),
                        )
                    }
                }
                if (mine) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = onDelete,
                    )
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
            ) {
                Text(
                    text = message.reactions.values.joinToString(" "),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        val caption = listOfNotNull(
            timeLabel(message.sentAt).takeIf { showCaption },
            "Seen ✓✓".takeIf { showSeen },
        ).joinToString(" · ")
        if (caption.isNotEmpty()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun FullscreenPhoto(message: Message, onDismiss: () -> Unit) {
    val bitmap = remember(message.id) {
        runCatching {
            val bytes = Base64.decode(message.body, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    } ?: return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun presenceLabel(lastActiveMillis: Long?): String? {
    if (lastActiveMillis == null) return null
    val minutes = (System.currentTimeMillis() - lastActiveMillis) / 60_000L
    return when {
        minutes < 2 -> "Active now"
        minutes < 60 -> "Active ${minutes}m ago"
        minutes < 60 * 24 -> "Active ${minutes / 60}h ago"
        else -> null
    }
}

@Composable
private fun DayChip(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun daysTogether(anniversaryMillis: Long): Long =
    (System.currentTimeMillis() - anniversaryMillis) / 86_400_000L + 1

private fun timeLabel(sentAt: Timestamp?): String? =
    sentAt?.toDate()?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) }

private fun sameDay(a: Timestamp?, b: Timestamp?): Boolean {
    if (a == null || b == null) return true
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    return fmt.format(a.toDate()) == fmt.format(b.toDate())
}

private fun dayLabel(sentAt: Timestamp?): String {
    val date = sentAt?.toDate() ?: return "Today"
    val now = System.currentTimeMillis()
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    return when (fmt.format(date)) {
        fmt.format(Date(now)) -> "Today"
        fmt.format(Date(now - 86_400_000L)) -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

@Composable
private fun PhotoBubble(message: Message) {
    val bitmap = remember(message.id) {
        runCatching {
            val bytes = Base64.decode(message.body, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Photo",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .widthIn(max = 272.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
    } else {
        Text(
            text = "Photo unavailable",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
