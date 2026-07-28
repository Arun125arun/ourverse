package com.lovenote.app.us

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownSection(
    countdown: CountdownEvent?,
    countdownTick: Int,
    showPicker: Boolean,
    pickerTitle: String,
    onClearCountdown: () -> Unit,
    onShowPicker: () -> Unit,
    onDismissPicker: () -> Unit,
    onPickerTitleChange: (String) -> Unit,
    onStartCountdown: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.countdown_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {
            if (countdown != null) {
                onClearCountdown()
            } else {
                onShowPicker()
            }
        }) {
            if (countdown != null) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_description_clear_countdown))
            } else {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_description_set_countdown))
            }
        }
    }
    countdown?.let { cd ->
        countdownTick // read to trigger recomposition every minute
        val remainingMs = cd.targetMillis - System.currentTimeMillis()
        if (remainingMs > 0) {
            val days = remainingMs / (1000L * 60 * 60 * 24)
            val hours = (remainingMs % (1000L * 60 * 60 * 24)) / (1000L * 60 * 60)
            val minutes = (remainingMs % (1000L * 60 * 60)) / (1000L * 60)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = cd.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CountdownUnit(value = days, label = stringResource(R.string.countdown_unit_days), highlight = true)
                        CountdownUnit(value = hours, label = stringResource(R.string.countdown_unit_hrs), highlight = false)
                        CountdownUnit(value = minutes, label = stringResource(R.string.countdown_unit_min), highlight = false)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "\u2764\uFE0F",
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }

    if (showPicker) {
        CountdownPickerDialog(
            title = pickerTitle,
            onTitleChange = onPickerTitleChange,
            onDismiss = onDismissPicker,
            onStartCountdown = onStartCountdown,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownPickerDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onStartCountdown: (Long) -> Unit,
) {
    var cdPickDate by remember { mutableStateOf(false) }
    val cdDateState = rememberDatePickerState()
    val cdDateLabel = cdDateState.selectedDateMillis?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
    } ?: stringResource(R.string.pick_a_date)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_countdown_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.countdown_what_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { cdPickDate = true }) { Text(cdDateLabel) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    cdDateState.selectedDateMillis?.let { millis ->
                        onStartCountdown(millis)
                    }
                },
                enabled = title.isNotBlank() && cdDateState.selectedDateMillis != null,
            ) { Text(stringResource(R.string.start_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
    if (cdPickDate) {
        DatePickerDialog(
            onDismissRequest = { cdPickDate = false },
            confirmButton = {
                TextButton(onClick = { cdPickDate = false }) { Text(stringResource(R.string.ok_button)) }
            },
        ) {
            DatePicker(state = cdDateState)
        }
    }
}

@Composable
internal fun CountdownUnit(value: Long, label: String, highlight: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(
                if (highlight) Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                else Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            ),
    ) {
        Text(
            text = "%02d".format(value),
            style = if (highlight) MaterialTheme.typography.headlineLarge
            else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
        )
    }
}
