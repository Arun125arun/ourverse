package com.lovenote.app.ping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovenote.app.us.Ping
import com.lovenote.app.us.PingType
import com.lovenote.app.us.UsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(
    repository: UsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pings by repository.pings().collectAsState(initial = emptyList())
    var animPhase by remember { mutableStateOf(0) }
    val pingTypes = PingType.entries

    LaunchedEffect(Unit) {
        delay(80)
        animPhase = 1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text = "Send a quick ping",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(12.dp))

            // Ping grid — 2 rows of 4
            val rows = pingTypes.chunked(4)
            rows.forEachIndexed { ri, row ->
                AnimatedVisibility(
                    visible = animPhase > ri,
                    enter = fadeIn() + slideInVertically { it / 2 },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEach { type ->
                            PingButton(type) {
                                scope.launch { runCatching { repository.sendPing(type) } }
                            }
                        }
                        repeat(4 - row.size) {
                            Spacer(Modifier.fillMaxWidth(0.25f))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Recent pings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))

            if (pings.isEmpty()) {
                Text(
                    text = "No pings yet — send one above!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(pings, key = { it.id }) { ping ->
                        PingHistoryRow(ping, repository.myUid) {
                            scope.launch { runCatching { repository.openPing(ping.id) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PingButton(type: PingType, onSend: () -> Unit) {
    var justSent by remember { mutableStateOf(false) }
    val bounceScale by animateFloatAsState(
        targetValue = if (justSent) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "bounce",
    )

    LaunchedEffect(justSent) {
        if (justSent) {
            delay(300)
            justSent = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.25f)
            .scale(bounceScale)
            .clickable {
                justSent = true
                onSend()
            }
            .padding(vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(type.emoji, fontSize = 26.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun PingHistoryRow(ping: Ping, myUid: String, onClick: () -> Unit) {
    val isMine = ping.senderUid == myUid
    val timeText = remember(ping.sentAtMillis) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(ping.sentAtMillis))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !ping.opened && !isMine, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (!ping.opened && !isMine) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(ping.type.emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ping.type.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (isMine) "You sent this" else "Partner sent this",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            if (!ping.opened && !isMine) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}
