package com.tomaspinho.atvr.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.tomaspinho.atvr.ui.theme.RemoteShapes

private const val SWIPE_THRESHOLD = 60.0f
private const val HAPTIC_SWIPE = 4
private const val HAPTIC_SELECT = 16

/**
 * Blank touch surface. Tap / long-press -> select (press / hold). Drag
 * accumulates deltas; when a principal axis exceeds 60px, emit the matching
 * direction command (always "press") and reset accumulators.
 */
@Composable
fun Touchpad(
    onSelect: () -> Unit,
    onLongSelect: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var accumulatedX by remember { mutableStateOf(0.0f) }
    var accumulatedY by remember { mutableStateOf(0.0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(4.dp, RemoteShapes.Large)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                ),
                shape = RemoteShapes.Large
            )
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RemoteShapes.Large)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        view.performHapticFeedback(HAPTIC_SELECT)
                        onSelect()
                    },
                    onLongPress = {
                        view.performHapticFeedback(HAPTIC_SELECT)
                        onLongSelect()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, delta ->
                        change.consume()
                        accumulatedX += delta.x
                        accumulatedY += delta.y
                        when {
                            accumulatedX >= SWIPE_THRESHOLD -> {
                                view.performHapticFeedback(HAPTIC_SWIPE)
                                onRight()
                                accumulatedX = 0f; accumulatedY = 0f
                            }
                            accumulatedX <= -SWIPE_THRESHOLD -> {
                                view.performHapticFeedback(HAPTIC_SWIPE)
                                onLeft()
                                accumulatedX = 0f; accumulatedY = 0f
                            }
                            accumulatedY >= SWIPE_THRESHOLD -> {
                                view.performHapticFeedback(HAPTIC_SWIPE)
                                onDown()
                                accumulatedX = 0f; accumulatedY = 0f
                            }
                            accumulatedY <= -SWIPE_THRESHOLD -> {
                                view.performHapticFeedback(HAPTIC_SWIPE)
                                onUp()
                                accumulatedX = 0f; accumulatedY = 0f
                            }
                        }
                    }
                )
            }
    )
}