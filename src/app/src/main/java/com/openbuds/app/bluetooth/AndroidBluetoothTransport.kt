package com.openbuds.app.bluetooth

import android.bluetooth.BluetoothSocket
import com.openbuds.core.driver.BluetoothTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class AndroidBluetoothTransport(
    private val socket: BluetoothSocket
) : BluetoothTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readJob: Job? = null

    private val _incoming = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val incoming: Flow<ByteArray> = _incoming.asSharedFlow()

    private val inputStream: InputStream = socket.inputStream
    private val outputStream: OutputStream = socket.outputStream

    init {
        readJob = scope.launch {
            val buffer = ByteArray(1024)
            while (isActive && socket.isConnected) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val chunk = buffer.copyOfRange(0, bytesRead)
                        _incoming.emit(chunk)
                    } else if (bytesRead == -1) {
                        break
                    }
                } catch (_: IOException) {
                    break
                }
            }
            disconnect()
        }
    }

    override suspend fun send(data: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                if (socket.isConnected) {
                    outputStream.write(data)
                    outputStream.flush()
                }
            } catch (_: IOException) {
                disconnect()
            }
        }
    }

    override fun disconnect() {
        readJob?.cancel()
        try {
            socket.close()
        } catch (_: IOException) {
        }
    }
}
