package com.lovenote.app.us

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun QuickPingRow(
    onPingClick: () -> Unit,
    onSendPing: (PingType) -> Unit,
) {
    val quickPings = listOf(PingType.HEART, PingType.MISS, PingType.THINKING, PingType.STAR)
    var animPhase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { delay(100); animPhase = 1 }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\u26A1",
                fontSize = 18.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Quick ping",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.weight(1f))

            quickPings.forEachIndexed { index, type ->
                AnimatedVisibility(
                    visible = animPhase > 0,
                    enter = fadeIn(),
                ) {
                    var bounced by remember { mutableStateOf(false) }
                    val bounceScale by animateFloatAsState(
                        targetValue = if (bounced) 1.3f else 1f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
                        label = "pingBounce",
                    )
                    LaunchedEffect(bounced) {
                        if (bounced) { delay(250); bounced = false }
                    }
                    Text(
                        text = type.emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .scale(bounceScale)
                            .clickable {
                                bounced = true
                                onSendPing(type)
                            },
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.clickable(onClick = onPingClick),
            ) {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
