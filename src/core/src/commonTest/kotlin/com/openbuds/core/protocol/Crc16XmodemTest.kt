package com.openbuds.core.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Crc16XmodemTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testKnownMbbBatteryPacketCrc() {
        val hex = "5a001000012701015a02035a640f0303000001bed9"
        val bytes = hex.hexToByteArray()

        val isValid = Crc16Xmodem.verify(bytes)
        assertTrue("Checksum verification failed", isValid)

        val crc = Crc16Xmodem.calculate(bytes, 0, bytes.size - 2)
        assertEquals(0xBED9, crc)
    }
}
