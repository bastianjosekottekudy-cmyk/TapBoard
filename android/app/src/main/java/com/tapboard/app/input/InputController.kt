package com.tapboard.app.input

import com.tapboard.app.bluetooth.BluetoothHidManager
import com.tapboard.app.connection.ConnectionMode
import com.tapboard.app.connection.ConnectionState
import com.tapboard.app.wifi.WifiClient
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared input path so Bluetooth HID and Wi‑Fi feel identical.
 */
class InputController(
    private val state: StateFlow<ConnectionState>,
    private val bluetooth: BluetoothHidManager,
    private val wifi: WifiClient,
    private val sensitivityProvider: () -> Float,
    private val invertScrollProvider: () -> Boolean
) {
    @Volatile private var mouseButtons: Int = 0
    @Volatile private var modifierMask: Int = 0
    private val pressedKeys = LinkedHashSet<Int>()

    private val activeMode: ConnectionMode?
        get() = (state.value as? ConnectionState.Connected)?.mode

    fun move(dx: Float, dy: Float) {
        val s = sensitivityProvider()
        var rdx = (dx * s).toInt()
        var rdy = (dy * s).toInt()
        if (rdx == 0 && rdy == 0 && (dx != 0f || dy != 0f)) {
            rdx = if (dx < 0) -1 else if (dx > 0) 1 else 0
            rdy = if (dy < 0) -1 else if (dy > 0) 1 else 0
        }
        when (activeMode) {
            ConnectionMode.Bluetooth -> {
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
            ConnectionMode.Wifi -> wifi.sendMouse(rdx, rdy, mouseButtons, 0, 0)
            null -> Unit
        }
    }

    fun scroll(vertical: Float, horizontal: Float = 0f) {
        val invert = if (invertScrollProvider()) -1 else 1
        val wheel = (vertical * invert).toInt().coerceIn(-15, 15)
        val hwheel = (horizontal * invert).toInt().coerceIn(-15, 15)
        if (wheel == 0 && hwheel == 0) return
        when (activeMode) {
            ConnectionMode.Bluetooth -> bluetooth.sendMouse(mouseButtons, 0, 0, wheel)
            ConnectionMode.Wifi -> wifi.sendMouse(0, 0, mouseButtons, wheel, hwheel)
            null -> Unit
        }
    }

    fun setButton(mask: Int, down: Boolean) {
        mouseButtons = if (down) mouseButtons or mask else mouseButtons and mask.inv()
        when (activeMode) {
            ConnectionMode.Bluetooth -> bluetooth.sendMouse(mouseButtons, 0, 0, 0)
            ConnectionMode.Wifi -> wifi.sendMouse(0, 0, mouseButtons, 0, 0)
            null -> Unit
        }
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
        flushKeyboard(wifiDown = true, hid = hid)
    }

    fun keyUp(hid: Int) {
        pressedKeys.remove(hid)
        flushKeyboard(wifiDown = false, hid = hid)
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

    private fun flushKeyboard(wifiDown: Boolean? = null, hid: Int? = null) {
        when (activeMode) {
            ConnectionMode.Bluetooth -> {
                val keys = pressedKeys.take(6).toIntArray()
                bluetooth.sendKeyboard(modifierMask, *keys)
            }
            ConnectionMode.Wifi -> {
                if (hid != null && wifiDown != null) {
                    wifi.sendKey(hid, modifierMask, wifiDown)
                }
            }
            null -> Unit
        }
    }

    companion object {
        const val BUTTON_LEFT = 0x01
        const val BUTTON_RIGHT = 0x02
        const val BUTTON_MIDDLE = 0x04
    }
}
