package com.lovenote.app.vibe

import com.lovenote.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateRitualSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, frequency: String, actionType: String, reminderTime: String, reminderDays: List<Int>, customPrompt: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("daily") }
    var actionType by remember { mutableStateOf("text") }
    var reminderTime by remember { mutableStateOf("09:00") }
    var customPrompt by remember { mutableStateOf("") }
    var reminderDays = remember { mutableStateListOf<Int>() }

    val actionTypes = listOf("text" to stringResource(R.string.ritual_text_action), "voice_note" to stringResource(R.string.ritual_voice_action), "photo" to stringResource(R.string.ritual_photo_action), "ping" to stringResource(R.string.ritual_ping_action), "mood" to stringResource(R.string.ritual_mood_action), "custom" to stringResource(R.string.ritual_custom_action))
    val dayLabels = listOf(stringResource(R.string.ritual_sun) to 0, stringResource(R.string.ritual_mon) to 1, stringResource(R.string.ritual_tue) to 2, stringResource(R.string.ritual_wed) to 3, stringResource(R.string.ritual_thu) to 4, stringResource(R.string.ritual_fri) to 5, stringResource(R.string.ritual_sat) to 6)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (step > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ritual_back_description))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Step ${step + 1} of 4",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            when (step) {
                0 -> {
                    Text(
                        text = stringResource(R.string.ritual_name_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.ritual_name_label)) },
                        placeholder = { Text(stringResource(R.string.ritual_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.ritual_description_label)) },
                        placeholder = { Text(stringResource(R.string.ritual_description_placeholder)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { step = 1 },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(stringResource(R.string.ritual_next_button)) }
                }

                1 -> {
                    Text(
                        text = stringResource(R.string.ritual_how_often_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("daily" to stringResource(R.string.ritual_daily), "weekly" to stringResource(R.string.ritual_weekly), "monthly" to stringResource(R.string.ritual_monthly)).forEachIndexed { idx, (value, label) ->
                            SegmentedButton(
                                selected = frequency == value,
                                onClick = { frequency = value },
                                shape = SegmentedButtonDefaults.itemShape(idx, 3),
                            ) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (frequency == "weekly") {
                        Text(
                            text = stringResource(R.string.ritual_which_days_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dayLabels.forEach { (label, idx) ->
                                val selected = idx in reminderDays
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) reminderDays.remove(idx) else reminderDays.add(idx)
                                    },
                                    label = { Text(label) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (frequency == "monthly") {
                        Text(
                            text = stringResource(R.string.ritual_which_day_month_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..28).forEach { day ->
                                val selected = day in reminderDays
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) reminderDays.remove(day) else reminderDays.add(day)
                                    },
                                    label = { Text(day.toString()) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text(stringResource(R.string.ritual_remind_at_label)) },
                        placeholder = { Text(stringResource(R.string.ritual_remind_at_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { step = 2 },
                        enabled = when (frequency) {
                            "daily" -> true
                            "weekly" -> reminderDays.isNotEmpty()
                            "monthly" -> reminderDays.isNotEmpty()
                            else -> true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(stringResource(R.string.ritual_next_button)) }
                }

                2 -> {
                    Text(
                        text = stringResource(R.string.ritual_action_type_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        actionTypes.forEach { (value, label) ->
                            val selected = actionType == value
                            FilterChip(
                                selected = selected,
                                onClick = { actionType = value },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                    if (actionType == "custom") {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customPrompt,
                            onValueChange = { customPrompt = it },
                            label = { Text(stringResource(R.string.ritual_prompt_label)) },
                            placeholder = { Text(stringResource(R.string.ritual_prompt_placeholder)) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { step = 3 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(stringResource(R.string.ritual_next_button)) }
                }

                3 -> {
                    Text(
                        text = stringResource(R.string.ritual_review_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                    ReviewRow(stringResource(R.string.ritual_review_name), name)
                    if (description.isNotBlank()) ReviewRow(stringResource(R.string.ritual_review_description), description)
                    ReviewRow(stringResource(R.string.ritual_review_frequency), frequency.replaceFirstChar { it.uppercase() })
                    val actionLabel = actionTypes.firstOrNull { it.first == actionType }?.second ?: actionType
                    ReviewRow(stringResource(R.string.ritual_review_action), actionLabel)
                    ReviewRow(stringResource(R.string.ritual_review_reminder), reminderTime)
                    if (frequency == "weekly" && reminderDays.isNotEmpty()) {
                        val daysText = reminderDays.sorted().map { day -> dayLabels.firstOrNull { it.second == day }?.first ?: day.toString() }.joinToString(", ")
                        ReviewRow(stringResource(R.string.ritual_review_days), daysText)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onCreate(name, description, frequency, actionType, reminderTime, reminderDays.toList(), customPrompt.ifBlank { null })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(stringResource(R.string.ritual_create_button)) }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
