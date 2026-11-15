package com.openbuds.app.log

import android.util.Log

/** Single log tag for the app layer so Bluetooth failures are visible in logcat. */
object OpenBudsLog {
    private const val TAG = "OpenBuds"

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(TAG, message, throwable) else Log.w(TAG, message)
    }
}
