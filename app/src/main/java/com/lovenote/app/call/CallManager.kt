package com.lovenote.app.call

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * One-to-one WebRTC calls with Firestore as the signaling channel — no call
 * server needed. Media flows peer-to-peer (STUN) with a free TURN relay as
 * fallback for strict networks.
 */
object CallManager {

    sealed class State {
        object Idle : State()
        data class Outgoing(val video: Boolean) : State()
        data class Incoming(val video: Boolean) : State()
        data class Active(val video: Boolean) : State()
    }

    var state by mutableStateOf<State>(State.Idle)
        private set
    var localVideoTrack by mutableStateOf<VideoTrack?>(null)
        private set
    var remoteVideoTrack by mutableStateOf<VideoTrack?>(null)
        private set
    var muted by mutableStateOf(false)
        private set
    var speakerOn by mutableStateOf(false)
        private set
    var screenSharing by mutableStateOf(false)
        private set

    val eglBase: EglBase by lazy { EglBase.create() }

    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var audioTrack: AudioTrack? = null
    private var capturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var cameraVideoTrack: VideoTrack? = null
    private var screenCapturer: VideoCapturer? = null
    private var screenSource: VideoSource? = null
    private var screenHelper: SurfaceTextureHelper? = null
    private var pendingProjection: android.content.Intent? = null
    private var registrations = mutableListOf<ListenerRegistration>()
    private var coupleId: String? = null
    private var myUid: String? = null
    private var isCaller = false
    private var appContext: Context? = null

    private val db get() = FirebaseFirestore.getInstance()
    private fun callDoc() = db.collection("couples").document(coupleId!!)
        .collection("call").document("current")

