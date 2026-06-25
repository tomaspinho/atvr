package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomaspinho.atvr.ui.theme.ThemeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionSheet(
    visible: Boolean,
    current: ThemeType,
    onThemeChange: (ThemeType) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp)) {
            Text("Appearance", style = MaterialTheme.typography.headlineSmall)
            ThemeOption("System Default", current == ThemeType.SYSTEM) { onThemeChange(ThemeType.SYSTEM) }
            ThemeOption("Light", current == ThemeType.LIGHT) { onThemeChange(ThemeType.LIGHT) }
            ThemeOption("Dark", current == ThemeType.DARK) { onThemeChange(ThemeType.DARK) }
            ThemeOption("True Black", current == ThemeType.AMOLED) { onThemeChange(ThemeType.AMOLED) }
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}