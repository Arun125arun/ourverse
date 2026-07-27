package com.lovenote.app.chat

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import com.lovenote.app.R
import com.lovenote.app.call.CallManager
import com.lovenote.app.notify.AppVisibility
import com.lovenote.app.notify.Notifier
import com.lovenote.app.settings.AppSettings
import com.lovenote.app.ui.Avatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private const val TYPING_VISIBLE_MILLIS = 6_000L
private const val TYPING_HEARTBEAT_MILLIS = 2_000L
private const val MAX_MESSAGE_LENGTH = 1000

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    repository: ChatRepository,
    onSettingsClick: () -> Unit,
    onGameClick: (gameId: String, gameType: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // null = still loading (so the empty state doesn't flash on entry)
    val messagesOrNull by repository.messages().collectAsState(initial = null)
    val messages = messagesOrNull.orEmpty()
    val listState = rememberLazyListState()
    var olderMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }

    // Snap to the newest message whenever one is sent or received.
    LaunchedEffect(messages.firstOrNull()?.id) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    // Reset pagination cursor when the chat screen is freshly opened.
    LaunchedEffect(Unit) {
        repository.resetPagination()
        olderMessages = emptyList()
        hasMore = true
    }

    // Combined list: newest first (real-time) + older (paginated, appended at tail).
    val allMessages = messages + olderMessages.filter {
        it.id !in messages.map { m -> m.id }.toSet()
    }

    // Load older messages when the user scrolls to the oldest end (top in reversed layout).
    LaunchedEffect(listState) {
        snapshotFlow {
            // In reverseLayout=true: firstOrNull() is the top item (oldest visible).
            val topIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            topIndex >= totalItems - 3 && !loadingMore && hasMore && allMessages.isNotEmpty()
        }.collect { shouldLoad ->
            if (shouldLoad) {
                loadingMore = true
                val loaded = runCatching { repository.loadOlderMessages() }.getOrDefault(emptyList())
                if (loaded.isEmpty()) {
                    hasMore = false
                } else {
                    olderMessages = olderMessages + loaded
                    hasMore = repository.canLoadMore()
                }
                loadingMore = false
            }
        }
    }

    val partnerTypingAt by repository.partnerTypingAt().collectAsState(initial = null)
    // Re-evaluate "Active …m ago" periodically even when nothing recomposes.
    var presenceNow by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            presenceNow = System.currentTimeMillis()
        }
    }
    val anniversary by repository.anniversaryMillis().collectAsState(initial = null)
    val partner by repository.partnerProfile().collectAsState(initial = null)
    var viewingPhoto by remember { mutableStateOf<Message?>(null) }
    var input by remember { mutableStateOf("") }
    var reactingTo by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Message?>(null) }
    var replying by remember { mutableStateOf<Message?>(null) }
    var hiddenIds by remember { mutableStateOf(HiddenMessages.load(context)) }
    val visibleMessages = allMessages.filter { it.id !in hiddenIds }
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
        // Haptic feedback for recording completion
        Notifier.vibrate(context)
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
                // Haptic feedback for recording start
                Notifier.vibrate(context)
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
                                val subtitle = presenceLabel(partner?.lastActiveMillis, presenceNow)
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
                    var editNudge by remember { mutableStateOf(false) }
                    // tap = send the nudge, hold = customize its message
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = {
                                    scope.launch {
                                        runCatching { repository.send(AppSettings.nudgeText) }
                                        Notifier.vibrate(context)
                                    }
                                },
                                onLongClick = { editNudge = true },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Send a nudge (hold to customize)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.scale(heartPulse),
                        )
                    }
                    if (editNudge) {
                        var draft by remember { mutableStateOf(AppSettings.nudgeText) }
                        AlertDialog(
                            onDismissRequest = { editNudge = false },
                            title = { Text("Your heart button ❤") },
                            text = {
                                Column {
                                    Text(
                                        "This is what a tap on the heart sends:",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    TextField(
                                        value = draft,
                                        onValueChange = { draft = it.take(80) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    AppSettings.setNudge(context, draft)
                                    editNudge = false
                                }) { Text("Save") }
                            },
                            dismissButton = {
                                TextButton(onClick = { editNudge = false }) { Text("Cancel") }
                            },
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
            if (messagesOrNull == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (visibleMessages.isEmpty()) {
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
                        Text(
                            text = "\u2764",
                            fontSize = 42.sp,
                            modifier = Modifier
                                .scale(beat)
                                .semantics { contentDescription = "Heart" },
                        )
                        Spacer(Modifier.height(6.dp))
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
                                onGameClick = onGameClick,
                            )
                        }
                    }
                    // Loading indicator for older messages (at the oldest end of reversed list).
                    if (loadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
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
                                    text = "View once",
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
                                color = if (VoiceRecorder.MAX_SECONDS - recordSeconds <= 5) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                            )
                            IconButton(onClick = {
                                recorder.cancel()
                                recording = false
                                // Haptic feedback for recording cancellation
                                Notifier.vibrate(context)
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Cancel recording")
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = input,
                                        onValueChange = {
                                            // Truncate if exceeds max length
                                            if (it.length > MAX_MESSAGE_LENGTH) {
                                                input = it.substring(0, MAX_MESSAGE_LENGTH)
                                            } else {
                                                input = it
                                            }
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
                                if (input.isNotEmpty()) {
                                    Text(
                                        text = "${input.length}/$MAX_MESSAGE_LENGTH",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (input.length >= MAX_MESSAGE_LENGTH) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .size(52.dp)
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
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                    when {
                        recording -> {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Stop recording",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        input.isNotBlank() -> {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        else -> {
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

private fun presenceLabel(lastActiveMillis: Long?, now: Long): String? {
    if (lastActiveMillis == null) return null
    val minutes = (now - lastActiveMillis) / 60_000L
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

private fun daysTogether(anniversaryMillis: Long): Long =
    (System.currentTimeMillis() - anniversaryMillis) / 86_400_000L + 1
