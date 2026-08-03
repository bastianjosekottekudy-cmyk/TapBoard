package com.tapboard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapboard.app.connection.ConnectionState
import com.tapboard.app.input.HidKeyCodes
import com.tapboard.app.ui.TapBoardViewModel

@Composable
fun KeyboardScreen(viewModel: TapBoardViewModel) {
    val state by viewModel.state.collectAsState()
    val connected = state is ConnectionState.Connected
    var text by remember { mutableStateOf("") }
    var mods by remember { mutableIntStateOf(0) }
    var showFKeys by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    fun toggleMod(mask: Int) {
        val down = mods and mask == 0
        mods = if (down) mods or mask else mods and mask.inv()
        viewModel.input.setModifier(mask, down)
    }

    fun send(hid: Int) {
        if (!connected) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        viewModel.input.tapKey(hid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Text("Keyboard", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (connected) "Tap keys or type in the field" else "Connect first",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                if (new.length > text.length) {
                    new.substring(text.length).forEach { viewModel.input.typeChar(it) }
                } else if (new.length < text.length) {
                    repeat(text.length - new.length) {
                        viewModel.input.tapKey(HidKeyCodes.BACKSPACE)
                    }
                }
                text = new
            },
            enabled = connected,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Send text") },
            minLines = 2
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Ctrl" to HidKeyCodes.MOD_LCTRL,
                "Shift" to HidKeyCodes.MOD_LSHIFT,
                "Alt" to HidKeyCodes.MOD_LALT,
                "Win" to HidKeyCodes.MOD_LGUI
            ).forEach { (label, mask) ->
                FilterChip(
                    selected = mods and mask != 0,
                    onClick = { if (connected) toggleMod(mask) },
                    label = { Text(label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        SoftKeyboard(
            enabled = connected,
            shiftHeld = mods and HidKeyCodes.MOD_LSHIFT != 0,
            onKey = { send(it) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { showFKeys = !showFKeys }, enabled = connected) {
            Text(if (showFKeys) "Hide function keys" else "Function keys")
        }
        if (showFKeys) {
            FKeyRow(enabled = connected, onKey = { send(it) })
        }
    }
}

@Composable
private fun SoftKeyboard(
    enabled: Boolean,
    shiftHeld: Boolean,
    onKey: (Int) -> Unit
) {
    val gap = 5.dp
    val rowHeight = 48.dp
    val surface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(surface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Number row
        KeyRow(height = rowHeight, gap = gap) {
            "1234567890".forEach { ch ->
                LetterKey(
                    label = ch.toString(),
                    weight = 1f,
                    enabled = enabled,
                    onClick = { onKey(HidKeyCodes.fromChar(ch)!!.first) }
                )
            }
        }

        // QWERTY
        KeyRow(height = rowHeight, gap = gap) {
            "QWERTYUIOP".forEach { ch ->
                LetterKey(
                    label = if (shiftHeld) ch.toString() else ch.lowercaseChar().toString(),
                    weight = 1f,
                    enabled = enabled,
                    onClick = { onKey(HidKeyCodes.fromChar(ch.lowercaseChar())!!.first) }
                )
            }
        }

        // ASDF — inset like a real keyboard (~half key on each side)
        KeyRow(height = rowHeight, gap = gap) {
            Spacer(modifier = Modifier.weight(0.5f))
            "ASDFGHJKL".forEach { ch ->
                LetterKey(
                    label = if (shiftHeld) ch.toString() else ch.lowercaseChar().toString(),
                    weight = 1f,
                    enabled = enabled,
                    onClick = { onKey(HidKeyCodes.fromChar(ch.lowercaseChar())!!.first) }
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        // ZXCV — more inset + backspace
        KeyRow(height = rowHeight, gap = gap) {
            Spacer(modifier = Modifier.weight(0.15f))
            "ZXCVBNM".forEach { ch ->
                LetterKey(
                    label = if (shiftHeld) ch.toString() else ch.lowercaseChar().toString(),
                    weight = 1f,
                    enabled = enabled,
                    onClick = { onKey(HidKeyCodes.fromChar(ch.lowercaseChar())!!.first) }
                )
            }
            ActionKey(
                label = "⌫",
                weight = 1.6f,
                enabled = enabled,
                emphasis = true,
                onClick = { onKey(HidKeyCodes.BACKSPACE) }
            )
            Spacer(modifier = Modifier.weight(0.15f))
        }

        // Bottom action row
        KeyRow(height = rowHeight, gap = gap) {
            ActionKey("Tab", 1.2f, enabled) { onKey(HidKeyCodes.TAB) }
            ActionKey(",", 1f, enabled) { onKey(HidKeyCodes.COMMA) }
            ActionKey("Space", 4.2f, enabled) { onKey(HidKeyCodes.SPACE) }
            ActionKey(".", 1f, enabled) { onKey(HidKeyCodes.PERIOD) }
            ActionKey("Enter", 1.6f, enabled, emphasis = true) { onKey(HidKeyCodes.ENTER) }
            ActionKey("Esc", 1.1f, enabled) { onKey(HidKeyCodes.ESCAPE) }
        }
    }
}

@Composable
private fun FKeyRow(enabled: Boolean, onKey: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(1..6, 7..12).forEach { range ->
            KeyRow(height = 40.dp, gap = 4.dp) {
                range.forEach { n ->
                    ActionKey("F$n", 1f, enabled) { onKey(HidKeyCodes.F1 + (n - 1)) }
                }
            }
        }
    }
}

@Composable
private fun KeyRow(
    height: Dp,
    gap: Dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun RowScope.LetterKey(
    label: String,
    weight: Float,
    enabled: Boolean,
    onClick: () -> Unit
) {
    KeyCap(
        label = label,
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        enabled = enabled,
        emphasis = false,
        onClick = onClick
    )
}

@Composable
private fun RowScope.ActionKey(
    label: String,
    weight: Float,
    enabled: Boolean,
    emphasis: Boolean = false,
    onClick: () -> Unit
) {
    KeyCap(
        label = label,
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        enabled = enabled,
        emphasis = emphasis,
        onClick = onClick
    )
}

@Composable
private fun KeyCap(
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    emphasis: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        emphasis -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        emphasis -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = if (label.length <= 2) 16.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
