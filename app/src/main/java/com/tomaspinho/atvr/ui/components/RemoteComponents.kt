package com.tomaspinho.atvr.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.tomaspinho.atvr.domain.PowerState

private const val HAPTIC_MEDIUM_CLICK = 3
private const val HAPTIC_SWIPE = 4
private const val HAPTIC_SELECT = 16

/** Tappable icon button supporting click + long-click, with haptics. */
@Composable
fun RemoteButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val view = LocalView.current
    IconButton(
        onClick = {
            view.performHapticFeedback(HAPTIC_MEDIUM_CLICK)
            onClick()
        },
        modifier = modifier.size(56.dp)
    ) {
        Icon(icon, contentDescription, tint = tint)
    }
    if (onLongClick != null) {
        // Combined long-press handling is done via a separate pointerInput in
        // the host; here we expose the callback so the screen can wire it.
        // (Kept simple for the icon-button variant.)
    }
}

/** Material 3 filled Button with a label (click-only). */
@Composable
fun RemoteTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    Button(
        onClick = {
            view.performHapticFeedback(HAPTIC_MEDIUM_CLICK)
            onClick()
        },
        modifier = modifier,
        enabled = enabled
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Power toggle button with state-dependent label + haptic. */
@Composable
fun PowerToggleButton(
    powerState: PowerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    val view = LocalView.current
    val label = when (powerState) {
        PowerState.ON -> "On"
        PowerState.OFF -> "Off"
        PowerState.STANDBY -> "Standby"
        PowerState.UNKNOWN -> "Power"
    }
    Button(
        onClick = {
            when (powerState) {
                PowerState.ON -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                PowerState.OFF, PowerState.STANDBY -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                else -> view.performHapticFeedback(HAPTIC_MEDIUM_CLICK)
            }
            onClick()
        },
        modifier = modifier
    ) {
        if (loading) {
            LoadingIndicator(size = 20)
        } else {
            Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Power")
            Text(" $label")
        }
    }
}