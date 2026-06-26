package com.tomaspinho.atvr.viewmodel.device

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tomaspinho.atvr.data.CredentialStorage
import com.tomaspinho.atvr.domain.AppInfo
import com.tomaspinho.atvr.domain.ConnectionState
import com.tomaspinho.atvr.domain.PairingHandler
import com.tomaspinho.atvr.domain.PairingState
import com.tomaspinho.atvr.domain.ScannedDevice
import com.tomaspinho.atvr.pyapi.PyCallbacks
import com.tomaspinho.atvr.repository.DeviceRepository
import com.tomaspinho.atvr.repository.SettingsRepository

sealed interface DeviceIntent {
    data object LoadKnownDevices : DeviceIntent
    data object ScanDevices : DeviceIntent
    data class ScanByIp(val ip: String) : DeviceIntent
    data class ConnectToDevice(val identifier: String, val name: String) : DeviceIntent
    data object DisconnectDevice : DeviceIntent
    data class StartPairing(val device: ScannedDevice, val protocol: String = "auto") : DeviceIntent
    data class SubmitPairingPin(val pin: String) : DeviceIntent
    data object CancelPairing : DeviceIntent
    data class ClearCredentials(val identifier: String, val name: String) : DeviceIntent
    data object ClearAllCredentials : DeviceIntent
    data class ToggleShowUnknownDevices(val value: Boolean) : DeviceIntent
    data class ToggleVolumeCapture(val value: Boolean) : DeviceIntent
    data object LoadApps : DeviceIntent
    data class LaunchApp(val bundleId: String, val name: String) : DeviceIntent
    data object DismissAppSheet : DeviceIntent
    data object ToggleKeyboardVisibility : DeviceIntent
    data class UpdateKeyboardInput(val text: String) : DeviceIntent
    data class SubmitKeyboardText(val text: String) : DeviceIntent
    data object DismissKeyboard : DeviceIntent
    data class LaunchDeepLink(val url: String) : DeviceIntent
}

data class DeviceUiState(
    val discoveredDevices: List<ScannedDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val connectedDeviceId: String? = null,
    val connectedDeviceName: String? = null,
    val pairingState: PairingState = PairingState.Idle,
    val showUnknownDevices: Boolean = false,
    val volumeCaptureEnabled: Boolean = false,
    val showMediaPlayer: Boolean = true,
    val mediaNotificationEnabled: Boolean = false,
    val touchpadAtBottom: Boolean = false,
    val availableApps: List<AppInfo> = emptyList(),
    val isLoadingApps: Boolean = false,
    val showAppSheet: Boolean = false,
    val keyboardVisible: Boolean = false,
    val keyboardInput: String = "",
    val toast: String? = null,
    val pairingBackoffSeconds: Int = 0
)

/**
 * Owns connection lifecycle, scanning, pairing, credentials, settings, app
 * list/launch, keyboard input (auto-show/hide on focus state) and deep-link
 * processing. The thin AppsViewModel, KeyboardViewModel and DeepLinkViewModel
 * were folded into this.
 */
