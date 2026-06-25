package com.tomaspinho.atvr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tomaspinho.atvr.domain.ScannedDevice

/**
 * Card showing a device's name, address, credential status, and Connect /
 * Pair / Clear-credentials actions. Highlighted when it's the connected device.
 */
@Composable
fun DeviceCard(
    device: ScannedDevice,
    isConnected: Boolean,
    savedProtocols: List<String>,
    onConnect: (String, String) -> Unit,
    onPair: (ScannedDevice) -> Unit,
    onClearCredentials: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = if (isConnected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (isConnected) "✓ ${device.name}" else device.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(device.address, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(4.dp))
            val status = if (savedProtocols.isEmpty()) {
                "⚠️ No saved credentials — pair first"
            } else {
                "🔑 Saved: ${savedProtocols.joinToString(", ")}"
            }
            Text(status, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConnect(device.identifier, device.name) },
                    modifier = Modifier.weight(1f)
                ) { Text("Connect") }
                OutlinedButton(
                    onClick = { onPair(device) },
                    modifier = Modifier.weight(1f)
                ) { Text("Pair") }
                if (savedProtocols.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onClearCredentials(device.identifier, device.name) },
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Forget",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}