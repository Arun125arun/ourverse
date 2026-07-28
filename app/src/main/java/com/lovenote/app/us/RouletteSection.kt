package com.lovenote.app.us

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.R

@Composable
fun RouletteSection(
    rouletteState: Map<String, Any?>,
    myUid: String,
    me: Profile?,
    partner: Profile?,
    showAnswer: Boolean,
    question: String?,
    answer: String,
    onSpin: () -> Unit,
    onAnswerChange: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onDismissAnswer: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.question_roulette_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSpin) {
            Text("\uD83C\uDFB2", fontSize = 26.sp)
        }
    }
    val rouletteAnswers = (rouletteState["answers"] as? Map<*, *>).orEmpty()
    if (rouletteAnswers.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.last_answers_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(6.dp))
                rouletteAnswers.entries.forEach { (uid, value) ->
                    val entry = value as? Map<*, *> ?: return@forEach
                    val ans = entry["answer"] as? String ?: ""
                    val isMine = uid == myUid
                    val name = if (isMine) me?.name?.substringBefore(' ') ?: stringResource(R.string.sender_me) else partner?.name?.substringBefore(' ') ?: stringResource(R.string.sender_partner)
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = if (isMine) "\uD83D\uDC64" else "\uD83D\uDC65",
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                text = ans.take(100),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAnswer) {
        AlertDialog(
            onDismissRequest = onDismissAnswer,
            title = { Text(question ?: stringResource(R.string.roulette_question_fallback)) },
            text = {
                OutlinedTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    label = { Text(stringResource(R.string.roulette_your_answer)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = onSubmitAnswer) { Text(stringResource(R.string.send_button)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissAnswer) { Text(stringResource(R.string.skip_button)) }
            },
        )
    }
}
