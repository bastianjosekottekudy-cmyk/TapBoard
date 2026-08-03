package com.tapboard.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tapboard.app.BuildConfig
import com.tapboard.app.ui.TapBoardViewModel

@Composable
fun SettingsScreen(viewModel: TapBoardViewModel, onOpenHelp: () -> Unit) {
    val sensitivity by viewModel.sensitivity.collectAsState()
    val invert by viewModel.invertScroll.collectAsState()
    val dark by viewModel.darkTheme.collectAsState()
    val haptics by viewModel.haptics.collectAsState()
    val keepOn by viewModel.keepScreenOn.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Pointer sensitivity")
        Slider(value = sensitivity, onValueChange = viewModel::setSensitivity, valueRange = 0.3f..3f)
        Text("%.1fx".format(sensitivity), color = MaterialTheme.colorScheme.onSurfaceVariant)

        SettingSwitch("Invert scroll", invert, viewModel::setInvertScroll)
        SettingSwitch("Dark theme", dark, viewModel::setDarkTheme)
        SettingSwitch("Haptic feedback", haptics, viewModel::setHaptics)
        SettingSwitch("Keep screen on (touchpad)", keepOn, viewModel::setKeepScreenOn)

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onOpenHelp) { Text("Help & pairing guide") }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "TapBoard ${BuildConfig.VERSION_NAME}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "No accounts · no cloud telemetry",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
