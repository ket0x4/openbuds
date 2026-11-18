package com.openbuds.app.bluetooth

import android.bluetooth.BluetoothSocket
import com.openbuds.app.log.OpenBudsLog
import com.openbuds.core.driver.BluetoothTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * RFCOMM transport for the headphones.
 *
 * Owns its coroutine scope (derived from [parentScope] when given) so the read loop can be torn
 * down without touching the caller's scope. [closed] fires exactly once when the link drops,
 * whether that was requested locally or caused by the buds going out of range.
 */
class AndroidBluetoothTransport(
    private val socket: BluetoothSocket,
    parentScope: CoroutineScope? = null
) : BluetoothTransport {

    private val transportJob = SupervisorJob(parentScope?.coroutineContext?.get(Job))
    private val scope = CoroutineScope((parentScope?.coroutineContext ?: Dispatchers.IO) + transportJob)

    private val closedOnce = AtomicBoolean(false)

    // Unsolicited frames (wear detection, ANC changes made on the buds) must never be dropped:
    // losing one leaves the UI showing stale state until the next unrelated event.
    private val _incoming = Channel<ByteArray>(Channel.UNLIMITED)
    override val incoming: Flow<ByteArray> = _incoming.receiveAsFlow()

    // RFCOMM cannot interleave two writes; concurrent commands would corrupt each other's frames.
    private val sendMutex = Mutex()

    private val _closed = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val closed: SharedFlow<Unit> = _closed.asSharedFlow()

    private val inputStream: InputStream = socket.inputStream
    private val outputStream: OutputStream = socket.outputStream

    init {
        scope.launch {
            val buffer = ByteArray(1024)
            try {
                while (isActive && socket.isConnected) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        _incoming.send(buffer.copyOfRange(0, bytesRead))
                    } else if (bytesRead == -1) {
                        break
                    }
                }
            } catch (e: IOException) {
                OpenBudsLog.w("RFCOMM read loop ended", e)
            } finally {
                closeInternal()
            }
        }
    }

    override suspend fun send(data: ByteArray) {
        sendMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    // A dead socket throws instead of silently swallowing the frame, so callers
                    // never apply optimistic state for a command the buds never received.
                    if (!socket.isConnected) throw IOException("RFCOMM socket is not connected")
                    outputStream.write(data)
                    outputStream.flush()
                } catch (e: IOException) {
                    closeInternal()
                    throw e
                }
            }
        }
    }

    override fun disconnect() {
        closeInternal()
    }

    private fun closeInternal() {
        if (!closedOnce.compareAndSet(false, true)) return
        runCatching { socket.close() }
            .onFailure { OpenBudsLog.w("Unable to close the RFCOMM socket", it) }
        transportJob.cancel()
        _closed.tryEmit(Unit)
    }
}
