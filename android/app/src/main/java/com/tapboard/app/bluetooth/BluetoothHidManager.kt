package com.tapboard.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.tapboard.app.connection.BondedBtDevice
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

class BluetoothHidManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val hidRef = AtomicReference<BluetoothHidDevice?>(null)
    private val hostRef = AtomicReference<BluetoothDevice?>(null)
    private val registered = AtomicReference(false)
    private val executor: Executor = Executors.newSingleThreadExecutor()

    private var onConnectionChanged: ((Boolean, String?) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun setListeners(
        onConnectionChanged: (Boolean, String?) -> Unit,
        onError: (String) -> Unit
    ) {
        this.onConnectionChanged = onConnectionChanged
        this.onError = onError
    }

    val isConnected: Boolean get() = hostRef.get() != null

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<BondedBtDevice> {
        val adapter = adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices.orEmpty().map {
            BondedBtDevice(it.name ?: it.address, it.address)
        }.sortedBy { it.name.lowercase() }
    }

    @SuppressLint("MissingPermission")
    suspend fun ensureRegistered(): Result<Unit> = suspendCancellableCoroutine { cont ->
        val adapter = adapter
        if (adapter == null || !adapter.isEnabled) {
            cont.resume(Result.failure(IllegalStateException("Bluetooth is off or unavailable")))
            return@suspendCancellableCoroutine
        }
        if (registered.get() && hidRef.get() != null) {
            cont.resume(Result.success(Unit))
            return@suspendCancellableCoroutine
        }
        val ok = adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile != BluetoothProfile.HID_DEVICE || proxy !is BluetoothHidDevice) {
                    if (cont.isActive) cont.resume(Result.failure(IllegalStateException("HID profile unavailable")))
                    return
                }
                hidRef.set(proxy)
                val sdp = BluetoothHidDeviceAppSdpSettings(
                    "TapBoard",
                    "TapBoard Keyboard & Mouse",
                    "TapBoard",
                    BluetoothHidDevice.SUBCLASS1_COMBO,
                    HidDescriptors.DESCRIPTOR
                )
                val inQos = BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
                )
                val outQos = BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
                )
                val registeredOk = proxy.registerApp(sdp, inQos, outQos, executor, callback)
                if (!registeredOk) {
                    registered.set(false)
                    if (cont.isActive) {
                        cont.resume(
                            Result.failure(
                                IllegalStateException(
                                    "This phone’s Bluetooth stack rejected HID registration."
                                )
                            )
                        )
                    }
                } else {
                    registered.set(true)
                    if (cont.isActive) cont.resume(Result.success(Unit))
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidRef.set(null)
                    registered.set(false)
                    hostRef.set(null)
                    onConnectionChanged?.invoke(false, null)
                }
            }
        }, BluetoothProfile.HID_DEVICE)
        if (!ok && cont.isActive) {
            cont.resume(Result.failure(IllegalStateException("Unable to open HID device profile")))
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Result<Unit> {
        val hid = hidRef.get() ?: return Result.failure(IllegalStateException("HID not registered"))
        val device = adapter?.getRemoteDevice(address)
            ?: return Result.failure(IllegalStateException("Unknown device"))
        return if (hid.connect(device)) Result.success(Unit)
        else Result.failure(IllegalStateException("Connect failed — pair the PC as a Bluetooth device first"))
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val hid = hidRef.get()
        val host = hostRef.getAndSet(null)
        if (hid != null && host != null) {
            runCatching { hid.disconnect(host) }
        }
        onConnectionChanged?.invoke(false, null)
    }

    @SuppressLint("MissingPermission")
    fun unregister() {
        disconnect()
        hidRef.get()?.let { runCatching { it.unregisterApp() } }
        registered.set(false)
        adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidRef.getAndSet(null))
    }

    @SuppressLint("MissingPermission")
    fun sendMouse(buttons: Int, dx: Int, dy: Int, wheel: Int) {
        val hid = hidRef.get() ?: return
        val host = hostRef.get() ?: return
        val report = HidDescriptors.mouseReport(buttons, dx, dy, wheel)
        hid.sendReport(host, HidDescriptors.ID_MOUSE, report)
    }

    @SuppressLint("MissingPermission")
    fun sendKeyboard(modifier: Int, vararg keys: Int) {
        val hid = hidRef.get() ?: return
        val host = hostRef.get() ?: return
        val report = HidDescriptors.keyboardReport(modifier, keys)
        hid.sendReport(host, HidDescriptors.ID_KEYBOARD, report)
    }

    fun releaseKeys() = sendKeyboard(0)

    private val callback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    hostRef.set(device)
                    onConnectionChanged?.invoke(true, device?.name ?: device?.address)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (hostRef.get()?.address == device?.address) {
                        hostRef.set(null)
                        onConnectionChanged?.invoke(false, null)
                    }
                }
            }
        }

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            this@BluetoothHidManager.registered.set(registered)
            if (!registered) {
                hostRef.set(null)
                onConnectionChanged?.invoke(false, null)
            }
        }
    }
}
