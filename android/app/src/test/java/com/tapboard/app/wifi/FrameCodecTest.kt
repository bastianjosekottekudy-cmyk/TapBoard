package com.tapboard.app.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class FrameCodecTest {
    @Test
    fun encodeAndDecodeRoundTrip() {
        val codec = FrameCodec()
        val payload = """{"v":1,"type":"ping","t":42}"""
        val bytes = codec.encode(payload)
        val len = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.BIG_ENDIAN).int
        assertEquals(bytes.size - 4, len)
        val decoded = codec.readFrameBytes(DataInputStream(ByteArrayInputStream(bytes)))
        assertEquals(payload, String(decoded, StandardCharsets.UTF_8))
    }

    @Test
    fun protocolConstants() {
        assertEquals(1, Protocol.VERSION)
        assertEquals(19528, Protocol.DISCOVERY_PORT)
        assertEquals(19529, Protocol.SESSION_PORT)
        assertTrue(Protocol.DISCOVER_MAGIC.startsWith("TAPBOARD"))
    }
}
