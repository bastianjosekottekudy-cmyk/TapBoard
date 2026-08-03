package com.tapboard.app.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HidKeyCodesTest {
    @Test
    fun mapsLettersWithShift() {
        val lower = HidKeyCodes.fromChar('a')
        assertNotNull(lower)
        assertEquals(HidKeyCodes.A, lower!!.first)
        assertEquals(0, lower.second)

        val upper = HidKeyCodes.fromChar('A')
        assertEquals(HidKeyCodes.A, upper!!.first)
        assertEquals(HidKeyCodes.MOD_LSHIFT, upper.second)
    }

    @Test
    fun mapsSymbols() {
        val q = HidKeyCodes.fromChar('?')
        assertEquals(HidKeyCodes.SLASH, q!!.first)
        assertEquals(HidKeyCodes.MOD_LSHIFT, q.second)
    }
}
