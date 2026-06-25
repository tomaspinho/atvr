package com.tomaspinho.atvr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Expandable troubleshooting card shown during onboarding scan (when no
 * devices found) and inside the PairingDialog (PIN help). Purely
 * informational; no pyatv interaction.
 *
 * @param title Card title
 * @param desc Optional supporting description
 * @param instructions Step list shown when expanded
 * @param onExpand Optional callback when expanded state toggles
 * @param onSupportClick Optional support-link button; null hides it
 */
@Composable
fun AirPlayHelpCard(
    title: String,
    desc: String? = null,
    instructions: List<String> = emptyList(),
    onExpand: ((Boolean) -> Unit)? = null,
    onSupportClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = !expanded
                        onExpand?.invoke(expanded)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (desc != null) {
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(Modifier.padding(top = 12.dp)) {
                    instructions.forEach { step ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("•", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.size(8.dp))
                            Text(step, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (onSupportClick != null) {
                        Spacer(Modifier.size(8.dp))
                        Row {
                            TextButton(onClick = onSupportClick) {
                                Text("AirPlay support link")
                            }
                        }
                    }
                }
            }
        }
    }
}