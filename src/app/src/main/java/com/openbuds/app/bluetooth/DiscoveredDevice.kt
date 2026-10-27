package com.openbuds.app.bluetooth

import android.bluetooth.BluetoothDevice

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice
)
