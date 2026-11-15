package com.openbuds.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.openbuds.app.log.OpenBudsLog
import java.util.UUID

/** Builds an RFCOMM socket, trying the standard service records before the hidden factory. */
class RfcommConnector(private val serviceUuid: UUID = SPP_UUID) {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        private const val RFCOMM_CHANNEL = 16
    }

    @SuppressLint("MissingPermission")
    fun open(device: BluetoothDevice): BluetoothSocket? {
        runCatching { return device.createInsecureRfcommSocketToServiceRecord(serviceUuid) }
            .onFailure { OpenBudsLog.w("Insecure RFCOMM socket unavailable for ${device.address}", it) }

        runCatching { return device.createRfcommSocketToServiceRecord(serviceUuid) }
            .onFailure { OpenBudsLog.w("Secure RFCOMM socket unavailable for ${device.address}", it) }

        runCatching {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            return method.invoke(device, RFCOMM_CHANNEL) as? BluetoothSocket
        }.onFailure { OpenBudsLog.w("Reflective RFCOMM socket unavailable for ${device.address}", it) }

        return null
    }
}
