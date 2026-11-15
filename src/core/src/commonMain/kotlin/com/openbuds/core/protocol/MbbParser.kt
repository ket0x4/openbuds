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
