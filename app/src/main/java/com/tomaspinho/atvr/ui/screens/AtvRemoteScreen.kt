package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.tomaspinho.atvr.domain.PairingState
import com.tomaspinho.atvr.repository.SettingsRepository
import com.tomaspinho.atvr.ui.components.AtvTopAppBar
import com.tomaspinho.atvr.ui.components.VolumeFeedback
import com.tomaspinho.atvr.ui.theme.ThemeType
import com.tomaspinho.atvr.viewmodel.device.DeviceIntent
import com.tomaspinho.atvr.viewmodel.device.DeviceViewModelFactory
import com.tomaspinho.atvr.viewmodel.remote.RemoteControlIntent
import com.tomaspinho.atvr.viewmodel.remote.RemoteControlViewModelFactory

@Composable
fun AtvRemoteScreen(
    onResetOnboarding: () -> Unit,
    startWithDeviceSheetOpen: Boolean,
    onStartWithDeviceSheetOpenConsumed: () -> Unit
) {
    val deviceViewModel: com.tomaspinho.atvr.viewmodel.device.DeviceViewModel =
        viewModel(factory = DeviceViewModelFactory())
    val remoteControlViewModel: com.tomaspinho.atvr.viewmodel.remote.RemoteControlViewModel =
        viewModel(factory = RemoteControlViewModelFactory())

    val deviceState by deviceViewModel.uiState.collectAsStateWithLifecycle()
    val remoteState by remoteControlViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val currentTheme by settingsRepository.themeFlow.collectAsStateWithLifecycle(
        initialValue = ThemeType.SYSTEM
    )
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showTroubleshooting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deviceViewModel.initialize {
            showDeviceSheet = true
        }
    }
    LaunchedEffect(startWithDeviceSheetOpen) {
        if (startWithDeviceSheetOpen) {
            showDeviceSheet = true
            onStartWithDeviceSheetOpenConsumed()
        }
    }
    LaunchedEffect(showDeviceSheet) {
        if (showDeviceSheet) {
            deviceViewModel.onIntent(DeviceIntent.LoadKnownDevices)
            deviceViewModel.onIntent(DeviceIntent.ScanDevices)
        }
    }
    LaunchedEffect(deviceState.toast) {
        deviceState.toast?.let {
            snackbarHostState.showSnackbar(it)
            deviceViewModel.clearToast()
        }
    }
    LaunchedEffect(remoteState.toast) {
        remoteState.toast?.let {
            snackbarHostState.showSnackbar(it)
            remoteControlViewModel.clearToast()
        }
    }
    LaunchedEffect(deviceState.connectionState, deviceState.connectedDeviceId) {
        val connected = deviceState.connectionState == com.tomaspinho.atvr.domain.ConnectionState.CONNECTED
        remoteControlViewModel.setConnectedDevice(deviceState.connectedDeviceId, connected)
    }

    // Volume feedback overlay auto-hides after VOLUME_FEEDBACK_DURATION ms.
    LaunchedEffect(remoteState.volumeFeedback) {
        if (remoteState.volumeFeedback.visible) {
            kotlinx.coroutines.delay(com.tomaspinho.atvr.viewmodel.remote.RemoteControlViewModel.VOLUME_FEEDBACK_DURATION)
            remoteControlViewModel.hideVolumeFeedback()
        }
    }

    val pairingState = deviceViewModel.pairingState.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = {
            AtvTopAppBar(
                deviceName = deviceState.connectedDeviceName,
                isConnected = remoteState.isConnected,
                powerState = remoteState.powerState,
                onHeaderClick = { showDeviceSheet = true },
                onAppsClick = { deviceViewModel.onIntent(DeviceIntent.LoadApps) },
                onPowerClick = { remoteControlViewModel.onIntent(RemoteControlIntent.TogglePower) },
                onKeyboardClick = { deviceViewModel.onIntent(DeviceIntent.ToggleKeyboardVisibility) },
                onSettingsClick = { showSettingsSheet = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            RemoteControlContent(
                isConnected = remoteState.isConnected,
                mediaInfo = remoteState.mediaInfo,
                showMediaPlayer = deviceState.showMediaPlayer,
                touchpadAtBottom = deviceState.touchpadAtBottom,
                onCommand = { cmd, action ->
                    remoteControlViewModel.onIntent(RemoteControlIntent.SendCommand(cmd, action))
                }
            )

            // Overlays (conditionally composed)
            DeviceSelectionSheet(
                visible = showDeviceSheet,
                devices = deviceState.discoveredDevices,
                connectedDeviceId = deviceState.connectedDeviceId,
                savedProtocols = emptyMap(),
                isScanning = deviceState.isScanning,
                showUnknown = deviceState.showUnknownDevices,
                onDismiss = { showDeviceSheet = false },
                onDeviceSelected = { id, name ->
                    if (id.isEmpty()) deviceViewModel.onIntent(DeviceIntent.DisconnectDevice)
                    else deviceViewModel.onIntent(DeviceIntent.ConnectToDevice(id, name))
                },
                onRemoveDevice = { id, name -> deviceViewModel.onIntent(DeviceIntent.ClearCredentials(id, name)) },
                onPair = { device -> deviceViewModel.onIntent(DeviceIntent.StartPairing(device)) },
                onScan = { deviceViewModel.onIntent(DeviceIntent.ScanDevices) },
                onManualIp = { showTroubleshooting = true },
                onToggleShowUnknown = { deviceViewModel.onIntent(DeviceIntent.ToggleShowUnknownDevices(it)) },
                pairingBackoffSeconds = deviceState.pairingBackoffSeconds
            )

            AppLauncherSheet(
                visible = deviceState.showAppSheet,
                apps = deviceState.availableApps,
                isLoading = deviceState.isLoadingApps,
                onAppSelected = { bundleId, name ->
                    deviceViewModel.onIntent(DeviceIntent.LaunchApp(bundleId, name))
                },
                onDismiss = { deviceViewModel.onIntent(DeviceIntent.DismissAppSheet) }
            )

            if (pairingState is PairingState.WaitingForPin) {
                PairingDialog(
                    deviceName = pairingState.deviceName,
                    onSubmit = { pin -> deviceViewModel.onIntent(DeviceIntent.SubmitPairingPin(pin)) },
                    onDismiss = { deviceViewModel.onIntent(DeviceIntent.CancelPairing) }
                )
            }

            SettingsSheet(
                visible = showSettingsSheet,
                showMediaPlayer = deviceState.showMediaPlayer,
                mediaNotificationEnabled = deviceState.mediaNotificationEnabled,
                touchpadAtBottom = deviceState.touchpadAtBottom,
                volumeCaptureEnabled = deviceState.volumeCaptureEnabled,
                onToggleMediaPlayer = { deviceViewModel.setShowMediaPlayer(it) },
                onToggleMediaNotification = { deviceViewModel.setMediaNotificationEnabled(it) },
                onToggleTouchpadAtBottom = { deviceViewModel.setTouchpadAtBottom(it) },
                onToggleVolumeCapture = { deviceViewModel.onIntent(DeviceIntent.ToggleVolumeCapture(it)) },
                onResetOnboarding = {
                    showSettingsSheet = false
                    onResetOnboarding()
                },
                onOpenTheme = {
                    showSettingsSheet = false
                    showThemeSheet = true
                },
                onResetPairState = {
                    showSettingsSheet = false
                    deviceViewModel.onIntent(DeviceIntent.ClearAllCredentials)
                },
                onDismiss = { showSettingsSheet = false }
            )

            ThemeSelectionSheet(
                visible = showThemeSheet,
                current = currentTheme,
                onThemeChange = { type ->
                    coroutineScope.launch { settingsRepository.setTheme(type) }
                    showThemeSheet = false
                },
                onDismiss = { showThemeSheet = false }
            )

            if (deviceState.keyboardVisible) {
                KeyboardDialog(
                    inputValue = deviceState.keyboardInput,
                    onValueChange = { deviceViewModel.onIntent(DeviceIntent.UpdateKeyboardInput(it)) },
                    onSend = { deviceViewModel.onIntent(DeviceIntent.SubmitKeyboardText(it)) },
                    onDismiss = { deviceViewModel.onIntent(DeviceIntent.DismissKeyboard) }
                )
            }

            if (showTroubleshooting) {
                TroubleshootingDialog(
                    onScan = { ip ->
                        deviceViewModel.onIntent(DeviceIntent.ScanByIp(ip))
                        showTroubleshooting = false
                    },
                    onDismiss = { showTroubleshooting = false }
                )
            }

            VolumeFeedback(
                visible = remoteState.volumeFeedback.visible,
                direction = remoteState.volumeFeedback.direction,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}