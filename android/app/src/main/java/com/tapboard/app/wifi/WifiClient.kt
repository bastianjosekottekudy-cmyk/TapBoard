package com.tapboard.app.wifi

import com.tapboard.app.BuildConfig
import com.tapboard.app.connection.DiscoveredWifiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicReference

class WifiClient {
    private val sessionRef = AtomicReference<WifiSession?>(null)

    val isConnected: Boolean get() = sessionRef.get()?.isConnected == true

    suspend fun discover(timeoutMs: Long = 2500L): List<DiscoveredWifiHost> = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, DiscoveredWifiHost>()
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.soTimeout = 400
            val payload = Protocol.DISCOVER_MAGIC.toByteArray()
            val broadcast = InetAddress.getByName("255.255.255.255")
            socket.send(DatagramPacket(payload, payload.size, broadcast, Protocol.DISCOVERY_PORT))
            // Also try subnet-local all-ones if available later; broadcast is enough for most LANs
            val deadline = System.currentTimeMillis() + timeoutMs
            val buf = ByteArray(2048)
            while (System.currentTimeMillis() < deadline) {
                try {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val text = String(packet.data, 0, packet.length)
                    if (!text.trim().startsWith("{")) {
                        // ignore non-JSON
                    } else {
                        val json = JSONObject(text)
                        if (json.optInt("v") == Protocol.VERSION &&
                            json.optString("type") == "discover_reply"
                        ) {
                            val fallback = packet.address.hostAddress
                            val host = json.optString("host").ifBlank { fallback.orEmpty() }
                            if (host.isNotBlank()) {
                                val name = json.optString("name", host)
                                val port = json.optInt("port", Protocol.SESSION_PORT)
                                val pinRequired = json.optBoolean("pinRequired", true)
                                found["$host:$port"] = DiscoveredWifiHost(name, host, port, pinRequired)
                            }
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    // keep polling until deadline
                }
            }
        }
        found.values.toList()
    }

    suspend fun connect(host: String, port: Int, pin: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            disconnectInternal()
            val session = WifiSession.connect(host, port)
            session.send(
                JSONObject()
                    .put("v", Protocol.VERSION)
                    .put("type", "hello")
                    .put("client", "TapBoard Android")
                    .put("clientVersion", BuildConfig.VERSION_NAME)
            )
            val ack = withTimeout(5000) { session.read() }
            if (ack.optString("type") != "hello_ack") {
                session.close()
                error("Unexpected hello response")
            }
            if (ack.optBoolean("authRequired", true)) {
                session.send(
                    JSONObject()
                        .put("v", Protocol.VERSION)
                        .put("type", "auth")
                        .put("pin", pin)
                )
                val auth = withTimeout(5000) { session.read() }
                when (auth.optString("type")) {
                    "auth_ok" -> Unit
                    "auth_fail" -> {
                        session.close()
                        error(auth.optString("reason", "invalid_pin"))
                    }
                    else -> {
                        session.close()
                        error("Unexpected auth response")
                    }
                }
            }
            sessionRef.set(session)
        }
    }

    fun sendMouse(dx: Int, dy: Int, buttons: Int, wheel: Int, hwheel: Int = 0) {
        val session = sessionRef.get() ?: return
        runCatching {
            session.send(
                JSONObject()
                    .put("v", Protocol.VERSION)
                    .put("type", "mouse")
                    .put("dx", dx)
                    .put("dy", dy)
                    .put("buttons", buttons)
                    .put("wheel", wheel)
                    .put("hwheel", hwheel)
            )
        }
    }

    fun sendKey(hid: Int, mods: Int, down: Boolean) {
        val session = sessionRef.get() ?: return
        runCatching {
            session.send(
                JSONObject()
                    .put("v", Protocol.VERSION)
                    .put("type", "key")
                    .put("hid", hid)
                    .put("mods", mods)
                    .put("down", down)
            )
        }
    }

    suspend fun ping(): Long? = withContext(Dispatchers.IO) {
        val session = sessionRef.get() ?: return@withContext null
        val t = System.currentTimeMillis()
        runCatching {
            session.send(JSONObject().put("v", Protocol.VERSION).put("type", "ping").put("t", t))
            val pong = withTimeout(2000) { session.read() }
            if (pong.optString("type") == "pong") System.currentTimeMillis() - t else null
        }.getOrNull()
    }

    fun disconnect() {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        sessionRef.getAndSet(null)?.let { s ->
            runCatching {
                s.send(JSONObject().put("v", Protocol.VERSION).put("type", "goodbye"))
            }
            s.close()
        }
    }
}
