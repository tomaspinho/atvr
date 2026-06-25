package com.tomaspinho.atvr.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.tomaspinho.atvr.data.CredentialStorage
import com.tomaspinho.atvr.domain.PairingHandler
import com.tomaspinho.atvr.domain.PairingState
import com.tomaspinho.atvr.domain.ScannedDevice
import com.tomaspinho.atvr.repository.DeviceRepository
import com.tomaspinho.atvr.repository.SettingsRepository

/**
 * Multi-step onboarding wizard: permissions, scan, device selection, pairing,
 * completion. Drives the [PairingHandler] state machine and the scan flow.
 */
class OnboardingViewModel(
    private val repository: DeviceRepository,
    private val settingsRepository: SettingsRepository,
    private val credentialStorage: CredentialStorage,
    private val pairingHandler: PairingHandler = PairingHandler()
) : ViewModel() {

    private val _step = MutableStateFlow(OnboardingStep.WELCOME)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val pairingState: StateFlow<PairingState> = pairingHandler.pairingState

    val onboardingComplete = settingsRepository.onboardingCompleteFlow

    private var selectedDevice: ScannedDevice? = null

    // ----------------------------------------------------------------- #
    // Step transitions
    // ----------------------------------------------------------------- #

    fun nextStep() {
        _step.value = when (_step.value) {
            OnboardingStep.WELCOME -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> OnboardingStep.DEVICE_SCAN
            OnboardingStep.DEVICE_SCAN -> OnboardingStep.DEVICE_SELECTION
            OnboardingStep.DEVICE_SELECTION -> OnboardingStep.SETTINGS
            OnboardingStep.SETTINGS -> OnboardingStep.COMPLETED
            OnboardingStep.COMPLETED -> OnboardingStep.COMPLETED
        }
        if (_step.value == OnboardingStep.DEVICE_SCAN) {
            startScanning()
        }
    }

    fun skip() {
        nextStep()
    }

    // ----------------------------------------------------------------- #
    // Scanning
    // ----------------------------------------------------------------- #

    fun startScanning(host: String? = null) {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            val result = repository.scanDevices(host)
            _isScanning.value = false
            result
                .onSuccess { devices ->
                    _scannedDevices.value = devices
                    if (devices.isNotEmpty() && _step.value == OnboardingStep.DEVICE_SCAN) {
                        nextStep()
                    }
                }
                .onFailure { _snackbarMessage.value = it.message ?: "Scan failed" }
        }
    }

    fun scanByIp(ip: String) {
        startScanning(ip)
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // ----------------------------------------------------------------- #
    // Device selection + pairing
    // ----------------------------------------------------------------- #

    fun selectDevice(device: ScannedDevice) {
        selectedDevice = device
        pairingHandler.startPairingFlow(device.identifier, device.name, listOf("mrp"))
        viewModelScope.launch {
            collectPairingState(device)
        }
    }

    private suspend fun collectPairingState(device: ScannedDevice) {
        pairingHandler.pairingState.collect { state ->
            when (state) {
                is PairingState.Pairing -> {
                    val session = repository.startPairing(device.identifier, state.protocol)
                    session.onSuccess { key -> pairingHandler.awaitingPin(key, state.protocol) }
                        .onFailure {
                            tryFallback(device)
                        }
                }
                is PairingState.FlowCompleted -> {
                    repository.connectToDevice(state.identifier)
                    nextStep()
                }
                is PairingState.Error -> _snackbarMessage.value = state.message
                else -> Unit
            }
        }
    }

    private fun tryFallback(device: ScannedDevice) {
        // Only one fallback queue run; after MRP fails, queue airplay+companion.
        if (pairingHandler.pairingState.value is PairingState.Error) return
        pairingHandler.startPairingFlow(device.identifier, device.name, listOf("airplay", "companion"))
    }

    fun submitPairingPin(pin: String) {
        val state = pairingState.value as? PairingState.WaitingForPin ?: return
        viewModelScope.launch {
            val result = repository.finishPairingAndSave(state.sessionKey, pin)
            result.onSuccess { pairingHandler.complete(selectedDevice?.identifier ?: "", state.deviceName) }
                .onFailure { tryFallback(selectedDevice ?: return@launch) }
        }
    }

    fun cancelPairing() {
        val state = pairingState.value as? PairingState.WaitingForPin
        val key = state?.sessionKey
        viewModelScope.launch {
            if (key != null) repository.cancelPairing(key)
            pairingHandler.cancel()
        }
    }

    // ----------------------------------------------------------------- #
    // Completion
    // ----------------------------------------------------------------- #

    fun markOnboardingComplete() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }
    }

    fun resetOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(false) }
        _step.value = OnboardingStep.WELCOME
        _scannedDevices.value = emptyList()
        pairingHandler.cancel()
    }

}