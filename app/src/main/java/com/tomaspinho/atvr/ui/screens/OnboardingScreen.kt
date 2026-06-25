package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomaspinho.atvr.R
import com.tomaspinho.atvr.ui.components.AirPlayHelpCard
import com.tomaspinho.atvr.ui.components.GradientButton
import com.tomaspinho.atvr.ui.components.RadarAnimation
import com.tomaspinho.atvr.viewmodel.onboarding.OnboardingStep
import com.tomaspinho.atvr.viewmodel.onboarding.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val devices by viewModel.scannedDevices.collectAsStateWithLifecycle()
    val snackbar by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbar) {
        if (snackbar != null) {
            snackbarHostState.showSnackbar(snackbar!!)
            viewModel.clearSnackbar()
        }
    }

    var showTroubleshooting by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(onGetStarted = { viewModel.nextStep() })
                OnboardingStep.PERMISSIONS -> PermissionsStep(onGrant = { viewModel.nextStep() })
                OnboardingStep.DEVICE_SCAN -> DeviceScanStep(
                    isScanning = isScanning,
                    hasDevices = devices.isNotEmpty(),
                    onRetry = { viewModel.startScanning() },
                    onManualIp = { showTroubleshooting = true },
                    onSkip = { viewModel.skip() },
                    onSupportClick = {}
                )
                OnboardingStep.DEVICE_SELECTION -> DeviceSelectionStep(
                    devices = devices,
                    onDeviceSelected = { viewModel.selectDevice(it) },
                    onRescan = { viewModel.startScanning() },
                    onSkip = { viewModel.skip() }
                )
                OnboardingStep.SETTINGS -> SettingsStep(onComplete = {
                    viewModel.markOnboardingComplete()
                    onComplete()
                })
                OnboardingStep.COMPLETED -> SettingsStep(onComplete = {
                    viewModel.markOnboardingComplete()
                    onComplete()
                })
            }
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(MaterialTheme.shapes.large)
                .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, Color.Black)))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        GradientButton(text = stringResource(R.string.onboarding_get_started), onClick = onGetStarted)
    }
}

@Composable
private fun PermissionsStep(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_setup_remote),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_permission_network_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        GradientButton(text = stringResource(R.string.onboarding_grant_access), onClick = onGrant)
    }
}

@Composable
private fun DeviceScanStep(
    isScanning: Boolean,
    hasDevices: Boolean,
    onRetry: () -> Unit,
    onManualIp: () -> Unit,
    onSkip: () -> Unit,
    onSupportClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            if (isScanning) RadarAnimation(size = 200.dp)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
        val statusText = when {
            isScanning -> stringResource(R.string.scanning)
            hasDevices -> stringResource(R.string.onboarding_devices_found)
            else -> stringResource(R.string.onboarding_no_devices_found)
        }
        Text(statusText, style = MaterialTheme.typography.titleMedium)
        if (!isScanning && !hasDevices) {
            Spacer(Modifier.height(16.dp))
            AirPlayHelpCard(
                title = "Can't find your Apple TV?",
                desc = "Try these steps to get discovered.",
                instructions = listOf(
                    "Check that your Apple TV and phone are on the same Wi-Fi network.",
                    "Make sure your Apple TV is powered on and awake.",
                    "Verify AirPlay is enabled in Apple TV settings.",
                    "If you still can't find it, try entering the IP address manually."
                ),
                onSupportClick = onSupportClick
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onManualIp) { Text(stringResource(R.string.onboarding_manual_ip)) }
            Spacer(Modifier.height(8.dp))
            GradientButton(text = stringResource(R.string.onboarding_try_again), onClick = onRetry)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
        }
    }
}

@Composable
private fun DeviceSelectionStep(
    devices: List<com.tomaspinho.atvr.domain.ScannedDevice>,
    onDeviceSelected: (com.tomaspinho.atvr.domain.ScannedDevice) -> Unit,
    onRescan: () -> Unit,
    onSkip: () -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = stringResource(R.string.onboarding_select_tv),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(devices, key = { it.identifier }) { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Filled.Check, contentDescription = null)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.onboarding_show_all), modifier = Modifier.weight(1f))
            Switch(checked = showAll, onCheckedChange = { showAll = it })
        }
        TextButton(onClick = onRescan) { Text(stringResource(R.string.onboarding_rescan)) }
        TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
    }
}

@Composable
private fun SettingsStep(onComplete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.onboarding_all_set), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onboarding_all_set_desc), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        GradientButton(text = stringResource(R.string.onboarding_lets_go), onClick = onComplete)
    }
}