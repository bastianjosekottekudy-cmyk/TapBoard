package com.tapboard.app.wifi

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

object Protocol {
    const val VERSION = 1
    const val DISCOVERY_PORT = 19528
    const val SESSION_PORT = 19529
    const val DISCOVER_MAGIC = "TAPBOARD_DISCOVER"
}

class FrameCodec {
    fun encode(json: JSONObject): ByteArray = encode(json.toString())

    fun encode(payload: String): ByteArray {
        val bytes = payload.toByteArray(StandardCharsets.UTF_8)
        return ByteBuffer.allocate(4 + bytes.size)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(bytes.size)
            .put(bytes)
            .array()
    }

    fun readFrameBytes(input: DataInputStream): ByteArray {
        val len = input.readInt()
        if (len <= 0 || len > 1_000_000) throw IOException("Invalid frame length: $len")
        val buf = ByteArray(len)
        input.readFully(buf)
        return buf
    }

    fun readFrame(input: DataInputStream): JSONObject {
        return JSONObject(String(readFrameBytes(input), StandardCharsets.UTF_8))
    }
}

class WifiSession(
    private val socket: Socket,
    private val codec: FrameCodec = FrameCodec()
) {
    private val input = DataInputStream(socket.getInputStream())
    private val output = DataOutputStream(socket.getOutputStream())
    private val writeLock = Any()

    fun send(json: JSONObject) {
        synchronized(writeLock) {
            output.write(codec.encode(json))
            output.flush()
        }
    }

    fun read(): JSONObject = codec.readFrame(input)

    fun close() {
        runCatching { socket.close() }
    }

    val isConnected: Boolean get() = socket.isConnected && !socket.isClosed

    companion object {
        fun connect(host: String, port: Int, timeoutMs: Int = 5000): WifiSession {
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            return WifiSession(socket)
        }
    }
}
