package com.openbuds.core.protocol

object MbbSerializer {
    fun serialize(command: MbbCommand): ByteArray {
        var dataSize = 0
        for ((_, value) in command.args) {
            dataSize += 2 + value.size
        }

        val dataBytes = ByteArray(dataSize)
        var offset = 0
        for ((tag, value) in command.args) {
            dataBytes[offset++] = tag.toByte()
            dataBytes[offset++] = value.size.toByte()
            value.copyInto(dataBytes, destinationOffset = offset)
            offset += value.size
        }

        val lengthByte = dataSize + 2 + 1
        require(lengthByte <= 255) { "Payload too large for MBB protocol" }

        val headerSize = 6
        val totalWithoutCrc = headerSize + dataSize
        val packet = ByteArray(totalWithoutCrc + 2)

        packet[0] = 0x5A.toByte()
        packet[1] = 0x00.toByte()
        packet[2] = lengthByte.toByte()
        packet[3] = 0x00.toByte()
        packet[4] = command.serviceId.toByte()
        packet[5] = command.commandId.toByte()

        dataBytes.copyInto(packet, destinationOffset = headerSize)

        val crc = Crc16Xmodem.calculate(packet, 0, totalWithoutCrc)
        packet[totalWithoutCrc] = ((crc shr 8) and 0xFF).toByte()
        packet[totalWithoutCrc + 1] = (crc and 0xFF).toByte()

        return packet
    }
}
