package com.tapboard.app.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.tapboard.app.connection.ConnectionState
import com.tapboard.app.input.InputController
import com.tapboard.app.ui.TapBoardViewModel
import kotlin.math.abs

@Composable
fun TouchpadScreen(viewModel: TapBoardViewModel) {
    val state by viewModel.state.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()
    val haptics by viewModel.haptics.collectAsState()
    val keepOn by viewModel.keepScreenOn.collectAsState()
    val connected = state is ConnectionState.Connected
    val context = LocalContext.current
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "pad")

    DisposableEffect(keepOn) {
        val window = (view.context as? android.app.Activity)?.window
        if (keepOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    fun haptic() {
        if (!haptics) return
        val vibrator = context.getSystemService<Vibrator>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(18)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Touchpad", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (connected) "Drag to move · tap click · two-finger scroll · long-press right-click"
            else "Connect first from the Connect tab",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .pointerInput(connected) {
                    if (!connected) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        var last = down.position
                        var moved = false
                        var twoFinger = false
                        val downTime = System.currentTimeMillis()
                        var scrollAcc = 0f
                        do {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.filter { it.pressed }
                            if (pressedPointers.size >= 2) {
                                twoFinger = true
                                val dy = pressedPointers.take(2).map { it.positionChange().y }.average().toFloat()
                                scrollAcc += dy
                                if (abs(scrollAcc) > 12f) {
                                    viewModel.input.scroll(scrollAcc / 40f)
                                    scrollAcc = 0f
                                    moved = true
                                }
                                pressedPointers.forEach { it.consume() }
                            } else if (pressedPointers.size == 1) {
                                val p = pressedPointers.first()
                                val delta = p.position - last
                                if (abs(delta.x) > 0.5f || abs(delta.y) > 0.5f) {
                                    viewModel.input.move(delta.x, delta.y)
                                    moved = true
                                }
                                last = p.position
                                p.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        pressed = false
                        val duration = System.currentTimeMillis() - downTime
                        if (!moved && !twoFinger) {
                            if (duration >= 450) {
                                viewModel.input.click(InputController.BUTTON_RIGHT)
                            } else {
                                viewModel.input.click(InputController.BUTTON_LEFT)
                            }
                            haptic()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (connected) "Trackpad surface" else "Offline",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Sensitivity")
        Slider(
            value = sensitivity,
            onValueChange = viewModel::setSensitivity,
            valueRange = 0.3f..3f
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.input.click(InputController.BUTTON_LEFT); haptic() },
                enabled = connected,
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text("Left") }
            OutlinedButton(
                onClick = { viewModel.input.click(InputController.BUTTON_RIGHT); haptic() },
                enabled = connected,
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text("Right") }
            OutlinedButton(
                onClick = { viewModel.input.click(InputController.BUTTON_MIDDLE); haptic() },
                enabled = connected,
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text("Middle") }
        }
    }
}
