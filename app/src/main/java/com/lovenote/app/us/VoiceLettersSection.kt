package com.lovenote.app.us

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.R
import com.lovenote.app.chat.VoicePlayer
import com.lovenote.app.chat.VoiceRecorder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceLettersSection(
    voiceLetters: List<VoiceLetter>,
    myUid: String,
    me: Profile?,
    partner: Profile?,
    recording: Boolean,
    recordSeconds: Int,
    playingVoiceId: String?,
    showCaptionDialog: Boolean,
    pendingAudio: Pair<String, Long>?,
    context: Context,
    onToggleRecording: () -> Unit,
    onPlayVoice: (VoiceLetter) -> Unit,
    onDismissCaption: () -> Unit,
    onConfirmCaption: suspend (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.voice_letters_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggleRecording) {
            if (recording) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.content_description_stop_recording_voice),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "${VoiceRecorder.MAX_SECONDS - recordSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.content_description_record_voice_letter),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    if (recording) {
        val infiniteTransition = rememberInfiniteTransition(label = "rec")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.8f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "pulse",
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(pulse)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.voice_letter_recording),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = "${recordSeconds}s / ${VoiceRecorder.MAX_SECONDS}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(5) { i ->
                        val barHeight by infiniteTransition.animateFloat(
                            initialValue = 6f,
                            targetValue = 18f + (i * 3),
                            animationSpec = infiniteRepeatable(
                                tween(300 + i * 100),
                                RepeatMode.Reverse,
                            ),
                            label = "bar$i",
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight.dp)
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (voiceLetters.isEmpty() && !recording) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("\uD83C\uDF99\uFE0F", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                Text(
                    text = stringResource(R.string.voice_letter_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    voiceLetters.forEach { letter ->
        val isMine = letter.senderUid == myUid
        val senderName = if (isMine) me?.name?.substringBefore(' ') ?: stringResource(R.string.sender_me) else partner?.name?.substringBefore(' ') ?: stringResource(R.string.sender_partner)
        val timeAgo = voiceLetterTimeAgo(letter.createdAtMillis)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onPlayVoice(letter) }, modifier = Modifier.size(36.dp)) {
                    Text(
                        text = if (playingVoiceId == letter.id) "\u23F8" else "\u25B6",
                        fontSize = 18.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (letter.caption.isNotBlank()) {
                        Text(
                            text = letter.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                    Text(
                        text = "${letter.durationSec}s \u00B7 $timeAgo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }

    if (showCaptionDialog && pendingAudio != null) {
        VoiceCaptionDialog(
            onDismiss = onDismissCaption,
            onConfirmCaption = onConfirmCaption,
        )
    }
}

@Composable
private fun VoiceCaptionDialog(
    onDismiss: () -> Unit,
    onConfirmCaption: suspend (String) -> Unit,
) {
    var captionText by remember { mutableStateOf("") }
    var sendingError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voice_caption_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text(stringResource(R.string.voice_caption_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                sendingError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                sendingError = null
                scope.launch {
                    val result = runCatching { onConfirmCaption(captionText) }
                    if (result.isFailure) {
                        sendingError = context.getString(R.string.voice_caption_send_error, result.exceptionOrNull()?.message ?: context.getString(R.string.voice_caption_unknown_error))
                    } else {
                        onDismiss()
                    }
                }
            }) { Text(stringResource(R.string.send_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

internal fun voiceLetterTimeAgo(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 172_800_000 -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}
