package com.openbuds.app.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import com.openbuds.app.di.appContainer
import com.openbuds.driver.huawei.FreeBuds5iMatcher

class BluetoothAclReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val device = IntentCompat.getParcelableExtra(
            intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        ) ?: return

        if (!FreeBuds5iMatcher.matches(device.name)) return

        val connectionManager = context.appContainer?.bluetoothConnectionManager ?: return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> connectionManager.connectToDevice(device)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> connectionManager.disconnect()
        }
    }
}
