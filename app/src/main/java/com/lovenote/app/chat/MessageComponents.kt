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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
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
    onGameClick: (gameId: String, gameType: String) -> Unit = { _, _ -> },
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
                    val voiceTextColor = if (mine) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = if (playingVoice) {
                                painterResource(R.drawable.ic_pause)
                            } else {
                                rememberVectorPainter(Icons.Filled.PlayArrow)
                            },
                            contentDescription = if (playingVoice) "Pause" else "Play",
                            tint = voiceTextColor,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val barSeeds = listOf(6f, 12f, 8f, 16f, 5f, 14f, 9f, 18f, 7f, 11f, 13f, 6f, 15f, 8f, 10f, 17f, 5f, 12f, 9f, 14f, 7f, 11f, 16f, 6f, 13f, 8f, 15f, 10f, 5f, 12f)
                                barSeeds.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(h.dp)
                                            .background(
                                                voiceTextColor.copy(alpha = 0.4f),
                                                RoundedCornerShape(1.dp),
                                            ),
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${message.durationSec ?: 0}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = voiceTextColor.copy(alpha = 0.6f),
                            )
                        }
                    }
                } else if (message.isPhoto && message.once) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (message.onceConsumed) R.drawable.ic_once_opened else R.drawable.ic_once,
                            ),
                            contentDescription = null,
                            tint = if (mine) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (message.onceConsumed) "Opened" else "View once — tap to view",
                            color = if (mine) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else if (message.isPhoto) {
                    PhotoBubble(message)
                } else if (message.isGameInvite) {
                    GameInviteBubble(
                        message = message,
                        mine = mine,
                        onClick = { onGameClick(message.gameId, message.gameType) },
                    )
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

            if (reactionPickerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        ) { onDismissPicker() }
                ) {
                    Column(
                        modifier = Modifier
                            .align(if (mine) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(horizontal = 8.dp)
                            .shadow(12.dp, RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            ) { }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            REACTION_EMOJIS.forEachIndexed { index, emoji ->
                                val pop = remember { Animatable(0f) }
                                LaunchedEffect(reactionPickerOpen) {
                                    if (reactionPickerOpen) {
                                        pop.snapTo(0f)
                                        delay(index * 50L)
                                        pop.animateTo(
                                            1f,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                    }
                                }
                                val selected = myReaction == emoji
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(pop.value)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent,
                                            CircleShape,
                                        )
                                        .clickable {
                                            onReact(emoji)
                                            onDismissPicker()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                        MenuAction(
                            painter = painterResource(R.drawable.ic_reply),
                            label = "Reply",
                            onClick = { onReply(); onDismissPicker() },
                        )
                        if (message.type == "text") {
                            MenuAction(
                                painter = painterResource(R.drawable.ic_copy),
                                label = "Copy",
                                onClick = { onCopy(); onDismissPicker() },
                            )
                        }
                        if (mine && message.type == "text") {
                            MenuAction(
                                painter = rememberVectorPainter(Icons.Filled.Edit),
                                label = "Edit",
                                onClick = { onEdit(); onDismissPicker() },
                            )
                        }
                        MenuAction(
                            painter = rememberVectorPainter(Icons.Filled.Delete),
                            label = if (mine) "Delete for everyone" else "Delete for me",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { onDelete(); onDismissPicker() },
                        )
                    }
                }
            }
        }
        }

        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val reactionCounts = remember(message.reactions) {
                    message.reactions.values.groupingBy { it }.eachCount()
                }

                REACTION_EMOJIS.forEach { emoji ->
                    val count = reactionCounts[emoji] ?: 0
                    if (count > 0) {
                        val userHasReacted = myReaction == emoji

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clickable { onReact(emoji) }
                                .semantics {
                                    if (userHasReacted) {
                                        contentDescription = "Remove $emoji reaction"
                                    } else {
                                        contentDescription = "Add $emoji reaction"
                                    }
                                }
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 18.sp
                            )
                            if (count > 1) {
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .background(
                                            shape = CircleShape,
                                            color = if (userHasReacted) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            }
                                        )
                                        .padding(horizontal = 2.dp, vertical = 0.dp)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }
        }

        val caption = listOfNotNull(
            "edited".takeIf { message.edited },
            timeLabel(message.sentAt).takeIf { showCaption },
            "Seen \u2713\u2713".takeIf { showSeen },
        ).joinToString(" \u00B7 ")
        if (caption.isNotEmpty()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .then(if (mine) Modifier.align(Alignment.End) else Modifier),
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
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
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

@Composable
internal fun GameInviteBubble(
    message: Message,
    mine: Boolean,
    onClick: () -> Unit,
) {
    val gameLabel = when (message.gameType) {
        "tictactoe" -> "Tic Tac Toe"
        "ludo" -> "Ludo"
        "truthdare" -> "Truth or Dare"
        "wordgame" -> "Word Game"
        else -> "Game"
    }
    val gameEmoji = when (message.gameType) {
        "tictactoe" -> "❌"
        "ludo" -> "🎲"
        "truthdare" -> "🔥"
        "wordgame" -> "🔤"
        else -> "🎮"
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = gameEmoji, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = gameLabel,
            style = MaterialTheme.typography.titleSmall,
            color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = message.body,
            style = MaterialTheme.typography.bodySmall,
            color = if (mine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (mine) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary,
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (mine) "View Game" else "Join Game",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
