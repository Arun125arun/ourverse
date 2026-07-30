package com.lovenote.app.us

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.R

private val MOOD_EMOJIS = listOf("🥰", "😊", "😐", "😔", "😤", "😴")

private val PULSE_WORDS = listOf(
    "cozy", "grateful", "excited", "tired", "stressed",
    "happy", "creative", "calm", "lonely", "loved",
    "anxious", "peaceful", "hungry", "productive", "silly",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodSection(
    myMood: Mood?,
    partnerMood: Mood?,
    onPick: (String, String?) -> Unit,
) {
    var showWords by remember { mutableStateOf(false) }
    var selectedEmoji by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.daily_pulse),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.how_are_you_feeling),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MOOD_EMOJIS.forEach { emoji ->
                    val selected = myMood?.emoji == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent,
                                CircleShape,
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape,
                            )
                            .clickable {
                                selectedEmoji = emoji
                                if (myMood?.statusWord == null) {
                                    showWords = true
                                } else {
                                    onPick(emoji, myMood.statusWord)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (myMood != null && myMood.statusWord != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.mood_status_you, myMood.emoji, myMood.statusWord),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else if (myMood != null) {
                Text(
                    text = stringResource(R.string.tap_word_to_add_status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                )
            }

            if (showWords || (myMood?.emoji != null && myMood.statusWord == null)) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PULSE_WORDS.forEach { word ->
                        val isSelected = myMood?.statusWord == word
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.clickable {
                                onPick(selectedEmoji ?: myMood?.emoji ?: "😊", word)
                                showWords = false
                            },
                        ) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val partnerText = partnerMood?.let { mood ->
                val word = mood.statusWord
                if (word != null) stringResource(R.string.mood_status_partner_with_word, mood.emoji, word)
                else stringResource(R.string.mood_status_partner_emoji, mood.emoji)
            } ?: stringResource(R.string.partner_not_checked_in)
            Text(
                text = partnerText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}
