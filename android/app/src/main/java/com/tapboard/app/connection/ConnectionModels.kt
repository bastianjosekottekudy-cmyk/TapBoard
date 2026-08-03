package com.tapboard.app.connection

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val targetName: String) : ConnectionState()
    data class Connected(val targetName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class BondedBtDevice(
    val name: String,
    val address: String
)
