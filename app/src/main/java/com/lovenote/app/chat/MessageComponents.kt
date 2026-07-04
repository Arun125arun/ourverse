package com.lovenote.app.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.lovenote.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val REACTION_EMOJIS = listOf("❤", "😂", "😍", "😢", "👍")

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageRow(
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
internal fun FullscreenPhoto(message: Message, onDismiss: () -> Unit) {
    var failed by remember(message.id) { mutableStateOf(false) }
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, message.id) {
        value = BubbleBitmaps.full(message.body)
        if (value == null) failed = true
    }
    // The dialog is always shown (even while decoding) so it can't get stuck.
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
            when {
                bitmap != null -> Image(
                    bitmap = bitmap!!,
                    contentDescription = "Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
                failed -> Text("Couldn't load this photo", color = Color.White)
                else -> CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

/** The quoted original shown at the top of a reply bubble. */
@Composable
internal fun QuoteBlock(label: String, text: String, mine: Boolean) {
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
internal fun MenuAction(
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
internal fun DayChip(label: String) {
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

internal fun timeLabel(sentAt: Timestamp?): String? =
    sentAt?.toDate()?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) }

internal fun sameDay(a: Timestamp?, b: Timestamp?): Boolean {
    if (a == null || b == null) return true
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    return fmt.format(a.toDate()) == fmt.format(b.toDate())
}

internal fun dayLabel(sentAt: Timestamp?): String {
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
internal fun PhotoBubble(message: Message) {
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
