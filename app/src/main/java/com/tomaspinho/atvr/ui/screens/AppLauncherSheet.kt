package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomaspinho.atvr.domain.AppInfo
import com.tomaspinho.atvr.ui.components.LoadingIndicator

private val builtInIcons: Map<String, androidx.compose.ui.graphics.vector.ImageVector> = mapOf(
    "com.apple.Arcade" to Icons.Filled.SportsEsports,
    "com.apple.TVMusic" to Icons.Filled.MusicNote,
    "com.apple.TVShows" to Icons.Filled.Tv,
    "com.apple.Fitness" to Icons.Filled.FitnessCenter,
    "com.apple.TVAppStore" to Icons.Filled.ShoppingBag,
    "com.apple.TVSettings" to Icons.Filled.Settings,
    "com.apple.podcasts" to Icons.Filled.Podcasts,
    "com.apple.facetime" to Icons.Filled.Videocam
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLauncherSheet(
    visible: Boolean,
    apps: List<AppInfo>,
    isLoading: Boolean,
    onAppSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState()
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(24.dp)) {
            Text("Apps", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(16.dp))
            when {
                isLoading -> Box(Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator(size = 48)
                }
                apps.isEmpty() -> Box(Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No apps found")
                }
                else -> {
                    val filtered = apps.filter { it.name.contains(query, ignoreCase = true) }
                    if (filtered.isEmpty()) {
                        Box(Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No matching apps")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(88.dp),
                            modifier = Modifier.fillMaxHeight(0.7f)
                        ) {
                            items(filtered, key = { it.bundleId }) { app ->
                                AppItem(app = app, onClick = { onAppSelected(app.bundleId, app.name) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItem(app: AppInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val icon = builtInIcons[app.bundleId]
            if (icon != null) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = app.name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            } else if (app.iconUrl != null) {
                coil.compose.AsyncImage(
                    model = app.iconUrl,
                    contentDescription = app.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Apps, contentDescription = app.name, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                }
            }
        }
        Spacer(Modifier.size(8.dp))
        Text(app.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}