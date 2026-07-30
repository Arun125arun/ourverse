package com.lovenote.app.us

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.R
import com.lovenote.app.ui.Avatar

private val MILESTONES =
    listOf(100L, 200L, 300L, 365L, 500L, 730L, 1000L, 1461L, 1825L, 2000L, 3650L)

@Composable
internal fun CoupleHero(
    me: Profile?,
    partner: Profile?,
    anniversaryMillis: Long?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(name = me?.name ?: "", photoUrl = me?.photoUrl ?: "", size = 64.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = me?.name?.substringBefore(' ') ?: "",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = "\u2764",
                fontSize = 26.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(name = partner?.name ?: "", photoUrl = partner?.photoUrl ?: "", size = 64.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = partner?.name?.substringBefore(' ') ?: "",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        anniversaryMillis?.let {
            Spacer(Modifier.height(12.dp))
            val days = (System.currentTimeMillis() - it) / 86_400_000L + 1
            Text(
                text = stringResource(R.string.days_together, days),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            MILESTONES.firstOrNull { milestone -> milestone > days }?.let { next ->
                val remaining = next - days
                Text(
                    text = if (remaining == 0L) {
                        stringResource(R.string.milestone_today, next)
                    } else {
                        stringResource(
                            R.string.milestone_upcoming, next, remaining,
                            stringResource(if (remaining == 1L) R.string.milestone_day else R.string.milestone_days),
                        )
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
