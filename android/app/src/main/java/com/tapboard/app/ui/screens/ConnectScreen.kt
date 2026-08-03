package com.tapboard.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tapboard.app.connection.ConnectionState
import com.tapboard.app.ui.TapBoardViewModel
import com.tapboard.app.ui.permissions.RequestPermissions
import com.tapboard.app.ui.permissions.bluetoothPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    viewModel: TapBoardViewModel,
    onOpenHelp: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val btDevices by viewModel.btDevices.collectAsState()

    var requestBt by remember { mutableStateOf(false) }

    if (requestBt) {
        RequestPermissions(bluetoothPermissions()) {
            requestBt = false
            if (it) viewModel.refreshBluetooth()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        TopAppBar(
            title = { Text("TapBoard") },
            actions = {
                IconButton(onClick = onOpenHelp) {
                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Help")
                }
            }
        )

        StatusBanner(state = state, onDisconnect = viewModel::disconnect, onClear = viewModel::clearError)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Paired devices", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { requestBt = true }) { Text("Refresh") }
        }
        Text(
            "Pair your PC in Android Bluetooth settings first, then connect here.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(btDevices) { device ->
                HostRow(
                    title = device.name,
                    subtitle = device.address,
                    onClick = { viewModel.connectBluetooth(device) }
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(
    state: ConnectionState,
    onDisconnect: () -> Unit,
    onClear: () -> Unit
) {
    val color by animateColorAsState(
        when (state) {
            is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
            is ConnectionState.Connecting -> MaterialTheme.colorScheme.surfaceVariant
            is ConnectionState.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "status"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(16.dp)
    ) {
        when (state) {
            ConnectionState.Disconnected -> Text("Not connected")
            is ConnectionState.Connecting -> Text("Connecting to ${state.targetName}…")
            is ConnectionState.Connected -> {
                Text("Connected to ${state.targetName}", style = MaterialTheme.typography.titleLarge)
                Text("Bluetooth", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            }
            is ConnectionState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onClear) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun HostRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
