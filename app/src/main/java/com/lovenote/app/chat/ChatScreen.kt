package com.lovenote.app.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
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
    var input by remember { mutableStateOf("") }
    var reactingTo by remember { mutableStateOf<String?>(null) }
    var lastHeartbeat by remember { mutableStateOf(0L) }

    // Read receipts: stamp partner messages as seen while the chat is open.
    LaunchedEffect(messages) {
        runCatching { repository.markPartnerMessagesSeen(messages) }
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

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { repository.sendPhoto(PhotoEncoder.encode(context, uri)) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OurVerse ❤")
                        val subtitle = when {
                            partnerTyping -> "typing…"
                            anniversary != null -> "Day ${daysTogether(anniversary!!)} together ❤"
                            else -> null
                        }
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                },
                actions = {
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (startsNewDay) DayChip(dayLabel(message.sentAt))
                            MessageRow(
                                message = message,
                                mine = message.isMine(repository.myUid),
                                showSeen = message.id == newestSeenMineId,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Send a photo",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        val now = System.currentTimeMillis()
                        if (it.isNotBlank() && now - lastHeartbeat > TYPING_HEARTBEAT_MILLIS) {
                            lastHeartbeat = now
                            scope.launch { runCatching { repository.setTyping() } }
                        }
                    },
                    placeholder = { Text("Message…") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                )
                IconButton(
                    onClick = {
                        val text = input
                        input = ""
                        scope.launch { repository.send(text) }
                    },
                    enabled = input.isNotBlank(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: Message,
    mine: Boolean,
    showSeen: Boolean,
    reactionPickerOpen: Boolean,
    onLongPress: () -> Unit,
    onDismissPicker: () -> Unit,
    onDelete: () -> Unit,
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
                        onClick = {},
                        onLongClick = onLongPress,
                    )
                    .padding(if (message.isPhoto) 4.dp else 0.dp),
            ) {
                if (message.isPhoto) {
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
            timeLabel(message.sentAt),
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
