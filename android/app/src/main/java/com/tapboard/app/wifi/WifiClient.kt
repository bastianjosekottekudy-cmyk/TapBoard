package com.tapboard.app.wifi

import android.content.Context
import android.net.wifi.WifiManager
import com.tapboard.app.BuildConfig
import com.tapboard.app.connection.DiscoveredWifiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

class WifiClient(private val appContext: Context) {
    private val sessionRef = AtomicReference<WifiSession?>(null)

    val isConnected: Boolean get() = sessionRef.get()?.isConnected == true

    suspend fun discover(timeoutMs: Long = 4000L): List<DiscoveredWifiHost> = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, DiscoveredWifiHost>()
        val wifi = appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val lock = wifi.createMulticastLock("tapboard-discover").apply {
            setReferenceCounted(false)
            acquire()
        }
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.reuseAddress = true
                socket.soTimeout = 300
                val payload = Protocol.DISCOVER_MAGIC.toByteArray(Charsets.UTF_8)

                // Global broadcast + every IPv4 subnet broadcast (works when 255.255.255.255 is blocked)
                val targets = linkedSetOf<InetAddress>()
                runCatching { targets += InetAddress.getByName("255.255.255.255") }
                for (addr in subnetBroadcasts()) {
                    targets += addr
                }
                // Also poke common gateway-ish directed broadcasts from DHCP link address
                for (target in targets) {
                    runCatching {
                        socket.send(DatagramPacket(payload, payload.size, target, Protocol.DISCOVERY_PORT))
                    }
                }
                // Retransmit a couple times — UDP is lossy on busy Wi‑Fi
                repeat(2) {
                    Thread.sleep(120)
                    for (target in targets) {
                        runCatching {
                            socket.send(DatagramPacket(payload, payload.size, target, Protocol.DISCOVERY_PORT))
                        }
                    }
                }

                val deadline = System.currentTimeMillis() + timeoutMs
                val buf = ByteArray(2048)
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                        if (!text.startsWith("{")) continue
                        val json = JSONObject(text)
                        if (json.optInt("v") != Protocol.VERSION) continue
                        if (json.optString("type") != "discover_reply") continue
                        val fallback = packet.address.hostAddress.orEmpty()
                        val host = json.optString("host").ifBlank { fallback }
                        if (host.isBlank()) continue
                        val name = json.optString("name", host)
                        val port = json.optInt("port", Protocol.SESSION_PORT)
                        val pinRequired = json.optBoolean("pinRequired", true)
                        found["$host:$port"] = DiscoveredWifiHost(name, host, port, pinRequired)
                    } catch (_: SocketTimeoutException) {
                        // keep waiting
                    }
                }
            }
        } finally {
            runCatching { if (lock.isHeld) lock.release() }
        }
        found.values.toList()
    }

    private fun subnetBroadcasts(): List<InetAddress> {
        val out = mutableListOf<InetAddress>()
        val ifaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (iface in ifaces) {
            if (!iface.isUp || iface.isLoopback) continue
            for (ia: InterfaceAddress in iface.interfaceAddresses) {
                val broadcast = ia.broadcast ?: continue
                if (broadcast is Inet4Address) out += broadcast
                val address = ia.address
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    // Also try directed .255 for /24 as fallback
                    val bytes = address.address
                    if (bytes.size == 4) {
                        bytes[3] = 0xff.toByte()
                        runCatching { out += InetAddress.getByAddress(bytes) }
                    }
                }
            }
        }
        return out.distinct()
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
