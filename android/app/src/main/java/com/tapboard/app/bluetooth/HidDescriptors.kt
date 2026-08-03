package com.tapboard.app.bluetooth

/**
 * Combo keyboard (ID 1) + mouse (ID 2) HID report map for BluetoothHidDevice.
 */
object HidDescriptors {
    const val ID_KEYBOARD = 1
    const val ID_MOUSE = 2

    val DESCRIPTOR: ByteArray = byteArrayOf(
        // Keyboard
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x06, // Usage (Keyboard)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), ID_KEYBOARD.toByte(), // Report ID
        0x05, 0x07, // Usage Page (Key Codes)
        0x19, 0xE0.toByte(), // Usage Minimum
        0x29, 0xE7.toByte(), // Usage Maximum
        0x15, 0x00, // Logical Minimum
        0x25, 0x01, // Logical Maximum
        0x75, 0x01, // Report Size
        0x95.toByte(), 0x08, // Report Count
        0x81.toByte(), 0x02, // Input (Data, Var, Abs) — modifiers
        0x95.toByte(), 0x01, // Report Count
        0x75, 0x08, // Report Size
        0x81.toByte(), 0x01, // Input (Const) — reserved
        0x95.toByte(), 0x05, // LEDs
        0x75, 0x01,
        0x05, 0x08,
        0x19, 0x01,
        0x29, 0x05,
        0x91.toByte(), 0x02, // Output
        0x95.toByte(), 0x01,
        0x75, 0x03,
        0x91.toByte(), 0x01, // Padding
        0x95.toByte(), 0x06, // 6 keys
        0x75, 0x08,
        0x15, 0x00,
        0x25, 0x65,
        0x05, 0x07,
        0x19, 0x00,
        0x29, 0x65,
        0x81.toByte(), 0x00, // Input (Data, Array)
        0xC0.toByte(), // End Collection

        // Mouse
        0x05, 0x01,
        0x09, 0x02,
        0xA1.toByte(), 0x01,
        0x85.toByte(), ID_MOUSE.toByte(),
        0x09, 0x01,
        0xA1.toByte(), 0x00,
        0x05, 0x09,
        0x19, 0x01,
        0x29, 0x03,
        0x15, 0x00,
        0x25, 0x01,
        0x95.toByte(), 0x03,
        0x75, 0x01,
        0x81.toByte(), 0x02, // Buttons
        0x95.toByte(), 0x01,
        0x75, 0x05,
        0x81.toByte(), 0x01, // Padding
        0x05, 0x01,
        0x09, 0x30,
        0x09, 0x31,
        0x09, 0x38,
        0x15, 0x81.toByte(), // -127
        0x25, 0x7F,
        0x75, 0x08,
        0x95.toByte(), 0x03,
        0x81.toByte(), 0x06, // X, Y, Wheel (Rel)
        0xC0.toByte(),
        0xC0.toByte()
    )

    fun keyboardReport(modifier: Int, keyCodes: IntArray): ByteArray {
        val report = ByteArray(8)
        report[0] = modifier.toByte()
        report[1] = 0
        for (i in 0 until minOf(6, keyCodes.size)) {
            report[2 + i] = keyCodes[i].toByte()
        }
        return report
    }

    fun mouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int): ByteArray {
        return byteArrayOf(
            (buttons and 0x07).toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte()
        )
    }
}
