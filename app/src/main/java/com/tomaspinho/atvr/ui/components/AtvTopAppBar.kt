package com.tomaspinho.atvr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tomaspinho.atvr.domain.PowerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtvTopAppBar(
    deviceName: String?,
    isConnected: Boolean,
    powerState: PowerState,
    onHeaderClick: () -> Unit,
    onAppsClick: () -> Unit,
    onPowerClick: () -> Unit,
    onKeyboardClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onHeaderClick() }
            ) {
                Text(deviceName ?: "Not Connected")
                if (isConnected) {
                    Spacer(Modifier.size(6.dp))
                    StatusDot(color = Color(0xFF4CAF50))
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onAppsClick, enabled = isConnected) {
                Icon(
                    Icons.Filled.Apps,
                    contentDescription = "Apps",
                    tint = if (isConnected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        },
        actions = {
            if (isConnected) {
                val powerColor = when (powerState) {
                    PowerState.ON -> Color(0xFF4CAF50)
                    PowerState.OFF, PowerState.STANDBY -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                IconButton(onClick = onPowerClick) {
                    Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Power", tint = powerColor)
                }
                IconButton(onClick = onKeyboardClick) {
                    Icon(Icons.Filled.Keyboard, contentDescription = "Keyboard")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    )
}