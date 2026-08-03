package com.tapboard.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tapboard.app.TapBoardLinks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Help") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Section(
                "Bluetooth pairing",
                "1. On your PC/TV, open Bluetooth settings and stay discoverable.\n" +
                    "2. On the phone, pair the host in Android Bluetooth settings.\n" +
                    "3. In TapBoard → Bluetooth → Refresh → Connect.\n" +
                    "4. Accept the HID connection on the host if prompted."
            )
            Section(
                "If Bluetooth HID fails",
                "Some phone makers limit acting as a Bluetooth keyboard. TapBoard will show an error — switch to Wi‑Fi mode and use the companion. This is normal on many OEM stacks."
            )
            Section(
                "Wi‑Fi companion",
                "1. Download TapBoard Companion for Windows from GitHub Releases.\n" +
                    "2. Run it, tap “Enable network access” once, approve the Windows prompt.\n" +
                    "3. Phone and PC must share the same LAN.\n" +
                    "4. In TapBoard → Wi‑Fi → Scan → enter the PIN shown in the companion."
            )
            Button(
                onClick = { open(TapBoardLinks.COMPANION_WINDOWS_EXE) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download Windows Companion")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { open(TapBoardLinks.COMPANION_RELEASES) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open GitHub Releases")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Section(
                "Privacy",
                "TapBoard does not create accounts or upload your input. Bluetooth identifiers and LAN traffic stay on your devices."
            )
            OutlinedButton(
                onClick = { open(TapBoardLinks.PRIVACY_POLICY) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Privacy policy")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Section(
                "Gestures",
                "One finger move · tap = left click · long-press = right click · two-finger vertical drag = scroll."
            )
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(6.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(12.dp))
}