class DeviceViewModel(
    private val appContext: Context,
    private val repository: DeviceRepository,
    private val settingsRepository: SettingsRepository,
    private val credentialStorage: CredentialStorage,
    private val pairingHandler: PairingHandler = PairingHandler()
) : ViewModel() {

    private var pairingCollectJob: kotlinx.coroutines.Job? = null
    private var backoffJob: kotlinx.coroutines.Job? = null

    private fun showToast(message: String) {
        _uiState.update { it.copy(toast = message) }
        Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
    }

    private fun startBackoffCountdown(seconds: Int) {
        backoffJob?.cancel()
        _uiState.update { it.copy(pairingBackoffSeconds = seconds) }
        backoffJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _uiState.update { it.copy(pairingBackoffSeconds = remaining) }
                kotlinx.coroutines.delay(1000)
            }
            _uiState.update { it.copy(pairingBackoffSeconds = 0) }
        }
    }

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    val pairingState: StateFlow<PairingState> = pairingHandler.pairingState

    // For deep-link processing dialog
    private val _deepLinkProcessing = MutableStateFlow(false)
    val deepLinkProcessing: StateFlow<Boolean> = _deepLinkProcessing.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.showUnknownDevicesFlow.collectLatest { v ->
                _uiState.update { it.copy(showUnknownDevices = v) }
            }
        }
        viewModelScope.launch {
            settingsRepository.volumeCaptureEnabledFlow.collectLatest { v ->
                _uiState.update { it.copy(volumeCaptureEnabled = v) }
            }
        }
        viewModelScope.launch {
            settingsRepository.showMediaPlayerFlow.collectLatest { v ->
                _uiState.update { it.copy(showMediaPlayer = v) }
            }
        }
        viewModelScope.launch {
            settingsRepository.mediaNotificationEnabledFlow.collectLatest { v ->
                _uiState.update { it.copy(mediaNotificationEnabled = v) }
            }
        }
        viewModelScope.launch {
            settingsRepository.touchpadAtBottomFlow.collectLatest { v ->
                _uiState.update { it.copy(touchpadAtBottom = v) }
            }
        }
        // Keyboard focus state pushed from pyatv.
        viewModelScope.launch {
            repository.keyboardFocusFlow.collectLatest { focused ->
                _uiState.update { it.copy(keyboardVisible = focused || it.keyboardVisible && focused.not()) }
                if (!focused) _uiState.update { it.copy(keyboardInput = "") }
            }
        }
    }

    // ----------------------------------------------------------------- #
    // Initialization (auto-reconnect)
    // ----------------------------------------------------------------- #

    fun initialize(onNoDevice: () -> Unit) {
        viewModelScope.launch {
            val known = credentialStorage.allCredentials.first()
            val first = known.keys.firstOrNull()
            if (first != null) {
                connectToDevice(first, "Apple TV")
            } else {
                onNoDevice()
            }
        }
    }

    // ----------------------------------------------------------------- #
    // Intent dispatch
    // ----------------------------------------------------------------- #

    fun onIntent(intent: DeviceIntent) {
        when (intent) {
            DeviceIntent.LoadKnownDevices -> loadKnownDevices()
            DeviceIntent.ScanDevices -> scanDevices(null)
            is DeviceIntent.ScanByIp -> scanDevices(intent.ip)
            is DeviceIntent.ConnectToDevice -> connectToDevice(intent.identifier, intent.name)
            DeviceIntent.DisconnectDevice -> disconnect()
            is DeviceIntent.StartPairing -> startPairing(intent.device, intent.protocol)
            is DeviceIntent.SubmitPairingPin -> submitPin(intent.pin)
            DeviceIntent.CancelPairing -> cancelPairing()
            is DeviceIntent.ClearCredentials -> clearCredentials(intent.identifier, intent.name)
            DeviceIntent.ClearAllCredentials -> clearAllCredentials()
            is DeviceIntent.ToggleShowUnknownDevices -> toggleShowUnknown(intent.value)
            is DeviceIntent.ToggleVolumeCapture -> toggleVolumeCapture(intent.value)
            DeviceIntent.LoadApps -> loadApps()
            is DeviceIntent.LaunchApp -> launchApp(intent.bundleId, intent.name)
            DeviceIntent.DismissAppSheet -> _uiState.update { it.copy(showAppSheet = false) }
            DeviceIntent.ToggleKeyboardVisibility -> _uiState.update { it.copy(keyboardVisible = !it.keyboardVisible) }
            is DeviceIntent.UpdateKeyboardInput -> _uiState.update { it.copy(keyboardInput = intent.text) }
            is DeviceIntent.SubmitKeyboardText -> submitKeyboardText(intent.text)
            DeviceIntent.DismissKeyboard -> dismissKeyboard()
            is DeviceIntent.LaunchDeepLink -> launchDeepLink(intent.url)
        }
    }

    // ----------------------------------------------------------------- #
    // Scanning
    // ----------------------------------------------------------------- #

    private fun loadKnownDevices() {
        viewModelScope.launch {
            val known = credentialStorage.allCredentials.first()
            val knownDevices = known.keys.map { id ->
                ScannedDevice(name = "Saved Device", address = "", identifier = id)
            }
            _uiState.update { it.copy(discoveredDevices = knownDevices + it.discoveredDevices) }
        }
    }

    private fun scanDevices(host: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val result = repository.scanDevices(host)
            _uiState.update { it.copy(isScanning = false) }
            result.onSuccess { devices -> _uiState.update { it.copy(discoveredDevices = devices) } }
                .onFailure { showToast("Scan failed") }
        }
    }

    // ----------------------------------------------------------------- #
    // Connection
    // ----------------------------------------------------------------- #

    fun connectToDevice(identifier: String, name: String) {
        if (_uiState.value.connectionState == ConnectionState.CONNECTED &&
            _uiState.value.connectedDeviceId == identifier
        ) {
            disconnect()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(connectionState = ConnectionState.CONNECTING) }
            val result = repository.connectToDevice(identifier)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.CONNECTED,
                        connectedDeviceId = identifier,
                        connectedDeviceName = name
                    )
                }
                registerListeners(identifier)
            }.onFailure {
                _uiState.update {
                    it.copy(connectionState = ConnectionState.DISCONNECTED)
                }
                showToast("Connect failed")
            }
        }
    }

    private fun disconnect() {
        val id = _uiState.value.connectedDeviceId ?: return
        viewModelScope.launch {
            repository.disconnectFromDevice(id)
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.DISCONNECTED,
                    connectedDeviceId = null,
                    connectedDeviceName = null
                )
            }
        }
    }

    private fun registerListeners(identifier: String) {
        viewModelScope.launch {
            val pushCb = PyCallbacks.PushCallback(repository)
            val powerCb = PyCallbacks.PowerCallback(repository)
            val kbCb = PyCallbacks.KeyboardCallback(repository)
            repository.registerPushCallback(identifier, PyCallbacks.proxy(pushCb))
            repository.registerPowerCallback(identifier, PyCallbacks.proxy(powerCb))
            repository.startKeyboardListener(identifier, PyCallbacks.proxy(kbCb))
        }
    }

    // ----------------------------------------------------------------- #
    // Pairing
    // ----------------------------------------------------------------- #

    private fun startPairing(device: ScannedDevice, protocol: String) {
        android.util.Log.d("DeviceViewModel", "startPairing: device=${device.name} id=${device.identifier} protocol=$protocol")
        pairingCollectJob?.cancel()
        pairingHandler.startPairingFlow(device.identifier, device.name, listOf(protocol))
        pairingCollectJob = viewModelScope.launch {
            pairingHandler.pairingState.collect { state ->
                android.util.Log.d("DeviceViewModel", "pairingState = $state")
                when (state) {
                    is PairingState.Pairing -> {
                        android.util.Log.d("DeviceViewModel", "Calling repository.startPairing(${device.identifier}, ${state.protocol})")
                        repository.startPairing(device.identifier, state.protocol)
                            .onSuccess { key ->
                                android.util.Log.d("DeviceViewModel", "startPairing success, sessionKey=$key")
                                pairingHandler.awaitingPin(key, state.protocol)
                            }
                            .onFailure { err ->
                                android.util.Log.e("DeviceViewModel", "startPairing failed", err)
                                val msg = err.message ?: "Pairing failed"
                                android.util.Log.d("DeviceViewModel", "error message: $msg")
                                val backoffMatch = Regex("BackOff=(\\d+)s").find(msg)
                                if (backoffMatch != null) {
                                    val seconds = backoffMatch.groupValues[1].toInt()
                                    showToast("Wait ${seconds}s before retrying pairing.")
                                    startBackoffCountdown(seconds)
                                    pairingHandler.cancel()
                                    pairingCollectJob?.cancel()
                                    pairingCollectJob = null
                                } else {
                                    showToast("Pairing failed: $msg")
                                    pairingHandler.failCurrentProtocol()
                                }
                            }
                    }
                    is PairingState.WaitingForPin -> {
                        android.util.Log.d("DeviceViewModel", "WaitingForPin, updating uiState")
                        _uiState.update { it.copy(pairingState = state) }
                    }
                    is PairingState.FlowCompleted -> {
                        android.util.Log.d("DeviceViewModel", "FlowCompleted, connecting")
                        repository.connectToDevice(state.identifier)
                        _uiState.update {
                            it.copy(
                                connectionState = ConnectionState.CONNECTED,
                                connectedDeviceId = state.identifier,
                                connectedDeviceName = state.deviceName,
                                pairingState = PairingState.Idle
                            )
                        }
                        pairingHandler.cancel()
                    }
                    is PairingState.Error -> showToast(state.message)
                    else -> Unit
                }
            }
        }
    }

    private fun submitPin(pin: String) {
        val state = pairingHandler.pairingState.value as? PairingState.WaitingForPin ?: return
        viewModelScope.launch {
            repository.finishPairingAndSave(state.sessionKey, pin)
                .onSuccess {
                    pairingHandler.complete(_uiState.value.connectedDeviceId ?: "", state.deviceName)
                }
                .onFailure { pairingHandler.failCurrentProtocol() }
        }
    }

    private fun cancelPairing() {
        val state = pairingHandler.pairingState.value as? PairingState.WaitingForPin
        val key = state?.sessionKey
        pairingCollectJob?.cancel()
        pairingCollectJob = null
        viewModelScope.launch {
            if (key != null) repository.cancelPairing(key)
            pairingHandler.cancel()
            _uiState.update { it.copy(pairingState = PairingState.Idle) }
        }
    }

    // ----------------------------------------------------------------- #
    // Credentials
    // ----------------------------------------------------------------- #

    private fun clearCredentials(identifier: String, name: String) {
        viewModelScope.launch {
            credentialStorage.clearCredentials(identifier)
            showToast("Forgot $name")
        }
    }

    private fun clearAllCredentials() {
        viewModelScope.launch {
            val id = _uiState.value.connectedDeviceId
            credentialStorage.clearAllCredentials()
            if (id != null) repository.disconnectFromDevice(id)
            _uiState.update {
                it.copy(
                    connectionState = ConnectionState.DISCONNECTED,
                    connectedDeviceId = null,
                    connectedDeviceName = null,
                    toast = "Forgot all devices"
                )
            }
        }
    }

    // ----------------------------------------------------------------- #
    // Settings toggles
    // ----------------------------------------------------------------- #

    fun setShowMediaPlayer(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowMediaPlayer(value) }
    }

    fun setMediaNotificationEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setMediaNotificationEnabled(value) }
    }

    fun setTouchpadAtBottom(value: Boolean) {
        viewModelScope.launch { settingsRepository.setTouchpadAtBottom(value) }
    }

    private fun toggleShowUnknown(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowUnknownDevices(value) }
    }

    private fun toggleVolumeCapture(value: Boolean) {
        viewModelScope.launch { settingsRepository.setVolumeCaptureEnabled(value) }
    }

    // ----------------------------------------------------------------- #
    // Apps
    // ----------------------------------------------------------------- #

    private fun loadApps() {
        val id = _uiState.value.connectedDeviceId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true, showAppSheet = true) }
            repository.getAppList(id)
                .onSuccess { apps -> _uiState.update { it.copy(availableApps = apps, isLoadingApps = false) } }
                .onFailure { _uiState.update { it.copy(isLoadingApps = false, toast = "Apps load failed") } }
        }
    }

    private fun launchApp(bundleId: String, name: String) {
        val id = _uiState.value.connectedDeviceId ?: return
        viewModelScope.launch {
            repository.launchApp(id, bundleId)
                .onSuccess { _uiState.update { it.copy(showAppSheet = false, toast = "Launched $name") } }
                .onFailure { showToast("Launch failed") }
        }
    }

    // ----------------------------------------------------------------- #
    // Keyboard
    // ----------------------------------------------------------------- #

    private fun submitKeyboardText(text: String) {
        val id = _uiState.value.connectedDeviceId ?: return
        viewModelScope.launch {
            repository.sendKeyboardText(id, text)
            _uiState.update { it.copy(keyboardInput = "") }
        }
    }

    private fun dismissKeyboard() {
        val id = _uiState.value.connectedDeviceId ?: return
        viewModelScope.launch {
            repository.clearKeyboardText(id)
            _uiState.update { it.copy(keyboardVisible = false, keyboardInput = "") }
        }
    }

    // ----------------------------------------------------------------- #
    // Deep-link
    // ----------------------------------------------------------------- #

    private fun launchDeepLink(url: String) {
        val id = _uiState.value.connectedDeviceId ?: return
        viewModelScope.launch {
            _deepLinkProcessing.value = true
            repository.launchApp(id, url)
            _deepLinkProcessing.value = false
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }
}