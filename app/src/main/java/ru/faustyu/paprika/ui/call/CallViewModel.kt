package ru.faustyu.paprika.ui.call

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import ru.faustyu.paprika.data.network.AppWebSocketManager
import ru.faustyu.paprika.data.network.NetworkModule
import ru.faustyu.paprika.data.network.TurnCredentials

sealed class CallState {
    object Idle : CallState()
    data class Incoming(
        val callId: Long,
        val callerId: Long,
        val callerName: String,
        val callType: String
    ) : CallState()
    data class Outgoing(
        val callId: Long,
        val calleeId: Long,
        val calleeName: String,
        val callType: String
    ) : CallState()
    data class Active(
        val callId: Long,
        val peerId: Long,
        val peerName: String = "",
        val callType: String,
        val startTime: Long = System.currentTimeMillis()
    ) : CallState()
    object Ended : CallState()
}

class CallViewModel(app: Application) : AndroidViewModel(app) {
    private val TAG = "CallVM"
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    var isMuted = MutableStateFlow(false)
        private set

    private var cachedTurnCredentials: TurnCredentials? = null

    init {
        CallManager.init(app)
        AppWebSocketManager.addListener("call_vm") { event ->
            handleWebSocketEvent(event)
        }
        // Pre-fetch TURN credentials
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.getTurnCredentials()
                if (res.isSuccessful) cachedTurnCredentials = res.body()
            } catch (_: Exception) {}
        }
    }

    // Caller: initiate call
    fun initiateCall(calleeId: Long, calleeName: String, callType: String = "audio") {
        viewModelScope.launch {
            try {
                val res = NetworkModule.api.initiateCall(
                    InitiateCallRequest(callee_id = calleeId, call_type = callType)
                )
                if (res.isSuccessful) {
                    val call = res.body() ?: return@launch
                    _callState.value = CallState.Outgoing(
                        callId = call.id,
                        calleeId = calleeId,
                        calleeName = calleeName,
                        callType = callType
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "initiateCall failed: $e")
            }
        }
    }

    // Callee: accept call
    fun acceptCall(callId: Long, callerId: Long) {
        viewModelScope.launch {
            try {
                NetworkModule.api.acceptCall(callId)
                // UI stays on IncomingCallScreen; we wait for call:offer via WebSocket
                // State will change to Active when offer/answer exchange completes
                val current = _callState.value as? CallState.Incoming ?: return@launch
                _callState.value = CallState.Active(
                    callId = callId,
                    peerId = callerId,
                    peerName = current.callerName,
                    callType = current.callType
                )
                // PeerConnection will be set up when we receive call:offer
                setupCallManagerCallbacks(callerId)
            } catch (e: Exception) {
                Log.e(TAG, "acceptCall failed: $e")
            }
        }
    }

    // Callee: reject call
    fun rejectCall(callId: Long) {
        viewModelScope.launch {
            try {
                NetworkModule.api.rejectCall(callId)
            } catch (e: Exception) {
                Log.e(TAG, "rejectCall failed: $e")
            } finally {
                _callState.value = CallState.Ended
            }
        }
    }

    // Either party: end call
    fun endCall() {
        val current = _callState.value
        val callId = when (current) {
            is CallState.Active -> current.callId
            is CallState.Outgoing -> current.callId
            else -> null
        }
        viewModelScope.launch {
            callId?.let {
                try {
                    NetworkModule.api.endCall(it)
                } catch (e: Exception) {
                    Log.e(TAG, "endCall failed: $e")
                }
            }
            CallManager.close()
            _callState.value = CallState.Ended
        }
    }

    fun toggleMute() {
        val muted = !isMuted.value
        isMuted.value = muted
        CallManager.setMuted(muted)
    }

    fun resetState() {
        _callState.value = CallState.Idle
    }

    private fun handleWebSocketEvent(event: Map<String, Any?>) {
        val type = event["event"] as? String ?: return
        Log.d(TAG, "WS event: $type")
        when (type) {
            "call:incoming" -> {
                if (_callState.value is CallState.Idle) {
                    val callId = (event["call_id"] as? Double)?.toLong() ?: return
                    val callerId = (event["caller_id"] as? Double)?.toLong() ?: return
                    val callType = event["call_type"] as? String ?: "audio"
                    val callerMap = event["caller"] as? Map<*, *>
                    val callerName = (callerMap?.get("username") as? String) ?: "Unknown"
                    _callState.value = CallState.Incoming(
                        callId = callId,
                        callerId = callerId,
                        callerName = callerName,
                        callType = callType
                    )
                }
            }

            "call:accepted" -> {
                // Caller receives this: start WebRTC signaling
                val current = _callState.value as? CallState.Outgoing ?: return
                val callId = (event["call_id"] as? Double)?.toLong() ?: return
                val calleeId = current.calleeId
                _callState.value = CallState.Active(
                    callId = callId,
                    peerId = calleeId,
                    peerName = current.calleeName,
                    callType = current.callType
                )
                // Caller creates offer
                setupCallManagerCallbacks(calleeId)
                CallManager.createPeerConnection(
                    turnUrls = cachedTurnCredentials?.urls ?: emptyList(),
                    turnUsername = cachedTurnCredentials?.username,
                    turnCredential = cachedTurnCredentials?.credential
                )
                CallManager.createOffer(onSuccess = { sdp ->
                    AppWebSocketManager.send(
                        mapOf(
                            "event" to "call:offer",
                            "target_user_id" to calleeId,
                            "sdp" to sdp
                        )
                    )
                })
            }

            "call:rejected" -> {
                CallManager.close()
                _callState.value = CallState.Ended
            }

            "call:ended" -> {
                CallManager.close()
                _callState.value = CallState.Ended
            }

            "call:offer" -> {
                // Callee receives this: create answer
                val sdp = event["sdp"] as? String ?: return
                val fromId = (event["from_user_id"] as? Double)?.toLong() ?: return
                CallManager.createPeerConnection(
                    turnUrls = cachedTurnCredentials?.urls ?: emptyList(),
                    turnUsername = cachedTurnCredentials?.username,
                    turnCredential = cachedTurnCredentials?.credential
                )
                setupCallManagerCallbacks(fromId)
                CallManager.setRemoteDescription(sdp, SessionDescription.Type.OFFER) {
                    CallManager.createAnswer(onSuccess = { answerSdp ->
                        AppWebSocketManager.send(
                            mapOf(
                                "event" to "call:answer",
                                "target_user_id" to fromId,
                                "sdp" to answerSdp
                            )
                        )
                    })
                }
            }

            "call:answer" -> {
                // Caller receives answer from callee
                val sdp = event["sdp"] as? String ?: return
                CallManager.setRemoteDescription(sdp, SessionDescription.Type.ANSWER)
            }

            "call:ice_candidate" -> {
                val candidate = event["candidate"] as? String ?: return
                CallManager.addIceCandidate(candidate)
            }
        }
    }

    private fun setupCallManagerCallbacks(peerId: Long) {
        CallManager.onIceCandidate = { candidateJson ->
            AppWebSocketManager.send(
                mapOf(
                    "event" to "call:ice_candidate",
                    "target_user_id" to peerId,
                    "candidate" to candidateJson
                )
            )
        }
        CallManager.onConnectionStateChange = { state ->
            when (state) {
                PeerConnection.PeerConnectionState.FAILED,
                PeerConnection.PeerConnectionState.CLOSED -> {
                    CallManager.close()
                    _callState.value = CallState.Ended
                }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppWebSocketManager.removeListener("call_vm")
        CallManager.close()
    }
}

data class InitiateCallRequest(
    val callee_id: Long,
    val call_type: String
)

data class CallResponse(
    val id: Long,
    val caller_id: Long,
    val callee_id: Long,
    val status: String,
    val call_type: String
)