    private fun ensureFactory(context: Context): PeerConnectionFactory {
        appContext = context.applicationContext
        factory?.let { return it }
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .createInitializationOptions(),
        )
        val f = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        factory = f
        return f
    }

    private fun iceServers() = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
            .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
    )

    /** Attach once per pairing so incoming calls ring while the app is open. */
    fun watch(context: Context, couple: String, uid: String) {
        coupleId = couple
        myUid = uid
        appContext = context.applicationContext
        registrations.forEach { it.remove() }
        registrations.clear()
        registrations += callDoc().addSnapshotListener { snap, _ ->
            val status = snap?.getString("status")
            val caller = snap?.getString("caller")
            val video = snap?.getBoolean("video") ?: false
            when {
                status == "ringing" && caller != uid && state is State.Idle ->
                    state = State.Incoming(video)
                status == "accepted" && caller == uid && state is State.Outgoing -> {
                    snap.getString("answerSdp")?.let { sdp ->
                        peer?.setRemoteDescription(
                            NoopSdpObserver,
                            SessionDescription(SessionDescription.Type.ANSWER, sdp),
                        )
                        state = State.Active(video)
                    }
                }
                (status == "ended" || status == "declined" || status == null) &&
                    state !is State.Idle -> cleanup()
            }
        }
    }

    fun startCall(context: Context, video: Boolean) {
        val uid = myUid ?: return
        isCaller = true
        state = State.Outgoing(video)
        setupMedia(context, video)
        createPeer(context, video, candidateField = "callerCandidates")
        peer?.createOffer(object : NoopSdpObserverBase() {
            override fun onCreateSuccess(desc: SessionDescription) {
                peer?.setLocalDescription(NoopSdpObserver, desc)
                callDoc().set(
                    mapOf(
                        "status" to "ringing",
                        "caller" to uid,
                        "video" to video,
                        "offerSdp" to desc.description,
                        "answerSdp" to null,
                        "startedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    ),
                )
                listenForRemoteCandidates("calleeCandidates")
            }
        }, MediaConstraints())
    }

    fun accept(context: Context) {
        val video = (state as? State.Incoming)?.video ?: false
        isCaller = false
        setupMedia(context, video)
        createPeer(context, video, candidateField = "calleeCandidates")
        callDoc().get().addOnSuccessListener { snap ->
            val offer = snap.getString("offerSdp") ?: return@addOnSuccessListener
            peer?.setRemoteDescription(object : NoopSdpObserverBase() {
                override fun onSetSuccess() {
                    peer?.createAnswer(object : NoopSdpObserverBase() {
                        override fun onCreateSuccess(desc: SessionDescription) {
                            peer?.setLocalDescription(NoopSdpObserver, desc)
                            callDoc().update(
                                mapOf("status" to "accepted", "answerSdp" to desc.description),
                            )
                            listenForRemoteCandidates("callerCandidates")
                            state = State.Active(video)
                        }
                    }, MediaConstraints())
                }
            }, SessionDescription(SessionDescription.Type.OFFER, offer))
        }
    }

    fun decline() {
        callDoc().update("status", "declined")
        cleanup()
    }

    fun hangup() {
        runCatching { callDoc().update("status", "ended") }
        cleanup()
    }

    fun toggleMute() {
        muted = !muted
        audioTrack?.setEnabled(!muted)
    }

    fun toggleSpeaker() {
        speakerOn = !speakerOn
        appContext?.getSystemService(AudioManager::class.java)?.isSpeakerphoneOn = speakerOn
    }

    fun switchCamera() {
        (capturer as? org.webrtc.CameraVideoCapturer)?.switchCamera(null)
    }

    private fun setupMedia(context: Context, video: Boolean) {
        val f = ensureFactory(context)
        audioTrack = f.createAudioTrack("audio0", f.createAudioSource(MediaConstraints()))
        muted = false
        context.getSystemService(AudioManager::class.java)?.mode =
            AudioManager.MODE_IN_COMMUNICATION
        speakerOn = video
        context.getSystemService(AudioManager::class.java)?.isSpeakerphoneOn = video
        if (video) {
            val enumerator = Camera2Enumerator(context)
            val front = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull() ?: return
            val cap = enumerator.createCapturer(front, null) ?: return
            capturer = cap
            surfaceHelper = SurfaceTextureHelper.create("capture", eglBase.eglBaseContext)
            videoSource = f.createVideoSource(cap.isScreencast)
            cap.initialize(surfaceHelper, context, videoSource!!.capturerObserver)
            cap.startCapture(960, 540, 24)
            cameraVideoTrack = f.createVideoTrack("video0", videoSource)
            localVideoTrack = cameraVideoTrack
        }
    }

    // --- Screen sharing (requires the mediaProjection foreground service) ---

    fun requestScreenShare(context: Context, projectionData: android.content.Intent) {
        pendingProjection = projectionData
        ScreenShareService.start(context)
    }

    /** Called by [ScreenShareService] once it is in the foreground. */
    fun onProjectionServiceReady(context: Context) {
        val data = pendingProjection ?: return
        pendingProjection = null
        val f = factory ?: return
        val metrics = context.resources.displayMetrics
        val cap = org.webrtc.ScreenCapturerAndroid(
            data,
            object : android.media.projection.MediaProjection.Callback() {},
        )
        screenHelper = SurfaceTextureHelper.create("screen", eglBase.eglBaseContext)
        screenSource = f.createVideoSource(true)
        cap.initialize(screenHelper, context, screenSource!!.capturerObserver)
        cap.startCapture(metrics.widthPixels / 2, metrics.heightPixels / 2, 15)
        screenCapturer = cap
        val track = f.createVideoTrack("screen0", screenSource)
        runCatching { capturer?.stopCapture() }
        videoSender()?.setTrack(track, false)
        localVideoTrack = track
        screenSharing = true
    }

    fun stopScreenShare(context: Context) {
        runCatching { screenCapturer?.stopCapture() }
        runCatching { screenCapturer?.dispose() }
        screenCapturer = null
        runCatching { screenHelper?.dispose() }
        screenHelper = null
        screenSource = null
        ScreenShareService.stop(context)
        runCatching { capturer?.startCapture(960, 540, 24) }
        cameraVideoTrack?.let { videoSender()?.setTrack(it, false) }
        localVideoTrack = cameraVideoTrack
        screenSharing = false
    }

    private fun videoSender() =
        peer?.senders?.firstOrNull { it.track()?.kind() == "video" }

    private fun createPeer(context: Context, video: Boolean, candidateField: String) {
        val f = ensureFactory(context)
        val config = PeerConnection.RTCConfiguration(iceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = f.createPeerConnection(config, object : PeerObserverBase() {
            override fun onIceCandidate(candidate: IceCandidate) {
                callDoc().collection(candidateField).add(
                    mapOf(
                        "candidate" to candidate.sdp,
                        "sdpMid" to candidate.sdpMid,
                        "sdpMLineIndex" to candidate.sdpMLineIndex,
                    ),
                )
            }

            override fun onAddStream(stream: MediaStream) {
                stream.videoTracks.firstOrNull()?.let { remoteVideoTrack = it }
            }

            override fun onTrack(transceiver: org.webrtc.RtpTransceiver) {
                (transceiver.receiver.track() as? VideoTrack)?.let { remoteVideoTrack = it }
            }
        })
        audioTrack?.let { peer?.addTrack(it, listOf("stream0")) }
        localVideoTrack?.let { peer?.addTrack(it, listOf("stream0")) }
    }

    private fun listenForRemoteCandidates(field: String) {
        registrations += callDoc().collection(field).addSnapshotListener { snap, _ ->
            snap?.documentChanges?.forEach { change ->
                if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                    val d = change.document
                    peer?.addIceCandidate(
                        IceCandidate(
                            d.getString("sdpMid"),
                            (d.getLong("sdpMLineIndex") ?: 0L).toInt(),
                            d.getString("candidate"),
                        ),
                    )
                }
            }
        }
    }

    private fun cleanup() {
        if (screenSharing) {
            runCatching { screenCapturer?.stopCapture() }
            runCatching { screenCapturer?.dispose() }
            screenCapturer = null
            runCatching { screenHelper?.dispose() }
            screenHelper = null
            screenSource = null
            appContext?.let { ScreenShareService.stop(it) }
            screenSharing = false
        }
        cameraVideoTrack = null
        runCatching { capturer?.stopCapture() }
        runCatching { capturer?.dispose() }
        capturer = null
        runCatching { surfaceHelper?.dispose() }
        surfaceHelper = null
        runCatching { peer?.close() }
        peer = null
        localVideoTrack = null
        remoteVideoTrack = null
        audioTrack = null
        videoSource = null
        appContext?.getSystemService(AudioManager::class.java)?.let {
            it.mode = AudioManager.MODE_NORMAL
            it.isSpeakerphoneOn = false
        }
        muted = false
        speakerOn = false
        state = State.Idle
        // keep the call-doc watcher (registrations[0]); drop candidate listeners
        while (registrations.size > 1) registrations.removeAt(registrations.lastIndex).remove()
    }
}

private object NoopSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

private abstract class NoopSdpObserverBase : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

private abstract class PeerObserverBase : PeerConnection.Observer {
    override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onRemoveStream(stream: MediaStream?) {}
    override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddStream(stream: MediaStream) {}
}
