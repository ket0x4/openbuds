package com.openbuds.core.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MbbFrameAccumulatorTest {

    private fun command(commandId: Int = 8) =
        MbbCommand(1, commandId, mapOf(1 to byteArrayOf(42)))

    @Test
    fun testCompleteFrameInOnePush() {
        val accumulator = MbbFrameAccumulator()
        val parsed = accumulator.push(MbbSerializer.serialize(command()))

        assertEquals(1, parsed.size)
        assertEquals(8, parsed[0].commandId)
    }

    @Test
    fun testFrameSplitAcrossPushes() {
        val accumulator = MbbFrameAccumulator()
        val frame = MbbSerializer.serialize(command())
        val splitAt = frame.size / 2

        val first = accumulator.push(frame.copyOfRange(0, splitAt))
        val second = accumulator.push(frame.copyOfRange(splitAt, frame.size))

        assertTrue(first.isEmpty())
        assertEquals(1, second.size)
        assertEquals(8, second[0].commandId)
    }

    @Test
    fun testBackToBackFramesInOnePush() {
        val accumulator = MbbFrameAccumulator()
        val combined = MbbSerializer.serialize(command(8)) + MbbSerializer.serialize(command(9))

        val parsed = accumulator.push(combined)

        assertEquals(2, parsed.size)
        assertEquals(8, parsed[0].commandId)
        assertEquals(9, parsed[1].commandId)
    }

    @Test
    fun testGarbageBeforeMagicIsDiscarded() {
        val accumulator = MbbFrameAccumulator()
        val garbage = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val frame = MbbSerializer.serialize(command())

        val parsed = accumulator.push(garbage + frame)

        assertEquals(1, parsed.size)
        assertEquals(8, parsed[0].commandId)
    }

    @Test
    fun testInvalidChecksumIsReported() {
        var reported: Throwable? = null
        val accumulator = MbbFrameAccumulator { reported = it }

        val frame = MbbSerializer.serialize(command())
        frame[frame.size - 1] = (frame[frame.size - 1].toInt() xor 0xFF).toByte()

        val parsed = accumulator.push(frame)

        assertTrue(parsed.isEmpty())
        assertNotNull(reported)
    }

    @Test
    fun testOversizedChunkDoesNotThrowAndRecovers() {
        val accumulator = MbbFrameAccumulator()

        val junk = ByteArray(5000) { 0x11 }
        assertTrue(accumulator.push(junk).isEmpty())

        val parsed = accumulator.push(MbbSerializer.serialize(command()))
        assertEquals(1, parsed.size)
    }

    @Test
    fun testClearDropsPartialFrame() {
        val accumulator = MbbFrameAccumulator()
        val frame = MbbSerializer.serialize(command())

        accumulator.push(frame.copyOfRange(0, frame.size - 1))
        accumulator.clear()

        val parsed = accumulator.push(frame)
        assertEquals(1, parsed.size)
    }
}
