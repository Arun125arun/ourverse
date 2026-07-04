package com.lovenote.app.chat

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.File
import com.google.firebase.Timestamp
import com.lovenote.app.R
import com.lovenote.app.call.CallManager
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
    onSettingsClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by repository.messages().collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    // Snap to the newest message whenever one is sent or received.
    LaunchedEffect(messages.firstOrNull()?.id) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    val partnerTypingAt by repository.partnerTypingAt().collectAsState(initial = null)
    val anniversary by repository.anniversaryMillis().collectAsState(initial = null)
    val partner by repository.partnerProfile().collectAsState(initial = null)
    var viewingPhoto by remember { mutableStateOf<Message?>(null) }
    var input by remember { mutableStateOf("") }
    var reactingTo by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Message?>(null) }
    var replying by remember { mutableStateOf<Message?>(null) }
    var hiddenIds by remember { mutableStateOf(HiddenMessages.load(context)) }
    val visibleMessages = remember(messages, hiddenIds) {
        messages.filter { it.id !in hiddenIds }
    }
    val clipboard = LocalClipboardManager.current
    var lastHeartbeat by remember { mutableStateOf(0L) }

    // Read receipts: stamp partner messages as seen while the chat is open.
    LaunchedEffect(messages) {
        runCatching { repository.markPartnerMessagesSeen(messages) }
    }

    DisposableEffect(Unit) {
        AppVisibility.chatVisible = true
        onDispose {
            AppVisibility.chatVisible = false
            VoicePlayer.stop()
        }
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
                expandedHeight = 52.dp,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            name = partner?.name ?: "",
                            photoUrl = partner?.photoUrl ?: "",
                            size = 32.dp,
                        )
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = partner?.name?.takeIf { it.isNotBlank() } ?: "OurVerse ❤",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (partnerTyping) {
                                TypingDots()
                            } else {
                                val subtitle = presenceLabel(partner?.lastActiveMillis)
                                    ?: anniversary?.let { "Day ${daysTogether(it)} together ❤" }
                                subtitle?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (it == "Active now") {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    var pendingCallVideo by remember { mutableStateOf<Boolean?>(null) }
                    val callPermissions = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions(),
                    ) { grants ->
                        val video = pendingCallVideo
                        pendingCallVideo = null
                        if (video != null && grants.values.all { it }) {
                            CallManager.startCall(context, video)
                        }
                    }

                    fun requestCall(video: Boolean) {
                        pendingCallVideo = video
                        callPermissions.launch(
                            if (video) {
                                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                            } else {
                                arrayOf(Manifest.permission.RECORD_AUDIO)
                            },
                        )
                    }

                    IconButton(onClick = { requestCall(false) }) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = "Voice call",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { requestCall(true) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_videocam),
                            contentDescription = "Video call",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val heartPulse by rememberInfiniteTransition(label = "nudge")
                        .animateFloat(
                            initialValue = 1f,
                            targetValue = 1.18f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(750),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "nudgeScale",
                        )
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
                            modifier = Modifier.scale(heartPulse),
                        )
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
            if (visibleMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                        Text("❤", fontSize = 42.sp, modifier = Modifier.scale(beat))
                        Spacer(Modifier.padding(6.dp))
                        Text(
                            text = "Say hi to your partner ❤",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            } else {
                val newestSeenMineId = visibleMessages
                    .firstOrNull { it.isMine(repository.myUid) && it.seen }
                    ?.id
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(visibleMessages, key = { _, m -> m.id }) { index, message ->
                        // reverseLayout: chip rendered above the bubble marks a new day
                        val startsNewDay = index == visibleMessages.lastIndex ||
                            !sameDay(message.sentAt, visibleMessages[index + 1].sentAt)
                        // reversed list: index-1 is the *next* (newer) message
                        val lastOfRun = index == 0 ||
                            visibleMessages[index - 1].senderUid != message.senderUid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                        ) {
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
                                myReaction = message.reactions[repository.myUid],
                                quoteLabel = message.replySender?.let { sender ->
                                    if (sender == repository.myUid) "You"
                                    else partner?.name?.substringBefore(' ') ?: "Them"
                                },
                                onReply = {
                                    reactingTo = null
                                    editing = null
                                    replying = message
                                },
                                onDelete = {
                                    reactingTo = null
                                    if (message.isMine(repository.myUid)) {
                                        scope.launch { runCatching { repository.delete(message.id) } }
                                    } else {
                                        hiddenIds = HiddenMessages.hide(context, message.id)
                                    }
                                },
                                onEdit = {
                                    reactingTo = null
                                    editing = message
                                    input = message.body
                                },
                                onCopy = {
                                    reactingTo = null
                                    clipboard.setText(AnnotatedString(message.body))
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

            // Reply banner
            replying?.let { target ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_reply),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = if (target.isMine(repository.myUid)) "You"
                                else partner?.name?.substringBefore(' ') ?: "Them",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = Message.preview(target),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                        }
                        IconButton(onClick = { replying = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel reply")
                        }
                    }
                }
            }

            // Editing banner
            editing?.let { target ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Text(
                            text = "✏ Editing: ${target.body.take(40)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            editing = null
                            input = ""
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel edit")
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
                                    val editTarget = editing
                                    val replyTarget = replying
                                    input = ""
                                    editing = null
                                    replying = null
                                    scope.launch {
                                        if (editTarget != null) {
                                            runCatching { repository.edit(editTarget.id, text) }
                                        } else {
                                            repository.send(text, replyTo = replyTarget)
                                        }
                                    }
                                }
                                else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(
                        targetState = recording || input.isNotBlank(),
                        label = "micSend",
                    ) { showSend ->
                        if (showSend) {
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
    myReaction: String?,
    quoteLabel: String?,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onPhotoClick: () -> Unit,
    playingVoice: Boolean,
    onVoiceToggle: () -> Unit,
    onReact: (String) -> Unit,
) {
    val dragScope = rememberCoroutineScope()
    val swipeOffset = remember { Animatable(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset.value > 130f) onReply()
                        dragScope.launch {
                            swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onDragCancel = {
                        dragScope.launch { swipeOffset.animateTo(0f) }
                    },
                ) { change, dragAmount ->
                    val next = (swipeOffset.value + dragAmount).coerceIn(0f, 200f)
                    dragScope.launch { swipeOffset.snapTo(next) }
                    if (dragAmount != 0f) change.consume()
                }
            },
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Reply hint revealed while swiping
            Icon(
                painter = painterResource(R.drawable.ic_reply),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(
                    alpha = (swipeOffset.value / 130f).coerceIn(0f, 1f),
                ),
                modifier = Modifier
                    .align(if (mine) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 6.dp)
                    .size(22.dp),
            )
        Box(
            modifier = Modifier
                .align(if (mine) Alignment.CenterEnd else Alignment.CenterStart)
                .offset { IntOffset(swipeOffset.value.toInt(), 0) },
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
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
                Column {
                if (message.replyToId != null) {
                    QuoteBlock(
                        label = quoteLabel ?: "",
                        text = message.replyText ?: "",
                        mine = mine,
                    )
                }
                if (message.isVoice) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                            style = MaterialTheme.typography.bodyMedium,
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
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                }
            }

            DropdownMenu(
                expanded = reactionPickerOpen,
                onDismissRequest = onDismissPicker,
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    REACTION_EMOJIS.forEachIndexed { index, emoji ->
                        val pop = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(index * 45L)
                            pop.animateTo(
                                1f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                        val selected = message.reactions.values.isNotEmpty() &&
                            myReaction == emoji
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .scale(pop.value)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    CircleShape,
                                )
                                .clickable { onReact(emoji) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                MenuAction(
                    painter = painterResource(R.drawable.ic_reply),
                    label = "Reply",
                    onClick = onReply,
                )
                if (message.type == "text") {
                    MenuAction(
                        painter = painterResource(R.drawable.ic_copy),
                        label = "Copy",
                        onClick = onCopy,
                    )
                }
                if (mine && message.type == "text") {
                    MenuAction(
                        painter = rememberVectorPainter(Icons.Filled.Edit),
                        label = "Edit",
                        onClick = onEdit,
                    )
                }
                MenuAction(
                    painter = rememberVectorPainter(Icons.Filled.Delete),
                    label = if (mine) "Delete for everyone" else "Delete for me",
                    tint = MaterialTheme.colorScheme.error,
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
            "edited".takeIf { message.edited },
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
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, message.id) {
        value = BubbleBitmaps.full(message.body)
    }
    val loaded = bitmap ?: return
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
                bitmap = loaded,
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

/** "typing" with three softly bouncing dots. */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "typing",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 160),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Text(
                text = ".",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            )
        }
    }
}

/** The quoted original shown at the top of a reply bubble. */
@Composable
private fun QuoteBlock(label: String, text: String, mine: Boolean) {
    val barColor = if (mine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .padding(start = 6.dp, end = 6.dp, top = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1F000000)),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(38.dp)
                .background(barColor),
        )
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = if (mine) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                },
            )
        }
    }
}

@Composable
private fun MenuAction(
    painter: Painter,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tint)
        Spacer(Modifier.width(24.dp))
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
    val bitmap by produceState(initialValue = BubbleBitmaps.peek(message.id), message.id) {
        if (value == null) value = BubbleBitmaps.bubble(message.id, message.body)
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Photo",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .widthIn(max = 252.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
    } ?: Box(
        modifier = Modifier
            .size(width = 220.dp, height = 160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
