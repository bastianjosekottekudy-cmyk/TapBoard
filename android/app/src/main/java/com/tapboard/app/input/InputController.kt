package com.tapboard.app.input

import com.tapboard.app.bluetooth.BluetoothHidManager
import com.tapboard.app.connection.ConnectionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Bluetooth HID input path for touchpad and keyboard.
 */
class InputController(
    private val state: StateFlow<ConnectionState>,
    private val bluetooth: BluetoothHidManager,
    private val sensitivityProvider: () -> Float,
    private val invertScrollProvider: () -> Boolean
) {
    @Volatile private var mouseButtons: Int = 0
    @Volatile private var modifierMask: Int = 0
    private val pressedKeys = LinkedHashSet<Int>()

    private val isConnected: Boolean
        get() = state.value is ConnectionState.Connected

    fun move(dx: Float, dy: Float) {
        if (!isConnected) return
        val s = sensitivityProvider()
        var rdx = (dx * s).toInt()
        var rdy = (dy * s).toInt()
        if (rdx == 0 && rdy == 0 && (dx != 0f || dy != 0f)) {
            rdx = if (dx < 0) -1 else if (dx > 0) 1 else 0
            rdy = if (dy < 0) -1 else if (dy > 0) 1 else 0
        }
        // Chunk large moves into HID signed-byte range
        var remX = rdx
        var remY = rdy
        while (remX != 0 || remY != 0) {
            val sx = remX.coerceIn(-127, 127)
            val sy = remY.coerceIn(-127, 127)
            bluetooth.sendMouse(mouseButtons, sx, sy, 0)
            remX -= sx
            remY -= sy
        }
    }

    fun scroll(vertical: Float, horizontal: Float = 0f) {
        if (!isConnected) return
        val invert = if (invertScrollProvider()) -1 else 1
        val wheel = (vertical * invert).toInt().coerceIn(-15, 15)
        if (wheel == 0) return
        bluetooth.sendMouse(mouseButtons, 0, 0, wheel)
    }

    fun setButton(mask: Int, down: Boolean) {
        if (!isConnected) return
        mouseButtons = if (down) mouseButtons or mask else mouseButtons and mask.inv()
        bluetooth.sendMouse(mouseButtons, 0, 0, 0)
    }

    fun click(mask: Int = BUTTON_LEFT) {
        setButton(mask, true)
        setButton(mask, false)
    }

    fun setModifier(mask: Int, down: Boolean) {
        modifierMask = if (down) modifierMask or mask else modifierMask and mask.inv()
        flushKeyboard()
    }

    fun keyDown(hid: Int) {
        pressedKeys.add(hid)
        flushKeyboard()
    }

    fun keyUp(hid: Int) {
        pressedKeys.remove(hid)
        flushKeyboard()
    }

    fun tapKey(hid: Int, extraMods: Int = 0) {
        val previous = modifierMask
        modifierMask = previous or extraMods
        keyDown(hid)
        keyUp(hid)
        modifierMask = previous
        flushKeyboard()
    }

    fun typeChar(ch: Char) {
        val mapped = HidKeyCodes.fromChar(ch) ?: return
        tapKey(mapped.first, mapped.second)
    }

    fun typeText(text: String) {
        text.forEach { typeChar(it) }
    }

    private fun flushKeyboard() {
        if (!isConnected) return
        val keys = pressedKeys.take(6).toIntArray()
        bluetooth.sendKeyboard(modifierMask, *keys)
    }

    companion object {
        const val BUTTON_LEFT = 0x01
        const val BUTTON_RIGHT = 0x02
        const val BUTTON_MIDDLE = 0x04
    }
}
