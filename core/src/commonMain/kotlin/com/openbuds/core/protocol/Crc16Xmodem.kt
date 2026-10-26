package com.openbuds.core.protocol

object Crc16Xmodem {
    fun calculate(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int {
        var crc = 0x0000
        val end = offset + length
        for (i in offset until end) {
            val byte = bytes[i].toInt() and 0xFF
            crc = crc xor (byte shl 8)
            for (bit in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }

    fun calculateBytes(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): ByteArray {
        val crc = calculate(bytes, offset, length)
        return byteArrayOf(
            ((crc shr 8) and 0xFF).toByte(),
            (crc and 0xFF).toByte()
        )
    }

    fun verify(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Boolean {
        if (length < 2) return false
        val dataLen = length - 2
        val expected = calculate(bytes, offset, dataLen)
        val high = bytes[offset + dataLen].toInt() and 0xFF
        val low = bytes[offset + dataLen + 1].toInt() and 0xFF
        val actual = (high shl 8) or low
        return expected == actual
    }
}
