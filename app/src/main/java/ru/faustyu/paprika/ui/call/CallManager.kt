package ru.faustyu.paprika.ui.call

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*

object CallManager {
    private const val TAG = "CallManager"

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null

    var onIceCandidate: ((String) -> Unit)? = null
    var onConnectionStateChange: ((PeerConnection.PeerConnectionState) -> Unit)? = null

    fun init(context: Context) {
        if (factory != null) return
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    fun createPeerConnection(
        turnUrls: List<String> = emptyList(),
        turnUsername: String? = null,
        turnCredential: String? = null
    ) {
        val iceServers = mutableListOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        if (turnUrls.isNotEmpty() && turnUsername != null && turnCredential != null) {
            iceServers.add(
                PeerConnection.IceServer.builder(turnUrls)
                    .setUsername(turnUsername)
                    .setPassword(turnCredential)
                    .createIceServer()
            )
        } else if (turnUrls.isNotEmpty()) {
            // STUN-only entries (no credentials needed)
            turnUrls.forEach { url ->
                if (url.startsWith("stun:")) {
                    iceServers.add(PeerConnection.IceServer.builder(url).createIceServer())
                }
            }
        }
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        val f = factory ?: return
        peerConnection = f.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val json = JSONObject().apply {
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                    put("candidate", candidate.sdp)
                }.toString()
                onIceCandidate?.invoke(json)
            }

            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                Log.d(TAG, "Connection state: $state")
                onConnectionStateChange?.invoke(state)
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        })

        // Add local audio track
        val constraints = MediaConstraints()
        val audioSource = f.createAudioSource(constraints)
        localAudioTrack = f.createAudioTrack("local_audio_0", audioSource)
        peerConnection?.addTrack(localAudioTrack)
    }

    fun createOffer(onSuccess: (String) -> Unit, onFailure: (String) -> Unit = {}) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(noopSdpObserver(), sdp)
                onSuccess(sdp.description)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) { onFailure(error) }
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    fun setRemoteDescription(sdp: String, type: SessionDescription.Type, onSuccess: () -> Unit = {}) {
        val sessionDesc = SessionDescription(type, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() { onSuccess() }
            override fun onCreateSuccess(sdp: SessionDescription) {}
            override fun onCreateFailure(error: String) { Log.e(TAG, "setRemote create fail: $error") }
            override fun onSetFailure(error: String) { Log.e(TAG, "setRemote set fail: $error") }
        }, sessionDesc)
    }

    fun createAnswer(onSuccess: (String) -> Unit, onFailure: (String) -> Unit = {}) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(noopSdpObserver(), sdp)
                onSuccess(sdp.description)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) { onFailure(error) }
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    fun addIceCandidate(candidateJson: String) {
        try {
            val obj = JSONObject(candidateJson)
            val candidate = IceCandidate(
                obj.getString("sdpMid"),
                obj.getInt("sdpMLineIndex"),
                obj.getString("candidate")
            )
            peerConnection?.addIceCandidate(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "addIceCandidate error: $e")
        }
    }

    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun close() {
        onIceCandidate = null
        onConnectionStateChange = null
        localAudioTrack?.dispose()
        localAudioTrack = null
        peerConnection?.close()
        peerConnection = null
    }

    private fun noopSdpObserver() = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String) { Log.e(TAG, "SDP fail: $error") }
        override fun onSetFailure(error: String) { Log.e(TAG, "SDP set fail: $error") }
    }
}
