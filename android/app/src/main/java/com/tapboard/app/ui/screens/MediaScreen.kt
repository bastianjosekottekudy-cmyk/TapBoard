package com.tapboard.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tapboard.app.connection.ConnectionState
import com.tapboard.app.input.HidKeyCodes
import com.tapboard.app.ui.TapBoardViewModel

@Composable
fun MediaScreen(viewModel: TapBoardViewModel) {
    val state by viewModel.state.collectAsState()
    val connected = state is ConnectionState.Connected
    fun key(hid: Int) = viewModel.input.tapKey(hid)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Media / Presenter", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
        Text(
            "Arrows for slides · Esc to exit · media transport",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // D-pad
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalButton(
                onClick = { key(HidKeyCodes.UP) },
                enabled = connected,
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Icon(Icons.Outlined.KeyboardArrowUp, "Up") }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { key(HidKeyCodes.LEFT) },
                    enabled = connected,
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(18.dp)
                ) { Icon(Icons.Outlined.KeyboardArrowLeft, "Left") }
                FilledTonalButton(
                    onClick = { key(HidKeyCodes.DOWN) },
                    enabled = connected,
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(18.dp)
                ) { Icon(Icons.Outlined.KeyboardArrowDown, "Down") }
                FilledTonalButton(
                    onClick = { key(HidKeyCodes.RIGHT) },
                    enabled = connected,
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(18.dp)
                ) { Icon(Icons.Outlined.KeyboardArrowRight, "Right") }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { key(HidKeyCodes.ESCAPE) }, enabled = connected, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Esc")
            }
            OutlinedButton(onClick = { key(HidKeyCodes.F5) }, enabled = connected, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("F5")
            }
            OutlinedButton(onClick = { key(HidKeyCodes.PAGE_DOWN) }, enabled = connected, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Next")
            }
            OutlinedButton(onClick = { key(HidKeyCodes.PAGE_UP) }, enabled = connected, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Prev")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text("Media", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { key(HidKeyCodes.MEDIA_PREV) }, enabled = connected, modifier = Modifier.weight(1f).height(56.dp)) {
                Icon(Icons.Outlined.SkipPrevious, null)
            }
            Button(onClick = { key(HidKeyCodes.MEDIA_PLAY_PAUSE) }, enabled = connected, modifier = Modifier.weight(1f).height(56.dp)) {
                Icon(Icons.Outlined.PlayArrow, null)
            }
            Button(onClick = { key(HidKeyCodes.MEDIA_NEXT) }, enabled = connected, modifier = Modifier.weight(1f).height(56.dp)) {
                Icon(Icons.Outlined.SkipNext, null)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { key(HidKeyCodes.VOLUME_DOWN) }, enabled = connected, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(Icons.Outlined.VolumeDown, null)
            }
            OutlinedButton(onClick = { key(HidKeyCodes.MUTE) }, enabled = connected, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(Icons.Outlined.VolumeOff, null)
            }
            OutlinedButton(onClick = { key(HidKeyCodes.VOLUME_UP) }, enabled = connected, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(Icons.Outlined.VolumeUp, null)
            }
        }
    }
}
