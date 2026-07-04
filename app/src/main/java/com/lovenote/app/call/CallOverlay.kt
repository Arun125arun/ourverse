package com.lovenote.app.call

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lovenote.app.notify.Notifier
import com.lovenote.app.ui.Avatar
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

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
                mirror = true,
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
                    Text(
                        text = when (state) {
                            is CallManager.State.Outgoing -> "Calling…"
                            is CallManager.State.Incoming ->
                                if (isVideo) "Incoming video call ❤" else "Incoming voice call ❤"
                            is CallManager.State.Active -> "Connected ❤"
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
                        RoundButton("📵", Color(0xFFD32F2F)) { CallManager.decline() }
                        RoundButton("📞", Color(0xFF2E7D32)) { CallManager.accept(context) }
                    }
                }
                else -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundButton(
                            if (CallManager.muted) "🔇" else "🎙",
                            Color(0x33FFFFFF),
                        ) { CallManager.toggleMute() }
                        RoundButton(
                            if (CallManager.speakerOn) "🔊" else "🔈",
                            Color(0x33FFFFFF),
                        ) { CallManager.toggleSpeaker() }
                        if (isVideo) {
                            RoundButton("🔄", Color(0x33FFFFFF)) { CallManager.switchCamera() }
                        }
                        RoundButton("📵", Color(0xFFD32F2F)) { CallManager.hangup() }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(label: String, background: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(background, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 26.sp)
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
