package com.tomaspinho.atvr.domain

import kotlinx.serialization.Serializable

@Serializable
data class ScannedDevice(
    val name: String,
    val address: String,
    val identifier: String,
    val model: String? = null,
    val services: List<String> = emptyList()
)

@Serializable
data class AppInfo(
    val name: String,
    val bundleId: String,
    val iconUrl: String? = null
)

@Serializable
data class MediaInfo(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val app: String? = null,
    val artwork: String? = null,
    val position: Long? = null,
    val totalTime: Long? = null,
    val deviceState: String? = null,
    val mediaType: String? = null
)

enum class PowerState { ON, OFF, STANDBY, UNKNOWN }

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED
}

data class ConnectedDevice(val id: String, val name: String)

enum class VolumeDirection { UP, DOWN }