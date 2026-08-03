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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.tapboard.app.TapBoardLinks
import com.tapboard.app.connection.ConnectionMode
import com.tapboard.app.connection.ConnectionState
import com.tapboard.app.connection.DiscoveredWifiHost
import com.tapboard.app.ui.TapBoardViewModel
import com.tapboard.app.ui.permissions.RequestPermissions
import com.tapboard.app.ui.permissions.bluetoothPermissions
import com.tapboard.app.ui.permissions.wifiPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    viewModel: TapBoardViewModel,
    onOpenHelp: () -> Unit
) {
    val context = LocalContext.current
    val mode by viewModel.mode.collectAsState()
    val state by viewModel.state.collectAsState()
    val wifiHosts by viewModel.wifiHosts.collectAsState()
    val btDevices by viewModel.btDevices.collectAsState()
    val scanning by viewModel.scanning.collectAsState()
    val savedPin by viewModel.wifiPin.collectAsState()

    var requestBt by remember { mutableStateOf(false) }
    var requestWifi by remember { mutableStateOf(false) }
    var pinHost by remember { mutableStateOf<DiscoveredWifiHost?>(null) }
    var pinText by remember { mutableStateOf(savedPin) }

    LaunchedEffect(savedPin) { if (pinText.isBlank()) pinText = savedPin }

    if (requestBt) {
        RequestPermissions(bluetoothPermissions()) {
            requestBt = false
            if (it) viewModel.refreshBluetooth()
        }
    }
    if (requestWifi) {
        RequestPermissions(wifiPermissions()) {
            requestWifi = false
            if (it) viewModel.discoverWifi()
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
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == ConnectionMode.Wifi,
                onClick = { viewModel.setMode(ConnectionMode.Wifi) },
                label = { Text("Wi‑Fi") },
                leadingIcon = { Icon(Icons.Outlined.Wifi, null) }
            )
            FilterChip(
                selected = mode == ConnectionMode.Bluetooth,
                onClick = { viewModel.setMode(ConnectionMode.Bluetooth) },
                label = { Text("Bluetooth") },
                leadingIcon = { Icon(Icons.Outlined.Bluetooth, null) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (mode) {
            ConnectionMode.Wifi -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Companions on LAN", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { requestWifi = true }) {
                        if (scanning) CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        ) else Text("Scan")
                    }
                }
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    item {
                        ManualConnectCard(
                            defaultPin = savedPin,
                            onConnect = { ip, pin ->
                                viewModel.connectWifiManual(ip, pin)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (wifiHosts.isEmpty() && !scanning) {
                        item {
                            Column {
                                Text(
                                    "Scan finds PCs on the same Wi‑Fi. If nothing appears, use the IP shown in TapBoard Companion (manual connect above). Also tap Enable network access in the companion once.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(TapBoardLinks.COMPANION_WINDOWS_EXE)
                                            )
                                        )
                                    }
                                ) { Text("Download Windows Companion") }
                            }
                        }
                    }
                    items(wifiHosts) { host ->
                        HostRow(
                            title = host.name,
                            subtitle = "${host.host}:${host.port}",
                            onClick = {
                                pinText = savedPin
                                pinHost = host
                            }
                        )
                    }
                }
            }
            ConnectionMode.Bluetooth -> {
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
    }

    pinHost?.let { host ->
        AlertDialog(
            onDismissRequest = { pinHost = null },
            title = { Text("Enter PIN") },
            text = {
                Column {
                    Text("PIN shown by TapBoard Companion on ${host.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { pinText = it.filter(Char::isDigit).take(6) },
                        label = { Text("6-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.connectWifi(host, pinText)
                        pinHost = null
                    },
                    enabled = pinText.length == 6 || !host.pinRequired
                ) { Text("Connect") }
            },
            dismissButton = {
                TextButton(onClick = { pinHost = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ManualConnectCard(
    defaultPin: String,
    onConnect: (ip: String, pin: String) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf(defaultPin) }
    LaunchedEffect(defaultPin) {
        if (pin.isBlank()) pin = defaultPin
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(16.dp)
    ) {
        Text("Connect by IP", style = MaterialTheme.typography.titleLarge)
        Text(
            "Use the LAN IP shown in TapBoard Companion if Scan finds nothing.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it.trim() },
            label = { Text("PC IP (e.g. 192.168.0.88)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6) },
            label = { Text("6-digit PIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onConnect(ip, pin) },
            enabled = ip.isNotBlank() && pin.length == 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
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
                Text(state.mode.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            }
            is ConnectionState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                if (state.suggestWifi) {
                    Text(
                        "Tip: switch to Wi‑Fi mode and use TapBoard Companion.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
