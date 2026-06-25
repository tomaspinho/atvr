package com.tomaspinho.atvr.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Orange->black gradient button used on onboarding and primary CTAs. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val brush = Brush.horizontalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surface)
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .background(brush, MaterialTheme.shapes.large)
                .padding(vertical = 2.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Int = 48,
    strokeWidth: Int = 4
) {
    CircularProgressIndicator(
        modifier = modifier.size(size.dp),
        strokeWidth = strokeWidth.dp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color, CircleShape)
            .padding(2.dp)
    )
}