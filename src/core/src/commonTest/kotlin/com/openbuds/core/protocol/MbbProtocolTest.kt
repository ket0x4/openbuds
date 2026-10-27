package com.openbuds.core.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MbbProtocolTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testParseKnownBatteryPacket() {
        val hex = "5a001000012701015a02035a640f0303000001bed9"
        val bytes = hex.hexToByteArray()

        val cmd = MbbParser.parse(bytes)
        assertEquals(1, cmd.serviceId)
        assertEquals(39, cmd.commandId)
        assertEquals(3, cmd.args.size)

        assertArrayEquals(byteArrayOf(90.toByte()), cmd.args[1])
        assertArrayEquals(byteArrayOf(90.toByte(), 100.toByte(), 15.toByte()), cmd.args[2])
        assertArrayEquals(byteArrayOf(0.toByte(), 0.toByte(), 1.toByte()), cmd.args[3])
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testSerializeAndDeserializeMatches() {
        val cmd = MbbCommand(
            serviceId = 43,
            commandId = 4,
            args = mapOf(1 to byteArrayOf(1.toByte(), 255.toByte()))
        )

        val serialized = MbbSerializer.serialize(cmd)
        val deserialized = MbbParser.parse(serialized)

        assertEquals(cmd, deserialized)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testAccumulatorChunkedStream() {
        val hex = "5a001000012701015a02035a640f0303000001bed9"
        val bytes = hex.hexToByteArray()

        val acc = MbbFrameAccumulator()
        val part1 = bytes.copyOfRange(0, 10)
        val part2 = bytes.copyOfRange(10, bytes.size)

        val cmds1 = acc.push(part1)
        assertTrue(cmds1.isEmpty())

        val cmds2 = acc.push(part2)
        assertEquals(1, cmds2.size)
        assertEquals(1, cmds2[0].serviceId)
        assertEquals(39, cmds2[0].commandId)
    }
}
