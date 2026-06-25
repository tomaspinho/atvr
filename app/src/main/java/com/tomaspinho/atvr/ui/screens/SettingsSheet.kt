package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    visible: Boolean,
    showMediaPlayer: Boolean,
    mediaNotificationEnabled: Boolean,
    touchpadAtBottom: Boolean,
    volumeCaptureEnabled: Boolean,
    onToggleMediaPlayer: (Boolean) -> Unit,
    onToggleMediaNotification: (Boolean) -> Unit,
    onToggleTouchpadAtBottom: (Boolean) -> Unit,
    onToggleVolumeCapture: (Boolean) -> Unit,
    onResetOnboarding: () -> Unit,
    onOpenTheme: () -> Unit,
    onResetPairState: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState()
    var displayExpanded by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Display") },
                leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { displayExpanded = !displayExpanded }
            )
            if (displayExpanded) {
                SwitchRow("Show Media Player Card", showMediaPlayer, onToggleMediaPlayer)
                SwitchRow("Show Media Notification", mediaNotificationEnabled, onToggleMediaNotification)
                SwitchRow("Move Touchpad to Bottom", touchpadAtBottom, onToggleTouchpadAtBottom)
            }

            SwitchRow("Use Volume Buttons", volumeCaptureEnabled, onToggleVolumeCapture)

            ListItem(
                headlineContent = { Text("Reset Tutorial") },
                supportingContent = { Text("Show the welcome screen again") },
                trailingContent = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResetOnboarding() }
            )

            ListItem(
                headlineContent = { Text("Appearance") },
                supportingContent = { Text("Customize app look and feel") },
                trailingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTheme() }
            )

            ListItem(
                headlineContent = { Text("Reset Pair State", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Forget all paired devices") },
                trailingContent = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResetPairState() }
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}