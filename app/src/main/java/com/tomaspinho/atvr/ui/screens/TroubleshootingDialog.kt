package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun TroubleshootingDialog(
    onScan: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var ip by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = ShapeDefaults.Large) {
            Column(Modifier.padding(24.dp)) {
                Text("Manual Connection", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.size(8.dp))
                Text("Enter the IP address of your Apple TV to connect directly.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.size(16.dp))
                Column(Modifier.padding(16.dp)) {
                    Text("Enter IP Manually", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.size(8.dp))
                    Row {
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            placeholder = { Text("192.168.1.x") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                onScan(ip); keyboard?.hide()
                            }),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.size(8.dp))
                        Button(onClick = { onScan(ip); keyboard?.hide() }, enabled = ip.isNotBlank()) {
                            Text("Connect")
                        }
                    }
                    Text("Settings > Network", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.size(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}