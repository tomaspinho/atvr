package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomaspinho.atvr.ui.components.AirPlayHelpCard
import com.tomaspinho.atvr.ui.theme.RemoteShapes

/**
 * Material Dialog shown when pairing with an Apple TV is in progress.
 * Always shows the PIN entry field (the `device_provides_pin` branch was
 * never implemented in practice and has been removed).
 */
@Composable
fun PairingDialog(
    deviceName: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSubmit(pin) },
                enabled = pin.length == 4
            ) { Text("Pair") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Pair with Apple TV") },
        text = {
            Column {
                Text("Enter the PIN shown on $deviceName", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.size(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value: String -> pin = value.filter { c -> c.isDigit() }.take(4) },
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RemoteShapes.Large,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(16.dp))
                AirPlayHelpCard(
                    title = "Having trouble with the PIN?",
                    desc = "Follow these steps to enter the PIN correctly.",
                    instructions = listOf(
                        "On your Apple TV, a 4-digit PIN should appear.",
                        "Enter exactly the digits shown on screen.",
                        "If the PIN expires, cancel and start pairing again."
                    )
                )
            }
        }
    )
}