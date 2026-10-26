package com.openbuds.core.protocol

object MbbParser {
    fun parse(packet: ByteArray): MbbCommand {
        require(packet.size >= 8) { "Packet too short: size=${packet.size}" }
        require(packet[0] == 0x5A.toByte() && packet[1] == 0x00.toByte()) { "Invalid magic bytes" }

        val lengthByte = packet[2].toInt() and 0xFF
        val expectedTotal = lengthByte + 5
        require(packet.size == expectedTotal) { "Length mismatch: got ${packet.size}, expected $expectedTotal" }

        require(Crc16Xmodem.verify(packet)) { "CRC16 checksum failed" }

        val serviceId = packet[4].toInt() and 0xFF
        val commandId = packet[5].toInt() and 0xFF

        val dataEnd = packet.size - 2
        var offset = 6
        val args = mutableMapOf<Int, ByteArray>()

        while (offset + 1 < dataEnd) {
            val tag = packet[offset].toInt() and 0xFF
            val len = packet[offset + 1].toInt() and 0xFF
            offset += 2

            if (offset + len <= dataEnd) {
                args[tag] = packet.copyOfRange(offset, offset + len)
                offset += len
            } else {
                break
            }
        }

        return MbbCommand(serviceId, commandId, args)
    }
}

class MbbFrameAccumulator {
    private val buffer = ByteArray(4096)
    private var bufferSize = 0

    fun push(incoming: ByteArray): List<MbbCommand> {
        val commands = mutableListOf<MbbCommand>()

        if (bufferSize + incoming.size > buffer.size) {
            bufferSize = 0
        }

        incoming.copyInto(buffer, destinationOffset = bufferSize)
        bufferSize += incoming.size

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
            } catch (_: Exception) {
            }
        }

        return commands
    }

    fun clear() {
        bufferSize = 0
    }
}
