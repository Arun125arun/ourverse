package com.lovenote.app.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

internal fun presenceLabel(lastActiveMillis: Long?, now: Long): String? {
    if (lastActiveMillis == null) return null
    val minutes = (now - lastActiveMillis) / 60_000L
    return when {
        minutes < 2 -> "Active now"
        minutes < 60 -> "Active ${minutes}m ago"
        minutes < 60 * 24 -> "Active ${minutes / 60}h ago"
        else -> null
    }
}

@Composable
internal fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "typing",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 160),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Text(
                text = ".",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            )
        }
    }
}

internal fun daysTogether(anniversaryMillis: Long): Long =
    (System.currentTimeMillis() - anniversaryMillis) / 86_400_000L + 1
