package com.tomaspinho.atvr.repository

import com.chaquo.python.PyObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.tomaspinho.atvr.data.CredentialStorage
import com.tomaspinho.atvr.domain.AppInfo
import com.tomaspinho.atvr.domain.MediaInfo
import com.tomaspinho.atvr.domain.PairingState
import com.tomaspinho.atvr.domain.PowerState
import com.tomaspinho.atvr.domain.PythonResult
import com.tomaspinho.atvr.domain.ScannedDevice
import com.tomaspinho.atvr.pyapi.PyApi

/**
 * The single repository that talks to pyatv via [PyApi]. There is no separate
 * UseCase layer -- pass-through UseCases were collapsed here. Only logic-bearing
 * operations (command debounce, power read-then-toggle) live as helpers below.
 *
 * Credential storage is delegated to [CredentialStorage], the single source of
 * truth; the Python `_stored_credentials` dict has been removed.
 */
class DeviceRepository(private val credentialStorage: CredentialStorage) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Media info pushed from pyatv, observed by RemoteControlViewModel. */
    private val _mediaInfoFlow = MutableSharedFlow<MediaInfo>(extraBufferCapacity = 16)
    val mediaInfoFlow: SharedFlow<MediaInfo> = _mediaInfoFlow.asSharedFlow()

    /** Power state pushed from pyatv, observed by RemoteControlViewModel. */
    private val _powerStateFlow = MutableSharedFlow<PowerState>(extraBufferCapacity = 8)
    val powerStateFlow: SharedFlow<PowerState> = _powerStateFlow.asSharedFlow()

    /** Keyboard focus state pushed from pyatv, observed by DeviceViewModel. */
    private val _keyboardFocusFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    val keyboardFocusFlow: SharedFlow<Boolean> = _keyboardFocusFlow.asSharedFlow()

    private var lastCommandAtMs: Long = 0L

    // --------------------------------------------------------------------- #
    // Scanning
    // --------------------------------------------------------------------- #

    suspend fun scanDevices(host: String? = null): Result<List<ScannedDevice>> {
        return when (val r = PyApi.scanDevices(host = host)) {
            is PythonResult.Ok -> {
                val devices = (r.payload["devices"] as? String)?.let { parseDevices(it) } ?: emptyList()
                Result.success(devices)
            }
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }
    }

    private fun parseDevices(raw: String): List<ScannedDevice> {
        val arr = json.parseToJsonElement(raw) as JsonArray
        return arr.map { el ->
            val o = el.jsonObject
            ScannedDevice(
                name = o["name"]?.jsonPrimitive?.contentOrNull ?: "Apple TV",
                address = o["address"]?.jsonPrimitive?.contentOrNull ?: "",
                identifier = o["identifier"]?.jsonPrimitive?.contentOrNull ?: "",
                model = o["model"]?.jsonPrimitive?.contentOrNull,
                services = (o["services"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    ?: emptyList()
            )
        }
    }

    // --------------------------------------------------------------------- #
    // Connection lifecycle
    // --------------------------------------------------------------------- #

    suspend fun connectToDevice(identifier: String): Result<Unit> {
        val creds = credentialStorage.snapshotCredentials(identifier)
        return when (val r = PyApi.connectToDevice(identifier, creds)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }
    }

    suspend fun disconnectFromDevice(identifier: String): Result<Unit> =
        when (val r = PyApi.disconnectDevice(identifier)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    // --------------------------------------------------------------------- #
    // Pairing
    // --------------------------------------------------------------------- #

    suspend fun startPairing(identifier: String, protocol: String = "mrp"): Result<String> =
        when (val r = PyApi.startPairing(identifier, protocol)) {
            is PythonResult.Ok -> Result.success(r.payload["session_key"] ?: "")
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    suspend fun finishPairingAndSave(sessionKey: String, pin: String): Result<Pair<String, String>> {
        val r = PyApi.finishPairing(sessionKey, pin)
        return when (r) {
            is PythonResult.Ok -> {
                val creds = r.payload["credentials"] ?: ""
                val proto = r.payload["protocol"] ?: "mrp"
                val deviceId = r.payload["device_id"] ?: sessionKey.substringBeforeLast('_')
                credentialStorage.saveCredentials(deviceId, proto, creds)
                Result.success(proto to creds)
            }
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }
    }

    suspend fun cancelPairing(sessionKey: String): Result<Unit> =
        when (val r = PyApi.cancelPairing(sessionKey)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    // --------------------------------------------------------------------- #
    // Remote commands (with 50ms debounce)
    // --------------------------------------------------------------------- #

    suspend fun sendRemoteCommand(identifier: String, command: String, action: String = "press"): Result<Unit> {
        val now = System.currentTimeMillis()
        val elapsed = now - lastCommandAtMs
        if (elapsed < DEBOUNCE_MS) delay(DEBOUNCE_MS - elapsed)
        lastCommandAtMs = System.currentTimeMillis()
        return when (val r = PyApi.sendRemoteCommand(identifier, command, action)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }
    }

    // --------------------------------------------------------------------- #
    // Media
    // --------------------------------------------------------------------- #

    suspend fun getNowPlaying(identifier: String): Result<MediaInfo?> {
        val r = PyApi.getPlayingInfo(identifier)
        return when (r) {
            is PythonResult.Ok -> Result.success(parseMediaInfo(r))
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }
    }

    private fun parseMediaInfo(r: PythonResult.Ok): MediaInfo? {
        // Python returns {device_state, media_type, position, total_time, metadata:{title,artist,album,app,artwork}}.
        val metadataRaw = r.payload["metadata"]
        val metadata: Map<String, String?> = if (metadataRaw != null) {
            runCatching {
                val o = json.parseToJsonElement(metadataRaw) as JsonObject
                o.toMap().mapValues { (_, v) -> (v as? JsonPrimitive)?.contentOrNull }
            }.getOrDefault(emptyMap())
        } else {
            // Fallback: flat shape (used by push callback parser).
            r.payload
        }
        val title = metadata["title"] ?: r.payload["title"]
        val artist = metadata["artist"] ?: r.payload["artist"]
        val album = metadata["album"] ?: r.payload["album"]
        val app = metadata["app"] ?: r.payload["app"]
        val artwork = metadata["artwork"] ?: r.payload["artwork"]
        val position = r.payload["position"]?.toLongOrNull()
        val totalTime = r.payload["total_time"]?.toLongOrNull()
        val deviceState = r.payload["device_state"]
        val mediaType = r.payload["media_type"]
        if (title == null && artist == null && artwork == null) return null
        return MediaInfo(title, artist, album, app, artwork, position, totalTime, deviceState, mediaType)
    }

    fun emitMediaInfo(info: MediaInfo) {
        _mediaInfoFlow.tryEmit(info)
    }

    // --------------------------------------------------------------------- #
    // Power (read + toggle)
    // --------------------------------------------------------------------- #

    suspend fun getPowerState(identifier: String): Result<PowerState> {
        val r = PyApi.getPowerState(identifier)
        return when (r) {
            is PythonResult.Ok -> Result.success(parsePowerState(r.payload["power_state"]))
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }
    }

    /** Read-then-toggle, inlined from the former TogglePowerUseCase. */
    suspend fun togglePower(identifier: String): Result<Unit> {
        val state = getPowerState(identifier).getOrNull() ?: PowerState.UNKNOWN
        val cmd = if (state == PowerState.ON) "turn_off" else "turn_on"
        return sendRemoteCommand(identifier, cmd, "press")
    }

    private fun parsePowerState(value: String?): PowerState = when (value?.lowercase()) {
        "on" -> PowerState.ON
        "off" -> PowerState.OFF
        "standby" -> PowerState.STANDBY
        else -> PowerState.UNKNOWN
    }

    fun emitPowerState(state: PowerState) {
        _powerStateFlow.tryEmit(state)
    }

    // --------------------------------------------------------------------- #
    // Apps
    // --------------------------------------------------------------------- #

    suspend fun getAppList(identifier: String): Result<List<AppInfo>> {
        val r = PyApi.getAppList(identifier)
        return when (r) {
            is PythonResult.Ok -> {
                val apps = (r.payload["apps"] as? String)?.let { parseAppList(it) } ?: emptyList()
                Result.success(apps)
            }
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }
    }

    private fun parseAppList(raw: String): List<AppInfo> {
        val arr = json.parseToJsonElement(raw) as JsonArray
        return arr.map { el ->
            val o = el.jsonObject
            AppInfo(
                name = o["name"]?.jsonPrimitive?.contentOrNull ?: "",
                bundleId = o["bundle_id"]?.jsonPrimitive?.contentOrNull ?: ""
            )
        }
    }

    suspend fun launchApp(identifier: String, bundleId: String): Result<Unit> =
        when (val r = PyApi.launchApp(identifier, bundleId)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    // --------------------------------------------------------------------- #
    // Keyboard
    // --------------------------------------------------------------------- #

    suspend fun startKeyboardListener(identifier: String, callback: PyObject): Result<Unit> =
        when (val r = PyApi.startKeyboardListener(identifier, callback)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    suspend fun sendKeyboardText(identifier: String, text: String): Result<Unit> =
        when (val r = PyApi.sendKeyboardText(identifier, text)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    suspend fun clearKeyboardText(identifier: String): Result<Unit> =
        when (val r = PyApi.clearKeyboardText(identifier)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    fun emitKeyboardFocus(focused: Boolean) {
        _keyboardFocusFlow.tryEmit(focused)
    }

    // --------------------------------------------------------------------- #
    // Push / power listeners registration
    // --------------------------------------------------------------------- #

    suspend fun registerPushCallback(identifier: String, callback: PyObject): Result<Unit> =
        when (val r = PyApi.setPushCallback(identifier, callback)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    suspend fun registerPowerCallback(identifier: String, callback: PyObject): Result<Unit> =
        when (val r = PyApi.setPowerCallback(identifier, callback)) {
            is PythonResult.Ok -> Result.success(Unit)
            is PythonResult.Err -> Result.failure(IllegalStateException(r.message))
        }

    companion object {
        private const val DEBOUNCE_MS = 50L
    }
}