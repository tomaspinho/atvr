package com.tomaspinho.atvr.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives the multi-protocol pairing state machine.
 *
 * Defaults to a single protocol (`mrp`) and only falls back to
 * `["airplay", "companion"]` if MRP pairing fails. It no longer queues all
 * three unconditionally.
 */
class PairingHandler {

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val queue = ArrayDeque<String>()
    private var currentIdentifier: String? = null
    private var currentName: String? = null

    /**
     * Enqueue protocols (default: MRP only) and start the first request.
     * Emits [PairingState.Pairing] for the consumer to surface a dialog.
     */
    fun startPairingFlow(identifier: String, name: String, protocols: List<String> = listOf("mrp")) {
        cancel()
        currentIdentifier = identifier
        currentName = name
        queue.clear()
        queue.addAll(protocols)
        processNext()
    }

    /**
     * Advance to the next queued protocol. Called after a protocol completes
     * (success or failure) or to kick off the first one.
     */
    fun processNext() {
        val id = currentIdentifier
        val name = currentName
        if (id == null || name == null) {
            _pairingState.value = PairingState.Idle
            return
        }
        val proto = queue.removeFirstOrNull()
        if (proto == null) {
            _pairingState.value = PairingState.FlowCompleted(id, name)
            return
        }
        _pairingState.value = PairingState.Pairing(name, proto)
    }

    /** Record that a pairing `begin()` succeeded and we're awaiting a PIN. */
    fun awaitingPin(sessionKey: String, protocol: String) {
        val name = currentName ?: return
        _pairingState.value = PairingState.WaitingForPin(name, protocol, sessionKey)
    }

    /** Record that the current protocol failed; try the next fallback. */
    fun failCurrentProtocol() {
        processNext()
    }

    /** Mark the whole flow completed successfully. */
    fun complete(identifier: String, name: String) {
        _pairingState.value = PairingState.FlowCompleted(identifier, name)
        queue.clear()
        currentIdentifier = null
        currentName = null
    }

    /** Cancel the in-flight pairing and reset to idle. */
    fun cancel() {
        _pairingState.value = PairingState.Idle
        queue.clear()
        currentIdentifier = null
        currentName = null
    }

    fun error(message: String) {
        _pairingState.value = PairingState.Error(message)
    }

    /** Protocols to try as fallbacks when the default (MRP) fails. */
    val fallbackProtocols: List<String> = listOf("airplay", "companion")
}