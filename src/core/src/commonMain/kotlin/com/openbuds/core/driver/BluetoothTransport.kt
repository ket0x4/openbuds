package com.openbuds.core.driver

import kotlinx.coroutines.flow.Flow

interface BluetoothTransport {
    val incoming: Flow<ByteArray>
    suspend fun send(data: ByteArray)
    fun disconnect()
}
