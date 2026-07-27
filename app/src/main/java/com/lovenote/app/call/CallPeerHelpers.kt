package com.lovenote.app.call

import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

internal object NoopSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

internal abstract class NoopSdpObserverBase : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

internal abstract class PeerObserverBase : PeerConnection.Observer {
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
