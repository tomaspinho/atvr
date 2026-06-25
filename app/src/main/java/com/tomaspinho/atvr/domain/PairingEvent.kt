package com.tomaspinho.atvr.domain

sealed interface PairingEvent {
    data class Request(val identifier: String, val deviceName: String, val protocol: String) : PairingEvent
    data class SubmitPin(val sessionKey: String, val pin: String) : PairingEvent
    data class Cancel(val sessionKey: String) : PairingEvent
}