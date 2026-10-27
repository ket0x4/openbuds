package com.openbuds.app.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import com.openbuds.app.OpenBudsApp

class BluetoothAclReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val device = IntentCompat.getParcelableExtra(
            intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        ) ?: return
        val deviceName = device.name ?: ""

        if (!deviceName.contains("FreeBuds", ignoreCase = true)) {
            return
        }

        val app = context.applicationContext as? OpenBudsApp ?: return
        val connectionManager = app.container.bluetoothConnectionManager

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                connectionManager.connectToDevice(device)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                connectionManager.disconnect()
            }
        }
    }
}
