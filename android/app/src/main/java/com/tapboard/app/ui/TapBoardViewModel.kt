package com.tapboard.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tapboard.app.connection.BondedBtDevice
import com.tapboard.app.connection.ConnectionMode
import com.tapboard.app.connection.ConnectionRepository
import com.tapboard.app.connection.ConnectionState
import com.tapboard.app.connection.DiscoveredWifiHost
import com.tapboard.app.input.InputController
import com.tapboard.app.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TapBoardViewModel(
    private val connection: ConnectionRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    val mode = connection.mode
    val state = connection.state
    val wifiHosts = connection.wifiHosts
    val btDevices = connection.btDevices
    val scanning = connection.scanning
    val input: InputController = connection.input

    val onboardingDone = settings.onboardingDone.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val sensitivity = settings.sensitivity.stateIn(viewModelScope, SharingStarted.Eagerly, 1.2f)
    val invertScroll = settings.invertScroll.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val darkTheme = settings.darkTheme.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val haptics = settings.haptics.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val keepScreenOn = settings.keepScreenOn.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val wifiPin = settings.wifiPin.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setMode(mode: ConnectionMode) = connection.setMode(mode)
    fun discoverWifi() = connection.discoverWifi()
    fun refreshBluetooth() = connection.refreshBluetoothDevices()
    fun connectWifi(host: DiscoveredWifiHost, pin: String) = connection.connectWifi(host, pin)
    fun connectBluetooth(device: BondedBtDevice) = connection.connectBluetooth(device)
    fun disconnect() = connection.disconnect()
    fun clearError() = connection.clearError()

    fun completeOnboarding() = viewModelScope.launch { settings.setOnboardingDone(true) }
    fun setSensitivity(v: Float) = viewModelScope.launch { settings.setSensitivity(v) }
    fun setInvertScroll(v: Boolean) = viewModelScope.launch { settings.setInvertScroll(v) }
    fun setDarkTheme(v: Boolean) = viewModelScope.launch { settings.setDarkTheme(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { settings.setHaptics(v) }
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { settings.setKeepScreenOn(v) }
    fun setWifiPin(v: String) = viewModelScope.launch { settings.setWifiPin(v) }

    fun isConnected(): Boolean = state.value is ConnectionState.Connected
}

class TapBoardViewModelFactory(
    private val connection: ConnectionRepository,
    private val settings: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TapBoardViewModel(connection, settings) as T
    }
}
