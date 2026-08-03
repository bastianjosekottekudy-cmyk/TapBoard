package com.tapboard.app.connection

enum class ConnectionMode {
    Bluetooth,
    Wifi
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val targetName: String) : ConnectionState()
    data class Connected(val targetName: String, val mode: ConnectionMode) : ConnectionState()
    data class Error(val message: String, val suggestWifi: Boolean = false) : ConnectionState()
}

data class DiscoveredWifiHost(
    val name: String,
    val host: String,
    val port: Int,
    val pinRequired: Boolean
)

data class BondedBtDevice(
    val name: String,
    val address: String
)
