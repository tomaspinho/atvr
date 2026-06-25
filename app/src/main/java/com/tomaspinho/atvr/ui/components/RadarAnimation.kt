package com.tomaspinho.atvr.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * Animated radar sweep used on the onboarding scan step while scanning.
 * Purely visual; no pyatv interaction.
 */
@Composable
fun RadarAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.size(size).rotate(angle)) {
        val radius = this.size.minDimension / 2
        val center = Offset(radius, radius)
        drawCircle(color = primary.copy(alpha = 0.15f), radius = radius, style = Stroke(width = 2f))
        drawCircle(color = primary.copy(alpha = 0.10f), radius = radius * 0.66f, style = Stroke(width = 2f))
        drawCircle(color = primary.copy(alpha = 0.10f), radius = radius * 0.33f, style = Stroke(width = 2f))
        val sweepBrush = Brush.sweepGradient(
            colors = listOf(primary.copy(alpha = 0.6f), Color.Transparent)
        )
        drawCircle(brush = sweepBrush, radius = radius, center = center)
    }
}