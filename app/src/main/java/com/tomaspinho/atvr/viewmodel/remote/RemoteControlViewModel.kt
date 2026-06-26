package com.tomaspinho.atvr.viewmodel.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tomaspinho.atvr.domain.MediaInfo
import com.tomaspinho.atvr.domain.PowerState
import com.tomaspinho.atvr.domain.VolumeDirection
import com.tomaspinho.atvr.repository.DeviceRepository

sealed interface RemoteControlIntent {
    data class SendCommand(val command: String, val action: String = "press") : RemoteControlIntent
    data object TogglePower : RemoteControlIntent
    data object RefreshMedia : RemoteControlIntent
    data object RefreshPowerState : RemoteControlIntent
}

data class VolumeFeedbackState(val visible: Boolean = false, val direction: VolumeDirection = VolumeDirection.UP)

data class RemoteControlUiState(
    val isConnected: Boolean = false,
    val mediaInfo: MediaInfo? = null,
    val powerState: PowerState = PowerState.UNKNOWN,
    val volumeFeedback: VolumeFeedbackState = VolumeFeedbackState()
)

/**
 * Remote commands (D-pad, play, volume, power), now-playing media info, power
 * state, volume feedback overlay state.
 */
class RemoteControlViewModel(private val repository: DeviceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteControlUiState())
    val uiState: StateFlow<RemoteControlUiState> = _uiState.asStateFlow()

    private var connectedDeviceId: String? = null

    init {
        viewModelScope.launch {
            repository.mediaInfoFlow.collectLatest { info ->
                _uiState.update { it.copy(mediaInfo = info) }
            }
        }
        viewModelScope.launch {
            repository.powerStateFlow.collectLatest { state ->
                _uiState.update { it.copy(powerState = state) }
            }
        }
    }

    fun setConnectedDevice(id: String?, connected: Boolean) {
        connectedDeviceId = id
        _uiState.update { it.copy(isConnected = connected) }
        if (connected && id != null) {
            refreshPowerState()
            refreshMedia()
        }
    }

    fun onIntent(intent: RemoteControlIntent) {
        when (intent) {
            is RemoteControlIntent.SendCommand -> sendCommand(intent.command, intent.action)
            RemoteControlIntent.TogglePower -> togglePower()
            RemoteControlIntent.RefreshMedia -> refreshMedia()
            RemoteControlIntent.RefreshPowerState -> refreshPowerState()
        }
    }

    fun sendCommand(command: String, action: String = "press") {
        val id = connectedDeviceId ?: return
        if (action == "press") {
            when (command) {
                "volume_up" -> _uiState.update {
                    it.copy(volumeFeedback = VolumeFeedbackState(true, VolumeDirection.UP))
                }
                "volume_down" -> _uiState.update {
                    it.copy(volumeFeedback = VolumeFeedbackState(true, VolumeDirection.DOWN))
                }
            }
        }
        viewModelScope.launch {
            val result = repository.sendRemoteCommand(id, command, action)
            if (result.isFailure) {
                // Explicit reconnect on dropped connection, then retry once.
                val reconnect = repository.connectToDevice(id)
                if (reconnect.isSuccess) {
                    repository.sendRemoteCommand(id, command, action)
                }
            }
        }
    }

    fun togglePower() {
        val id = connectedDeviceId ?: return
        viewModelScope.launch {
            repository.togglePower(id)
            delay(1000)
            refreshPowerState()
        }
    }

    fun refreshMedia() {
        val id = connectedDeviceId ?: return
        viewModelScope.launch {
            repository.getNowPlaying(id).onSuccess { info ->
                _uiState.update { it.copy(mediaInfo = info) }
            }
        }
    }

    fun refreshPowerState() {
        val id = connectedDeviceId ?: return
        viewModelScope.launch {
            repository.getPowerState(id).onSuccess { state ->
                _uiState.update { it.copy(powerState = state) }
            }
        }
    }

    fun hideVolumeFeedback() {
        _uiState.update { it.copy(volumeFeedback = it.volumeFeedback.copy(visible = false)) }
    }

    companion object {
        const val VOLUME_FEEDBACK_DURATION = 1000L
    }
}