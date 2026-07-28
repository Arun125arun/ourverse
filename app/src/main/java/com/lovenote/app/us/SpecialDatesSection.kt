package com.lovenote.app.us

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

@Composable
fun SpecialDatesSection(
    events: List<CoupleEvent>,
    showAddEvent: Boolean,
    onAddClick: () -> Unit,
    onDeleteEvent: (String) -> Unit,
    onDismissAddEvent: () -> Unit,
    onConfirmAddEvent: (String, Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.special_dates_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAddClick) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_description_add_special_date))
        }
    }
    if (events.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\u2764\uFE0F",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.no_dates_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.no_dates_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    } else {
        events.forEach { event ->
            EventRow(
                event = event,
                onDelete = { onDeleteEvent(event.id) },
            )
        }
    }

    if (showAddEvent) {
        AddEventDialog(
            onDismiss = onDismissAddEvent,
            onAdd = onConfirmAddEvent,
        )
    }
}

@Composable
private fun EventRow(event: CoupleEvent, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(Date(event.dateMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = countdownLabel(event.dateMillis),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.content_description_delete),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

private fun countdownLabel(dateMillis: Long): String {
    val days = ((dateMillis - System.currentTimeMillis()) / 86_400_000.0)
    return when {
        days in -1.0..0.0 -> "today ❤"
        days > 0 -> "in ${days.toInt() + 1} days"
        else -> "${-days.toInt()} days ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()
    val dateLabel = dateState.selectedDateMillis?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
    } ?: stringResource(R.string.pick_a_date)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_special_date_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.special_date_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { pickDate = true }) { Text(dateLabel) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dateState.selectedDateMillis?.let { onAdd(title, it) }
                },
                enabled = title.isNotBlank() && dateState.selectedDateMillis != null,
            ) { Text(stringResource(R.string.add_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (pickDate) {
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.ok_button)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    }
}
