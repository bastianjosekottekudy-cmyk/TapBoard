package com.tapboard.app.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.hypot

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
            if (connected) {
                "Move · tap click · hold then drag · two-finger scroll · two-finger tap = right-click"
            } else {
                "Connect first from the Connect tab"
            },
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
                        var scrolled = false
                        var twoFinger = false
                        var dragging = false
                        var wasDragging = false
                        var travel = 0f
                        val downTime = System.currentTimeMillis()
                        val scrollPixelsPerNotch = 18f
                        val dragHoldMs = 320L
                        val dragSlopPx = 28f

                        fun endDragIfNeeded() {
                            if (dragging) {
                                viewModel.input.setButton(InputController.BUTTON_LEFT, false)
                                dragging = false
                                wasDragging = true
                            }
                        }

                        try {
                            do {
                                val event = awaitPointerEvent()
                                val pressedPointers = event.changes.filter { it.pressed }
                                val now = System.currentTimeMillis()

                                if (pressedPointers.size >= 2) {
                                    twoFinger = true
                                    endDragIfNeeded()
                                    val dy = pressedPointers
                                        .take(2)
                                        .map { it.positionChange().y }
                                        .average()
                                        .toFloat()
                                    if (abs(dy) > 0.2f) {
                                        viewModel.input.scroll(dy / scrollPixelsPerNotch)
                                        scrolled = true
                                        moved = true
                                    }
                                    pressedPointers.forEach { it.consume() }
                                } else if (pressedPointers.size == 1 && !twoFinger) {
                                    val p = pressedPointers.first()
                                    val delta = p.position - last
                                    val step = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                                    travel += step

                                    // Hold still briefly → left button down (click-and-drag).
                                    if (!dragging &&
                                        travel < dragSlopPx &&
                                        now - downTime >= dragHoldMs
                                    ) {
                                        dragging = true
                                        viewModel.input.setButton(InputController.BUTTON_LEFT, true)
                                        haptic()
                                    }

                                    if (abs(delta.x) > 0.5f || abs(delta.y) > 0.5f) {
                                        viewModel.input.move(delta.x, delta.y)
                                        if (dragging || travel >= dragSlopPx) {
                                            moved = true
                                        }
                                    }
                                    last = p.position
                                    p.consume()
                                } else if (pressedPointers.size == 1) {
                                    pressedPointers.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        } finally {
                            endDragIfNeeded()
                            pressed = false
                        }

                        val duration = System.currentTimeMillis() - downTime
                        when {
                            // Short tap → left click (hold-drag already sent down/up).
                            !moved && !twoFinger && !wasDragging && duration < dragHoldMs -> {
                                viewModel.input.click(InputController.BUTTON_LEFT)
                                haptic()
                            }
                            // Two-finger tap → right click
                            twoFinger && !scrolled && duration < 450 -> {
                                viewModel.input.click(InputController.BUTTON_RIGHT)
                                haptic()
                            }
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
        Text(
            "Hold Left/Right/Middle to keep the button down while you drag on the pad.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HoldMouseButton(
                label = "Left",
                mask = InputController.BUTTON_LEFT,
                enabled = connected,
                outlined = false,
                input = viewModel.input,
                onHaptic = { haptic() },
                modifier = Modifier.weight(1f).height(52.dp)
            )
            HoldMouseButton(
                label = "Right",
                mask = InputController.BUTTON_RIGHT,
                enabled = connected,
                outlined = true,
                input = viewModel.input,
                onHaptic = { haptic() },
                modifier = Modifier.weight(1f).height(52.dp)
            )
            HoldMouseButton(
                label = "Middle",
                mask = InputController.BUTTON_MIDDLE,
                enabled = connected,
                outlined = true,
                input = viewModel.input,
                onHaptic = { haptic() },
                modifier = Modifier.weight(1f).height(52.dp)
            )
        }
    }
}

/**
 * Press = mouse button down, release = up — so you can hold Left and drag on the pad.
 */
@Composable
private fun HoldMouseButton(
    label: String,
    mask: Int,
    enabled: Boolean,
    outlined: Boolean,
    input: InputController,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        outlined -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.primary
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        outlined -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .then(
                if (outlined) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                } else {
                    Modifier
                }
            )
            .pointerInput(enabled, mask) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    input.setButton(mask, true)
                    onHaptic()
                    try {
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        input.setButton(mask, false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}
