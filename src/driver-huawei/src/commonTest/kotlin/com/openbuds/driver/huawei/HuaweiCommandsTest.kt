package com.openbuds.driver.huawei

import com.openbuds.core.protocol.MbbCommand
import com.openbuds.core.protocol.MbbParser
import com.openbuds.driver.huawei.command.HuaweiCommands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HuaweiCommandsTest {

    private fun roundTrip(command: MbbCommand): MbbCommand =
        MbbParser.parse(com.openbuds.core.protocol.MbbSerializer.serialize(command))

    @Test
    fun testBatteryAndDeviceInfoQueries() {
        roundTrip(HuaweiCommands.batteryQuery()).let {
            assertEquals(HuaweiConstants.SERVICE_BATTERY, it.serviceId)
            assertEquals(HuaweiConstants.COMMAND_BATTERY_QUERY, it.commandId)
        }

        roundTrip(HuaweiCommands.deviceInfoRequest()).let {
            assertEquals(HuaweiConstants.SERVICE_DEVICE_INFO, it.serviceId)
            assertEquals(HuaweiConstants.COMMAND_DEVICE_INFO_EXT, it.commandId)
            assertEquals(24, it.args.size)
        }
    }

    @Test
    fun testNoiseControlSet() {
        roundTrip(HuaweiCommands.ancSet(modeCode = 1, levelCode = 2)).let {
            assertEquals(HuaweiConstants.SERVICE_SETTINGS, it.serviceId)
            assertEquals(HuaweiConstants.COMMAND_ANC_SET, it.commandId)
            assertEquals(1, it.args[1]?.get(0)?.toInt())
            assertEquals(2, it.args[1]?.get(1)?.toInt())
        }
    }

    @Test
    fun testGestureSettersCarryBothBuds() {
        roundTrip(HuaweiCommands.doubleTapSet(1, 2)).let {
            assertEquals(HuaweiConstants.COMMAND_DOUBLE_TAP_SET, it.commandId)
            assertEquals(1, it.args[1]?.firstOrNull()?.toInt())
            assertEquals(2, it.args[2]?.firstOrNull()?.toInt())
        }

        roundTrip(HuaweiCommands.tripleTapSet(7, 255)).let {
            assertEquals(HuaweiConstants.COMMAND_TRIPLE_TAP_SET, it.commandId)
            assertEquals(7, it.args[1]?.firstOrNull()?.toInt())
            assertEquals(255, it.args[2]?.firstOrNull()?.toInt()?.and(0xFF))
        }

        roundTrip(HuaweiCommands.holdSplitSet(10, 10)).let {
            assertEquals(HuaweiConstants.COMMAND_HOLD_SPLIT_SET, it.commandId)
            assertEquals(10, it.args[1]?.firstOrNull()?.toInt())
            assertEquals(10, it.args[2]?.firstOrNull()?.toInt())
        }
    }

    @Test
    fun testBooleanFlags() {
        assertEquals(1, roundTrip(HuaweiCommands.autoPauseSet(true)).args[1]?.firstOrNull()?.toInt())
        assertEquals(0, roundTrip(HuaweiCommands.autoPauseSet(false)).args[1]?.firstOrNull()?.toInt())
        assertEquals(1, roundTrip(HuaweiCommands.lowLatencySet(true)).args[1]?.firstOrNull()?.toInt())
        assertEquals(0, roundTrip(HuaweiCommands.soundQualitySet(false)).args[1]?.firstOrNull()?.toInt())
    }

    @Test
    fun testDualConnectCommands() {
        val mac = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66)

        roundTrip(HuaweiCommands.dualConnectExecute(3, mac)).let {
            assertEquals(HuaweiConstants.COMMAND_DUAL_CONNECT_EXECUTE, it.commandId)
            assertTrue(it.args[3]?.contentEquals(mac) == true)
        }

        roundTrip(HuaweiCommands.dualConnectPreferredSet(mac)).let {
            assertEquals(HuaweiConstants.COMMAND_DUAL_CONNECT_PREFERRED_SET, it.commandId)
            assertTrue(it.args[1]?.contentEquals(mac) == true)
        }
    }

    @Test
    fun testFitTestCommand() {
        assertEquals(1, roundTrip(HuaweiCommands.fitTest(true)).args[1]?.firstOrNull()?.toInt())
        assertEquals(0, roundTrip(HuaweiCommands.fitTest(false)).args[1]?.firstOrNull()?.toInt())
    }

    @Test
    fun testQueryArguments() {
        assertEquals(1, roundTrip(HuaweiCommands.lowLatencyGet()).args.size)
        assertEquals(1, roundTrip(HuaweiCommands.soundQualityGet()).args.size)
        assertEquals(2, roundTrip(HuaweiCommands.holdModesGet()).args.size)
    }
}
