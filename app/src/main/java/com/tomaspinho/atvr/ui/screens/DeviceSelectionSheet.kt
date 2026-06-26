package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tomaspinho.atvr.domain.ScannedDevice
import com.tomaspinho.atvr.ui.components.AirPlayHelpCard
import com.tomaspinho.atvr.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionSheet(
    visible: Boolean,
    devices: List<ScannedDevice>,
    connectedDeviceId: String?,
    savedProtocols: Map<String, List<String>>,
    isScanning: Boolean,
    showUnknown: Boolean,
    onDismiss: () -> Unit,
    onDeviceSelected: (String, String) -> Unit,
    onRemoveDevice: (String, String) -> Unit,
    onPair: (ScannedDevice) -> Unit,
    onScan: () -> Unit,
    onManualIp: () -> Unit,
    onToggleShowUnknown: (Boolean) -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Devices", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { /* debug */ }) {
                    Icon(Icons.Filled.BugReport, contentDescription = "Debug")
                }
            }
            if (isScanning && devices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(size = 64, strokeWidth = 6)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
                items(devices, key = { it.identifier }) { device ->
                    val connected = device.identifier == connectedDeviceId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device.identifier, device.name) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Tv, contentDescription = null)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(device.name, fontWeight = FontWeight.SemiBold)
                            Text(device.address, style = MaterialTheme.typography.bodySmall)
                        }
                        if (connected) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            val hasCreds = savedProtocols[device.identifier]?.isNotEmpty() == true
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!hasCreds) {
                                    OutlinedButton(
                                        onClick = { onPair(device) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) { Text("Pair") }
                                }
                                if (hasCreds) {
                                    IconButton(onClick = { onRemoveDevice(device.identifier, device.name) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Forget", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            Spacer(Modifier.size(8.dp))
            if (isScanning) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(size = 32)
                }
            } else {
                Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan for Devices")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show all devices", modifier = Modifier.weight(1f))
                Switch(checked = showUnknown, onCheckedChange = onToggleShowUnknown)
            }
            Button(onClick = onManualIp, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.BugReport, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Manual IP")
            }
            if (devices.isEmpty()) {
                Spacer(Modifier.size(12.dp))
                AirPlayHelpCard(
                    title = "Can't find your Apple TV?",
                    desc = "Try these steps to get discovered.",
                    instructions = listOf(
                        "Check that your Apple TV and phone are on the same Wi-Fi network.",
                        "Make sure your Apple TV is powered on and awake.",
                        "Verify AirPlay is enabled in Apple TV settings.",
                        "If you still can't find it, try entering the IP address manually."
                    )
                )
            }
        }
    }
}