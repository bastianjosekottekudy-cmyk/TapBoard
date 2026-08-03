package com.tapboard.app.connection

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tapboard.app.bluetooth.BluetoothHidManager
import com.tapboard.app.input.InputController
import com.tapboard.app.service.TapBoardForegroundService
import com.tapboard.app.settings.SettingsRepository
import com.tapboard.app.wifi.WifiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionRepository(
    private val context: Context,
    private val settings: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val wifiClient = WifiClient()
    private val bluetoothManager = BluetoothHidManager(context)

    private val _mode = MutableStateFlow(ConnectionMode.Wifi)
    val mode: StateFlow<ConnectionMode> = _mode.asStateFlow()

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _wifiHosts = MutableStateFlow<List<DiscoveredWifiHost>>(emptyList())
    val wifiHosts: StateFlow<List<DiscoveredWifiHost>> = _wifiHosts.asStateFlow()

    private val _btDevices = MutableStateFlow<List<BondedBtDevice>>(emptyList())
    val btDevices: StateFlow<List<BondedBtDevice>> = _btDevices.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    @Volatile private var sensitivity = 1.2f
    @Volatile private var invertScroll = false

    val input: InputController = InputController(
        state = _state,
        bluetooth = bluetoothManager,
        wifi = wifiClient,
        sensitivityProvider = { sensitivity },
        invertScrollProvider = { invertScroll }
    )

    init {
        bluetoothManager.setListeners(
            onConnectionChanged = { connected, name ->
                if (_mode.value != ConnectionMode.Bluetooth) return@setListeners
                if (connected) {
                    _state.value = ConnectionState.Connected(name ?: "Bluetooth host", ConnectionMode.Bluetooth)
                    startForeground()
                } else if (_state.value is ConnectionState.Connected) {
                    _state.value = ConnectionState.Disconnected
                    stopForeground()
                }
            },
            onError = { msg ->
                _state.value = ConnectionState.Error(msg, suggestWifi = true)
            }
        )
        scope.launch {
            settings.sensitivity.collect { sensitivity = it }
        }
        scope.launch {
            settings.invertScroll.collect { invertScroll = it }
        }
    }

    fun setMode(mode: ConnectionMode) {
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) {
            disconnect()
        }
        _mode.value = mode
    }

    fun refreshBluetoothDevices() {
        _btDevices.value = bluetoothManager.bondedDevices()
    }

    fun discoverWifi() {
        scope.launch {
            _scanning.value = true
            _wifiHosts.value = runCatching { wifiClient.discover() }.getOrDefault(emptyList())
            _scanning.value = false
        }
    }

    fun connectWifi(host: DiscoveredWifiHost, pin: String) {
        scope.launch {
            _mode.value = ConnectionMode.Wifi
            _state.value = ConnectionState.Connecting(host.name)
            val result = wifiClient.connect(host.host, host.port, pin)
            if (result.isSuccess) {
                settings.setWifiPin(pin)
                _state.value = ConnectionState.Connected(host.name, ConnectionMode.Wifi)
                startForeground()
            } else {
                _state.value = ConnectionState.Error(
                    result.exceptionOrNull()?.message ?: "Wi‑Fi connection failed"
                )
            }
        }
    }

    fun connectBluetooth(device: BondedBtDevice) {
        scope.launch {
            _mode.value = ConnectionMode.Bluetooth
            _state.value = ConnectionState.Connecting(device.name)
            val reg = bluetoothManager.ensureRegistered()
            if (reg.isFailure) {
                _state.value = ConnectionState.Error(
                    reg.exceptionOrNull()?.message ?: "HID registration failed",
                    suggestWifi = true
                )
                return@launch
            }
            val conn = bluetoothManager.connect(device.address)
            if (conn.isFailure) {
                _state.value = ConnectionState.Error(
                    conn.exceptionOrNull()?.message ?: "Bluetooth connect failed",
                    suggestWifi = true
                )
            }
            // Connected state comes from HID callback
        }
    }

    fun disconnect() {
        wifiClient.disconnect()
        bluetoothManager.disconnect()
        _state.value = ConnectionState.Disconnected
        stopForeground()
    }

    fun clearError() {
        if (_state.value is ConnectionState.Error) {
            _state.value = ConnectionState.Disconnected
        }
    }

    private fun startForeground() {
        val intent = Intent(context, TapBoardForegroundService::class.java).apply {
            action = TapBoardForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopForeground() {
        val intent = Intent(context, TapBoardForegroundService::class.java).apply {
            action = TapBoardForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
