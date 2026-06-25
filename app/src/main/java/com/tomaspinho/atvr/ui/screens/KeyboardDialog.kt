package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun KeyboardDialog(
    inputValue: String,
    onValueChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onSend(inputValue) }) { Text("Send") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hide") } },
        title = { Text("Apple TV Input") },
        text = {
            Column {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onValueChange,
                    placeholder = { Text("Type to send…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend(inputValue) })
                )
                Spacer(Modifier.size(8.dp))
            }
        }
    )
}