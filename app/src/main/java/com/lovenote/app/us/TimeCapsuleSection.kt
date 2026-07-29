package com.lovenote.app.us

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TimeCapsuleSection(
    repository: UsRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val capsules by repository.timeCapsules().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }

    val sealed = capsules.filter { !it.opened }
    val opened = capsules.filter { it.opened }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.time_capsule_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.time_capsule_new), style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.time_capsule_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )

        if (sealed.isEmpty() && opened.isEmpty()) {
            Text(
                text = stringResource(R.string.time_capsule_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }

        // Sealed capsules
        sealed.forEach { capsule ->
            Spacer(Modifier.height(10.dp))
            SealedCapsuleCard(capsule, repository.myUid) {
                scope.launch { runCatching { repository.openTimeCapsule(capsule.id) } }
            }
        }

        // Opened capsules
        if (opened.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Opened",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            opened.forEach { capsule ->
                Spacer(Modifier.height(8.dp))
                OpenedCapsuleCard(capsule, repository.myUid)
            }
        }
    }

    if (showCreateDialog) {
        CreateCapsuleDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, message, unlockMillis ->
                showCreateDialog = false
                scope.launch {
                    runCatching { repository.sendTimeCapsule(title, message, null, unlockMillis) }
                }
            },
        )
    }
}

@Composable
private fun SealedCapsuleCard(capsule: TimeCapsule, myUid: String, onOpen: () -> Unit) {
    val isMine = capsule.senderUid == myUid
    val now = System.currentTimeMillis()
    val remaining = capsule.unlockAtMillis - now
    val unlocked = remaining <= 0

    val scale by animateFloatAsState(
        targetValue = if (unlocked) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "sealScale",
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (unlocked) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (unlocked) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = capsule.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (isMine) "You sealed this" else "Partner sealed this",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (unlocked) {
                Text(
                    text = stringResource(R.string.time_capsule_ready),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.time_capsule_reveal))
                }
            } else {
                val days = TimeUnit.MILLISECONDS.toDays(remaining)
                val hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24
                val timeText = buildString {
                    if (days > 0) append("${days}d ")
                    append("${hours}h left")
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun OpenedCapsuleCard(capsule: TimeCapsule, myUid: String) {
    val isMine = capsule.senderUid == myUid
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💌",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = capsule.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val openedDate = capsule.openedAtMillis?.let {
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it))
                    } ?: ""
                    Text(
                        text = "${if (isMine) "You sealed" else "Partner sealed"} · Opened $openedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Text(
                            text = capsule.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCapsuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, message: String, unlockAtMillis: Long) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(1) }
    val options = listOf(
        stringResource(R.string.time_capsule_1day) to 1L,
        stringResource(R.string.time_capsule_3days) to 3L,
        stringResource(R.string.time_capsule_1week) to 7L,
        stringResource(R.string.time_capsule_1month) to 30L,
        stringResource(R.string.time_capsule_1year) to 365L,
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.time_capsule_create_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.time_capsule_label_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.time_capsule_label_message)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.time_capsule_unlock_in),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEachIndexed { index, (label, days) ->
                        val selected = selectedDays == days.toInt()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedDays = days.toInt() },
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unlockMillis = System.currentTimeMillis() + selectedDays * 86_400_000L
                    onConfirm(title, message, unlockMillis)
                },
                enabled = title.isNotBlank() && message.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.time_capsule_seal))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.time_capsule_cancel))
            }
        },
    )
}
