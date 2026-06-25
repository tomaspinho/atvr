package com.tomaspinho.atvr.domain

sealed interface PairingState {
    data object Idle : PairingState
    data class Pairing(val deviceName: String, val protocol: String) : PairingState
    data class WaitingForPin(val deviceName: String, val protocol: String, val sessionKey: String) : PairingState
    data class FlowCompleted(val identifier: String, val deviceName: String) : PairingState
    data class Error(val message: String) : PairingState
}