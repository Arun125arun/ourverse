package com.lovenote.app.call

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lovenote.app.R
import com.lovenote.app.notify.Notifier
import com.lovenote.app.ui.Avatar
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/** "3:07" under an hour, "1:03:07" beyond it. */
private fun durationLabel(elapsedMillis: Long): String {
    val total = (elapsedMillis / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** Fullscreen call UI drawn above everything while a call is in progress. */
@Composable
fun CallOverlay(partnerName: String, partnerPhoto: String) {
    val context = LocalContext.current
    val state = CallManager.state
    if (state is CallManager.State.Idle) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2151019)),
    ) {
        val isVideo = when (state) {
            is CallManager.State.Outgoing -> state.video
            is CallManager.State.Incoming -> state.video
            is CallManager.State.Active -> state.video
            else -> false
        }

        if (isVideo && CallManager.remoteVideoTrack != null) {
            VideoView(
                track = CallManager.remoteVideoTrack,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isVideo && CallManager.localVideoTrack != null) {
            VideoView(
                track = CallManager.localVideoTrack,
                mirror = CallManager.frontCamera && !CallManager.screenSharing,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                if (!isVideo || CallManager.remoteVideoTrack == null) {
                    Avatar(name = partnerName, photoUrl = partnerPhoto, size = 96.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(partnerName, color = Color.White, fontSize = 24.sp)
                    Spacer(Modifier.height(6.dp))
                    // Ticks once a second so the call duration counts up live.
                    var now by remember { mutableStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(state is CallManager.State.Active) {
                        while (true) {
                            now = System.currentTimeMillis()
                            delay(1_000)
                        }
                    }
                    Text(
                        text = when (state) {
                            is CallManager.State.Outgoing -> "Calling…"
                            is CallManager.State.Incoming ->
                                if (isVideo) "Incoming video call ❤" else "Incoming voice call ❤"
                            is CallManager.State.Active ->
                                CallManager.connectedAtMillis
                                    ?.let { "❤ ${durationLabel(now - it)}" }
                                    ?: "Connected ❤"
                            else -> ""
                        },
                        color = Color(0xFFB09AA8),
                        fontSize = 15.sp,
                    )
                }
            }

            when (state) {
                is CallManager.State.Incoming -> {
                    LaunchedEffect(Unit) { Notifier.vibrate(context) }
                    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        RoundButton(
                            painter = painterResource(R.drawable.ic_call_end),
                            background = Color(0xFFD32F2F),
                        ) { CallManager.decline() }
                        RoundButton(
                            icon = Icons.Filled.Call,
                            background = Color(0xFF2E7D32),
                        ) { CallManager.accept(context) }
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundButton(
                            painter = painterResource(
                                if (CallManager.muted) R.drawable.ic_mic_off else R.drawable.ic_mic,
                            ),
                            background = if (CallManager.muted) Color.White else Color(0x33FFFFFF),
                            tint = if (CallManager.muted) Color.Black else Color.White,
                        ) { CallManager.toggleMute() }
                        RoundButton(
                            painter = painterResource(
                                if (CallManager.speakerOn) {
                                    R.drawable.ic_volume_up
                                } else {
                                    R.drawable.ic_volume_mute
                                },
                            ),
                            background = if (CallManager.speakerOn) Color.White else Color(0x33FFFFFF),
                            tint = if (CallManager.speakerOn) Color.Black else Color.White,
                        ) { CallManager.toggleSpeaker() }
                        if (isVideo && !CallManager.screenSharing) {
                            RoundButton(
                                icon = Icons.Filled.Refresh,
                                background = Color(0x33FFFFFF),
                            ) { CallManager.switchCamera() }
                        }
                        if (isVideo) {
                            RoundButton(
                                painter = painterResource(R.drawable.ic_screen_share),
                                background = if (CallManager.screenSharing) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0x33FFFFFF)
                                },
                            ) {
                                if (CallManager.screenSharing) {
                                    CallManager.stopScreenShare(context)
                                } else {
                                    val manager = context.getSystemService(
                                        android.media.projection.MediaProjectionManager::class.java,
                                    )
                                    projectionLauncher.launch(manager.createScreenCaptureIntent())
                                }
                            }
                        }
                        RoundButton(
                            painter = painterResource(R.drawable.ic_call_end),
                            background = Color(0xFFD32F2F),
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
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(background, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            painter != null -> Icon(
                painter = painter,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
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
