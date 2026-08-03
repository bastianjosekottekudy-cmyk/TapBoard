package com.tapboard.app.connection

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tapboard.app.bluetooth.BluetoothHidManager
import com.tapboard.app.input.InputController
import com.tapboard.app.service.TapBoardForegroundService
import com.tapboard.app.settings.SettingsRepository
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
    private val bluetoothManager = BluetoothHidManager(context)

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _btDevices = MutableStateFlow<List<BondedBtDevice>>(emptyList())
    val btDevices: StateFlow<List<BondedBtDevice>> = _btDevices.asStateFlow()

    @Volatile private var sensitivity = 1.2f
    @Volatile private var invertScroll = false

    val input: InputController = InputController(
        state = _state,
        bluetooth = bluetoothManager,
        sensitivityProvider = { sensitivity },
        invertScrollProvider = { invertScroll }
    )

    init {
        bluetoothManager.setListeners(
            onConnectionChanged = { connected, name ->
                if (connected) {
                    _state.value = ConnectionState.Connected(name ?: "Bluetooth host")
                    startForeground()
                } else if (_state.value is ConnectionState.Connected) {
                    _state.value = ConnectionState.Disconnected
                    stopForeground()
                }
            },
            onError = { msg ->
                _state.value = ConnectionState.Error(msg)
            }
        )
        scope.launch {
            settings.sensitivity.collect { sensitivity = it }
        }
        scope.launch {
            settings.invertScroll.collect { invertScroll = it }
        }
    }

    fun refreshBluetoothDevices() {
        _btDevices.value = bluetoothManager.bondedDevices()
    }

    fun connectBluetooth(device: BondedBtDevice) {
        scope.launch {
            _state.value = ConnectionState.Connecting(device.name)
            val reg = bluetoothManager.ensureRegistered()
            if (reg.isFailure) {
                _state.value = ConnectionState.Error(
                    reg.exceptionOrNull()?.message ?: "HID registration failed"
                )
                return@launch
            }
            val conn = bluetoothManager.connect(device.address)
            if (conn.isFailure) {
                _state.value = ConnectionState.Error(
                    conn.exceptionOrNull()?.message ?: "Bluetooth connect failed"
                )
            }
            // Connected state comes from HID callback
        }
    }

    fun disconnect() {
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
