package com.tomaspinho.atvr.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tomaspinho.atvr.domain.MediaInfo

/**
 * Now-playing card: artwork thumbnail (or music-note placeholder), title,
 * artist/album/app subtitle, thin progress bar. Purely display -- data flows
 * from push updates and pull refreshes.
 */
@Composable
fun NowPlayingCard(
    mediaInfo: MediaInfo?,
    modifier: Modifier = Modifier
) {
    if (mediaInfo == null) return
    Card(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .aspectRatio(1f)
            ) {
                val art = mediaInfo.artwork
                if (!art.isNullOrEmpty()) {
                    val dataUri = remember(art) { "data:image/jpeg;base64,$art" }
                    AsyncImage(
                        model = dataUri,
                        contentDescription = "Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = mediaInfo.title ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val subtitle = listOfNotNull(mediaInfo.artist, mediaInfo.album, mediaInfo.app)
                    .joinToString(" · ")
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val position = mediaInfo.position?.toFloat() ?: 0f
        val total = mediaInfo.totalTime?.toFloat() ?: 0f
        val progress = if (total > 0f) (position / total).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
    }
}