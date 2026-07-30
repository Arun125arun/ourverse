package com.lovenote.app.us

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lovenote.app.R

@Composable
internal fun DailyQuestionCard(
    question: String,
    myAnswer: String?,
    partnerAnswer: String?,
    onSubmit: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.todays_question),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(14.dp))

            when {
                myAnswer == null -> {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(stringResource(R.string.your_answer_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onSubmit(draft) },
                        enabled = draft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.answer_button))
                    }
                    Text(
                        text = stringResource(R.string.partner_answer_unlocks),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                else -> {
                    AnswerBlock(label = stringResource(R.string.label_you), text = myAnswer)
                    Spacer(Modifier.height(10.dp))
                    if (partnerAnswer != null) {
                        AnswerBlock(label = stringResource(R.string.label_them_heart), text = partnerAnswer)
                    } else {
                        Text(
                            text = stringResource(R.string.waiting_for_answer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnswerBlock(label: String, text: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
internal fun QuizCard(
    question: QuizQuestion,
    partnerName: String,
    mine: QuizEntry?,
    theirs: QuizEntry?,
    onSubmit: (answer: Int, guess: Int) -> Unit,
) {
    var myPick by remember(question.prompt) { mutableStateOf<Int?>(null) }
    var myGuess by remember(question.prompt) { mutableStateOf<Int?>(null) }
    fun opt(index: Int): String = question.options.getOrNull(index) ?: "?"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.couple_quiz_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(question.prompt, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            when {
                mine == null -> {
                    Text(stringResource(R.string.your_pick), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    OptionGrid(question.options, myPick) { myPick = it }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.what_did_partner_pick, partnerName),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    OptionGrid(question.options, myGuess) { myGuess = it }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val pick = myPick ?: return@Button
                            val guess = myGuess ?: return@Button
                            onSubmit(pick, guess)
                        },
                        enabled = myPick != null && myGuess != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.lock_it_in))
                    }
                }
                theirs == null -> {
                    Text(
                        stringResource(R.string.quiz_picked_and_guessed, opt(mine.answer), opt(mine.guess)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.quiz_waiting_for_partner, partnerName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                else -> {
                    val iWasRight = mine.guess == theirs.answer
                    val theyWereRight = theirs.guess == mine.answer
                    Text(
                        stringResource(R.string.quiz_they_picked, opt(theirs.answer)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(if (iWasRight) R.string.quiz_guess_right else R.string.quiz_guess_wrong, opt(mine.guess)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(
                            if (theyWereRight) R.string.quiz_partner_guessed_right else R.string.quiz_partner_guessed_wrong,
                            partnerName, opt(theirs.guess),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OptionGrid(
    options: List<String>,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEachIndexed { rowIndex, rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowOptions.forEachIndexed { colIndex, option ->
                    val index = rowIndex * 2 + colIndex
                    val isSelected = selected == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(index) },
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
