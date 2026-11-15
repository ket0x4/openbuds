package com.openbuds.core.protocol

/**
 * Reassembles MBB frames from an arbitrary byte stream.
 *
 * Frames can be split across reads or arrive back-to-back, so bytes are buffered until a full
 * frame is present. Bytes before the `0x5A 0x00` magic are discarded.
 *
 * @param onInvalidFrame invoked with the rejection reason for every frame that failed to parse,
 *   so transport/protocol desyncs are observable instead of silent.
 */
class MbbFrameAccumulator(
    private val onInvalidFrame: ((Throwable) -> Unit)? = null
) {
    private val buffer = ByteArray(4096)
    private var bufferSize = 0

    fun push(incoming: ByteArray): List<MbbCommand> {
        val commands = mutableListOf<MbbCommand>()

        if (incoming.size >= buffer.size) {
            // A chunk at least as large as the window cannot contain a buffered prefix; keep
            // the newest bytes so a frame straddling the boundary is still recognised.
            incoming.copyInto(buffer, destinationOffset = 0, startIndex = incoming.size - buffer.size)
            bufferSize = buffer.size
        } else {
            if (bufferSize + incoming.size > buffer.size) {
                bufferSize = 0
            }
            incoming.copyInto(buffer, destinationOffset = bufferSize)
            bufferSize += incoming.size
        }

        while (bufferSize >= 8) {
            var startIndex = -1
            for (i in 0 until (bufferSize - 1)) {
                if (buffer[i] == 0x5A.toByte() && buffer[i + 1] == 0x00.toByte()) {
                    startIndex = i
                    break
                }
            }

            if (startIndex == -1) {
                bufferSize = 0
                break
            }

            if (startIndex > 0) {
                buffer.copyInto(buffer, destinationOffset = 0, startIndex = startIndex, endIndex = bufferSize)
                bufferSize -= startIndex
            }

            if (bufferSize < 8) break

            val lengthByte = buffer[2].toInt() and 0xFF
            val packetLen = lengthByte + 5

            if (bufferSize < packetLen) {
                break
            }

            val packetBytes = buffer.copyOfRange(0, packetLen)
            buffer.copyInto(buffer, destinationOffset = 0, startIndex = packetLen, endIndex = bufferSize)
            bufferSize -= packetLen

            try {
                commands.add(MbbParser.parse(packetBytes))
            } catch (e: Exception) {
                onInvalidFrame?.invoke(e)
            }
        }

        return commands
    }

    fun clear() {
        bufferSize = 0
    }
}
