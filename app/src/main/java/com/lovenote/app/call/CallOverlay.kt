package com.lovenote.app.call

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lovenote.app.R
import com.lovenote.app.notify.Notifier
import com.lovenote.app.ui.Avatar
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

private fun durationLabel(elapsedMillis: Long): String {
    val total = (elapsedMillis / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

@Composable
fun CallOverlay(partnerName: String, partnerPhoto: String) {
    val context = LocalContext.current
    val state = CallManager.state
    if (state is CallManager.State.Idle) return

    val isVideo = when (state) {
        is CallManager.State.Outgoing -> state.video
        is CallManager.State.Incoming -> state.video
        is CallManager.State.Active -> state.video
        else -> false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Video backgrounds ──
        if (isVideo) {
            // Remote video — full screen background
            if (CallManager.remoteVideoTrack != null) {
                VideoView(
                    track = CallManager.remoteVideoTrack,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // No remote video yet — dark gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E)),
                )
            }

            // Local video — WhatsApp-style small corner preview
            if (CallManager.localVideoTrack != null) {
                VideoView(
                    track = CallManager.localVideoTrack,
                    mirror = CallManager.frontCamera && !CallManager.screenSharing,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(width = 120.dp, height = 170.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .align(Alignment.TopEnd),
                )
            }
        } else {
            // Voice call — dark gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)),
                        ),
                    ),
            )
        }

        // ── Top gradient overlay for text readability ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x88000000), Color.Transparent),
                    ),
                )
                .align(Alignment.TopCenter),
        )

        // ── Bottom gradient overlay for controls ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xAA000000)),
                    ),
                )
                .align(Alignment.BottomCenter),
        )

        // ── Content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── Top: Partner info ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp),
            ) {
                if (!isVideo || CallManager.remoteVideoTrack == null) {
                    Avatar(name = partnerName, photoUrl = partnerPhoto, size = 96.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        partnerName,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Call status
                var now by remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(state is CallManager.State.Active) {
                    while (true) {
                        now = System.currentTimeMillis()
                        delay(1_000)
                    }
                }
                Text(
                    text = when (state) {
                        is CallManager.State.Outgoing -> "Calling..."
                        is CallManager.State.Incoming ->
                            if (isVideo) "Incoming video call" else "Incoming voice call"
                        is CallManager.State.Active ->
                            CallManager.connectedAtMillis
                                ?.let { durationLabel(now - it) }
                                ?: "Connected"
                        else -> ""
                    },
                    color = Color(0xFFB0B0B0),
                    fontSize = 15.sp,
                )
            }

            // ── Bottom: Call controls ──
            when (state) {
                is CallManager.State.Incoming -> {
                    LaunchedEffect(Unit) { Notifier.vibrate(context) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tap to answer",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                            RoundButton(
                                painter = painterResource(R.drawable.ic_call_end),
                                background = Color(0xFFD32F2F),
                                label = "Decline",
                            ) { CallManager.decline() }
                            RoundButton(
                                icon = Icons.Filled.Call,
                                background = Color(0xFF2E7D32),
                                label = "Accept",
                            ) { CallManager.accept(context) }
                        }
                    }
                }
                else -> {
                    val projectionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult(),
                    ) { result ->
                        val data = result.data
                        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
                            CallManager.requestScreenShare(context, data)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ControlButton(
                                painter = painterResource(if (CallManager.muted) R.drawable.ic_mic_off else R.drawable.ic_mic),
                                active = !CallManager.muted,
                                label = if (CallManager.muted) "Unmute" else "Mute",
                            ) { CallManager.toggleMute() }
                            ControlButton(
                                painter = painterResource(if (CallManager.speakerOn) R.drawable.ic_volume_up else R.drawable.ic_volume_mute),
                                active = CallManager.speakerOn,
                                label = "Speaker",
                            ) { CallManager.toggleSpeaker() }
                            if (isVideo) {
                                ControlButton(
                                    icon = Icons.Filled.Refresh,
                                    active = true,
                                    label = "Flip",
                                ) { CallManager.switchCamera() }
                                ControlButton(
                                    painter = painterResource(R.drawable.ic_screen_share),
                                    active = CallManager.screenSharing,
                                    label = "Share",
                                ) {
                                    if (CallManager.screenSharing) {
                                        CallManager.stopScreenShare(context)
                                    } else {
                                        val manager = context.getSystemService(android.media.projection.MediaProjectionManager::class.java)
                                        projectionLauncher.launch(manager.createScreenCaptureIntent())
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        // End call button
                        RoundButton(
                            painter = painterResource(R.drawable.ic_call_end),
                            background = Color(0xFFD32F2F),
                            size = 72.dp,
                            iconSize = 32.dp,
                            label = "End",
                        ) { CallManager.hangup() }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(
    background: Color,
    painter: Painter? = null,
    icon: ImageVector? = null,
    tint: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp,
    label: String? = null,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(4.dp, CircleShape)
                .background(background, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                painter != null -> Icon(painter = painter, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
                icon != null -> Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
            }
        }
        label?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ControlButton(
    painter: Painter? = null,
    icon: ImageVector? = null,
    active: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (active) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                painter != null -> Icon(
                    painter = painter,
                    contentDescription = label,
                    tint = if (active) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp),
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun VideoView(track: VideoTrack?, mirror: Boolean = false, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(CallManager.eglBase.eglBaseContext, null)
                setMirror(mirror)
                setEnableHardwareScaler(true)
                setZOrderMediaOverlay(false)
            }
        },
        update = { view ->
            view.setMirror(mirror)
            val previous = view.tag as? VideoTrack
            if (previous !== track) {
                previous?.removeSink(view)
                track?.addSink(view)
                view.tag = track
            }
        },
        onRelease = { view ->
            (view.tag as? VideoTrack)?.removeSink(view)
            view.release()
        },
    )
}
