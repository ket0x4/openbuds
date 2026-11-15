package com.openbuds.driver.huawei

import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.DualConnectConnectionState
import com.openbuds.core.model.EqPreset
import com.openbuds.core.model.FitResult
import com.openbuds.core.model.FitTestStatus
import com.openbuds.core.model.GestureAction
import com.openbuds.core.model.HoldAncModes
import com.openbuds.core.protocol.MbbCommand
import com.openbuds.driver.huawei.handler.HuaweiResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HuaweiResponseParserTest {

    private fun command(commandId: Int, args: Map<Int, ByteArray>) =
        MbbCommand(HuaweiConstants.SERVICE_SETTINGS, commandId, args)

    @Test
    fun testBattery() {
        val status = HuaweiResponseParser.parseBattery(
            MbbCommand(
                serviceId = HuaweiConstants.SERVICE_BATTERY,
                commandId = HuaweiConstants.COMMAND_BATTERY_QUERY,
                args = mapOf(
                    2 to byteArrayOf(80, 0, 55),
                    3 to byteArrayOf(0, 1, 0)
                )
            )
        )

        assertEquals(80, status?.leftPercent)
        assertNull(status?.rightPercent)
        assertEquals(55, status?.casePercent)
        assertFalse(status!!.isLeftCharging)
        assertTrue(status.isRightCharging)
    }

    @Test
    fun testBatteryWithMissingArgs() {
        assertNull(
            HuaweiResponseParser.parseBattery(
                MbbCommand(HuaweiConstants.SERVICE_BATTERY, HuaweiConstants.COMMAND_BATTERY_QUERY)
            )
        )
    }

    @Test
    fun testAncStatus() {
        val status = HuaweiResponseParser.parseAncStatus(
            command(HuaweiConstants.COMMAND_ANC_STATUS, mapOf(1 to byteArrayOf(2, 1)))
        )

        assertEquals(AncMode.NOISE_CANCELLATION, status?.mode)
        assertEquals(AncLevel.ULTRA, status?.level)
        assertNull(HuaweiResponseParser.parseAncStatus(command(HuaweiConstants.COMMAND_ANC_STATUS, mapOf(1 to byteArrayOf(2)))))
    }

    @Test
    fun testInEar() {
        val patch = HuaweiResponseParser.parseInEar(
            command(HuaweiConstants.COMMAND_IN_EAR, mapOf(8 to byteArrayOf(1), 9 to byteArrayOf(0)))
        )

        assertEquals(true, patch?.left)
        assertEquals(false, patch?.right)
        assertNull(HuaweiResponseParser.parseInEar(command(HuaweiConstants.COMMAND_IN_EAR, emptyMap())))
    }

    @Test
    fun testFitTest() {
        val result = HuaweiResponseParser.parseFitTest(
            command(HuaweiConstants.COMMAND_FIT_TEST, mapOf(1 to byteArrayOf(1), 2 to byteArrayOf(2)))
        )

        assertEquals(FitTestStatus.COMPLETED, result.status)
        assertEquals(FitResult.GOOD, result.leftResult)
        assertEquals(FitResult.POOR, result.rightResult)
    }

    @Test
    fun testDeviceInfoPatchDropsBlankFields() {
        val patch = HuaweiResponseParser.parseDeviceInfoPatch(
            MbbCommand(
                serviceId = HuaweiConstants.SERVICE_DEVICE_INFO,
                commandId = HuaweiConstants.COMMAND_DEVICE_INFO_EXT,
                args = mapOf(
                    7 to "1.0.0.178".encodeToByteArray(),
                    15 to ByteArray(0)
                )
            )
        )

        assertEquals("1.0.0.178", patch.firmwareVersion)
        assertNull(patch.modelName)
        assertNull(patch.serialNumber)
    }

    @Test
    fun testGestureSettings() {
        val taps = HuaweiResponseParser.parseDoubleTap(
            command(HuaweiConstants.COMMAND_DOUBLE_TAP_GET, mapOf(1 to byteArrayOf(1), 2 to byteArrayOf(0)))
        )
        assertEquals(GestureAction.PLAY_PAUSE, taps?.left)
        assertEquals(GestureAction.VOICE_ASSISTANT, taps?.right)
        assertNull(HuaweiResponseParser.parseDoubleTap(command(HuaweiConstants.COMMAND_DOUBLE_TAP_GET, mapOf(1 to byteArrayOf(1)))))

        assertEquals(
            GestureAction.CYCLE_ANC,
            HuaweiResponseParser.parseHold(command(HuaweiConstants.COMMAND_HOLD_GET, mapOf(1 to byteArrayOf(10))))
        )
        assertEquals(
            HoldAncModes.ANC_AND_AWARENESS,
            HuaweiResponseParser.parseHoldModes(command(HuaweiConstants.COMMAND_HOLD_MODES_GET, mapOf(1 to byteArrayOf(3))))
        )
        assertEquals(
            GestureAction.ADJUST_VOLUME,
            HuaweiResponseParser.parseSwipe(command(HuaweiConstants.COMMAND_SWIPE_GET, mapOf(1 to byteArrayOf(0))))
        )
    }

    @Test
    fun testAudioSettings() {
        assertEquals(
            EqPreset.BASS_BOOST,
            HuaweiResponseParser.parseEq(command(HuaweiConstants.COMMAND_EQ_GET, mapOf(2 to byteArrayOf(2))))
        )
        assertEquals(
            true,
            HuaweiResponseParser.parseLowLatency(command(HuaweiConstants.COMMAND_LOW_LATENCY, mapOf(2 to byteArrayOf(1))))
        )
        assertEquals(
            false,
            HuaweiResponseParser.parseSoundQuality(command(HuaweiConstants.COMMAND_SOUND_QUALITY_GET, mapOf(2 to byteArrayOf(0))))
        )
        assertEquals(
            true,
            HuaweiResponseParser.parseSoundQuality(command(HuaweiConstants.COMMAND_SOUND_QUALITY_GET, mapOf(1 to byteArrayOf(1))))
        )
    }

    @Test
    fun testDualConnectRow() {
        val device = HuaweiResponseParser.parseDualConnectRow(
            command(
                HuaweiConstants.COMMAND_DUAL_CONNECT_ENUMERATE,
                mapOf(
                    4 to byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0x11, 0x22, 0x33),
                    5 to byteArrayOf(9),
                    7 to byteArrayOf(1),
                    8 to byteArrayOf(0),
                    9 to "Pixel 9 Pro".encodeToByteArray()
                )
            )
        )

        assertEquals("aabbcc112233", device?.macAddress)
        assertEquals("Pixel 9 Pro", device?.name)
        assertEquals(DualConnectConnectionState.PLAYING, device?.connectionState)
        assertTrue(device?.isPreferred == true)
        assertFalse(device?.autoConnect == true)
    }

    @Test
    fun testDualConnectRowRequiresMac() {
        assertNull(
            HuaweiResponseParser.parseDualConnectRow(
                command(HuaweiConstants.COMMAND_DUAL_CONNECT_ENUMERATE, mapOf(5 to byteArrayOf(1)))
            )
        )
    }
}
