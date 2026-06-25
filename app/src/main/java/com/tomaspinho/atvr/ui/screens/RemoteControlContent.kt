package com.tomaspinho.atvr.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomaspinho.atvr.domain.MediaInfo
import com.tomaspinho.atvr.ui.components.NowPlayingCard
import com.tomaspinho.atvr.ui.components.RemoteButton
import com.tomaspinho.atvr.ui.components.Touchpad
import com.tomaspinho.atvr.viewmodel.remote.RemoteControlIntent

@Composable
fun RemoteControlContent(
    isConnected: Boolean,
    mediaInfo: MediaInfo?,
    showMediaPlayer: Boolean,
    touchpadAtBottom: Boolean,
    onCommand: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val touchpad: @Composable () -> Unit = {
        Touchpad(
            onSelect = { onCommand("select", "press") },
            onLongSelect = { onCommand("select", "hold") },
            onUp = { onCommand("up", "press") },
            onDown = { onCommand("down", "press") },
            onLeft = { onCommand("left", "press") },
            onRight = { onCommand("right", "press") }
        )
    }
    val controls: @Composable () -> Unit = {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RemoteButton(Icons.Filled.Menu, "Menu", onClick = { onCommand("menu", "press") })
                RemoteButton(Icons.Filled.PlayArrow, "Play/Pause", onClick = { onCommand("play", "press") })
                RemoteButton(Icons.Filled.Tv, "Home", onClick = { onCommand("home", "press") })
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RemoteButton(Icons.Filled.SkipPrevious, "Previous", onClick = { onCommand("previous", "press") })
                RemoteButton(Icons.Filled.Replay10, "Rewind", onClick = { onCommand("rewind", "press") })
                RemoteButton(Icons.Filled.Forward10, "Fast Forward", onClick = { onCommand("fast_forward", "press") })
                RemoteButton(Icons.Filled.SkipNext, "Next", onClick = { onCommand("next", "press") })
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteButton(Icons.Filled.Remove, "Volume Down", onClick = { onCommand("volume_down", "press") })
                    RemoteButton(Icons.Filled.VolumeOff, "Mute", onClick = { onCommand("mute", "press") })
                    RemoteButton(Icons.Filled.Add, "Volume Up", onClick = { onCommand("volume_up", "press") })
                }
            }
            if (showMediaPlayer && mediaInfo != null) {
                Spacer(Modifier.size(8.dp))
                NowPlayingCard(mediaInfo = mediaInfo)
            }
        }
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (touchpadAtBottom) {
            controls()
            Spacer(Modifier.size(12.dp))
            touchpad()
        } else {
            touchpad()
            Spacer(Modifier.size(12.dp))
            controls()
        }
    }
}