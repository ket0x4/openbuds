package com.openbuds.driver.huawei

import com.openbuds.core.driver.BluetoothTransport
import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.CodecPreference
import com.openbuds.core.model.DeviceInfo
import com.openbuds.core.model.DualConnectConnectionState
import com.openbuds.core.model.EqPreset
import com.openbuds.core.model.FitResult
import com.openbuds.core.model.FitTestResult
import com.openbuds.core.model.FitTestStatus
import com.openbuds.core.model.HoldAncModes
import com.openbuds.core.protocol.MbbCommand
import com.openbuds.core.protocol.MbbParser
import com.openbuds.core.protocol.MbbSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreeBuds5iDriverTest {

    private class FakeTransport : BluetoothTransport {
        val incomingFlow = MutableSharedFlow<ByteArray>(replay = 10)
        val sentPackets = mutableListOf<ByteArray>()

        override val incoming = incomingFlow

        override suspend fun send(data: ByteArray) {
            sentPackets.add(data)
        }

        override fun disconnect() {}
    }

    @Test
    fun testBatteryStatusUpdate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeTransport = FakeTransport()
        val info = DeviceInfo("HUAWEI FreeBuds 5i", "00:11:22:33:44:55")
        val driver = FreeBuds5iDriver(
            transport = fakeTransport,
            initialDeviceInfo = info,
            enableWatchdog = false,
            scope = CoroutineScope(testDispatcher)
        )

        driver.start()

        val batteryPacket = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 1,
                commandId = 39,
                args = mapOf(
                    1 to byteArrayOf(85.toByte()),
                    2 to byteArrayOf(85.toByte(), 90.toByte(), 100.toByte()),
                    3 to byteArrayOf(1.toByte(), 0.toByte(), 0.toByte())
                )
            )
        )

        fakeTransport.incomingFlow.emit(batteryPacket)
        advanceUntilIdle()

        val battery = driver.batteryStatus.value
        assertEquals(85, battery.leftPercent)
        assertEquals(90, battery.rightPercent)
        assertEquals(100, battery.casePercent)
        assertTrue(battery.isLeftCharging)

        driver.close()
    }

    @Test
    fun testAncStatusUpdate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeTransport = FakeTransport()
        val info = DeviceInfo("HUAWEI FreeBuds 5i", "00:11:22:33:44:55")
        val driver = FreeBuds5iDriver(
            transport = fakeTransport,
            initialDeviceInfo = info,
            enableWatchdog = false,
            scope = CoroutineScope(testDispatcher)
        )

        driver.start()

        val ancPacket = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 43,
                commandId = 42,
                args = mapOf(1 to byteArrayOf(2.toByte(), 1.toByte()))
            )
        )

        fakeTransport.incomingFlow.emit(ancPacket)
        advanceUntilIdle()

        assertEquals(AncMode.NOISE_CANCELLATION, driver.ancMode.value)
        assertEquals(AncLevel.ULTRA, driver.ancLevel.value)

        driver.close()
    }

    @Test
    fun testEqSettingUpdate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeTransport = FakeTransport()
        val info = DeviceInfo("HUAWEI FreeBuds 5i", "00:11:22:33:44:55")
        val driver = FreeBuds5iDriver(
            transport = fakeTransport,
            initialDeviceInfo = info,
            enableWatchdog = false,
            scope = CoroutineScope(testDispatcher)
        )

        driver.start()

        val eqPacket = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 43,
                commandId = 74,
                args = mapOf(2 to byteArrayOf(2.toByte()))
            )
        )

        fakeTransport.incomingFlow.emit(eqPacket)
        advanceUntilIdle()

        assertEquals(EqPreset.BASS_BOOST, driver.settings.value.audio.eqPreset)

        driver.close()
    }

    @Test
    fun testSoundQualityCodecUpdate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeTransport = FakeTransport()
        val info = DeviceInfo("HUAWEI FreeBuds 5i", "00:11:22:33:44:55")
        val driver = FreeBuds5iDriver(
            transport = fakeTransport,
            initialDeviceInfo = info,
            enableWatchdog = false,
            scope = CoroutineScope(testDispatcher)
        )

        driver.start()

        // Emit incoming sound quality response: 0 = AAC Connection Priority
        val aacPacket = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 43,
                commandId = HuaweiConstants.COMMAND_SOUND_QUALITY_GET,
                args = mapOf(2 to byteArrayOf(0.toByte()))
            )
        )
        fakeTransport.incomingFlow.emit(aacPacket)
        advanceUntilIdle()

        assertFalse(driver.settings.value.audio.soundQualityPreference)
        assertEquals(CodecPreference.AAC_CONNECTION, driver.settings.value.audio.codecPreference)

        // Switch to LDAC Quality Priority
        fakeTransport.sentPackets.clear()
        driver.setCodecPreference(CodecPreference.LDAC_QUALITY)
        advanceUntilIdle()

        assertTrue(driver.settings.value.audio.soundQualityPreference)
        assertEquals(CodecPreference.LDAC_QUALITY, driver.settings.value.audio.codecPreference)
        val sentCmds = fakeTransport.sentPackets.map { MbbParser.parse(it) }
        assertTrue(sentCmds.any {
            it.serviceId == HuaweiConstants.SERVICE_SETTINGS &&
                it.commandId == HuaweiConstants.COMMAND_SOUND_QUALITY_SET &&
                it.args[1]?.firstOrNull() == 1.toByte()
        })

        // Switch back to AAC Connection Priority
        fakeTransport.sentPackets.clear()
        driver.setCodecPreference(CodecPreference.AAC_CONNECTION)
        advanceUntilIdle()

        assertFalse(driver.settings.value.audio.soundQualityPreference)
        assertEquals(CodecPreference.AAC_CONNECTION, driver.settings.value.audio.codecPreference)
        val sentAacCmds = fakeTransport.sentPackets.map { MbbParser.parse(it) }
        assertTrue(sentAacCmds.any {
            it.serviceId == HuaweiConstants.SERVICE_SETTINGS &&
                it.commandId == HuaweiConstants.COMMAND_SOUND_QUALITY_SET &&
                it.args[1]?.firstOrNull() == 0.toByte()
        })

        driver.close()
    }

    @Test
    fun testDualConnectEnumerationAndActions() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeTransport = FakeTransport()
        val info = DeviceInfo("HUAWEI FreeBuds 5i", "00:11:22:33:44:55")
        val driver = FreeBuds5iDriver(
            transport = fakeTransport,
            initialDeviceInfo = info,
            enableWatchdog = false,
            scope = CoroutineScope(testDispatcher)
        )

        driver.start()

        val device1Packet = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 43,
                commandId = HuaweiConstants.COMMAND_DUAL_CONNECT_ENUMERATE,
                args = mapOf(
                    2 to byteArrayOf(2),
                    3 to byteArrayOf(0),
                    4 to byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0x11, 0x22, 0x33),
                    5 to byteArrayOf(9), // PLAYING
                    7 to byteArrayOf(1), // Preferred
                    8 to byteArrayOf(1), // AutoConnect
                    9 to "Pixel 9 Pro".encodeToByteArray()
                )
            )
        )

        val device2Packet = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 43,
                commandId = HuaweiConstants.COMMAND_DUAL_CONNECT_ENUMERATE,
                args = mapOf(
                    2 to byteArrayOf(2),
                    3 to byteArrayOf(1),
                    4 to byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66),
                    5 to byteArrayOf(0), // DISCONNECTED
                    7 to byteArrayOf(0),
                    8 to byteArrayOf(0),
                    9 to "MacBook Pro".encodeToByteArray()
                )
            )
        )

        fakeTransport.incomingFlow.emit(device1Packet)
        fakeTransport.incomingFlow.emit(device2Packet)
        advanceUntilIdle()

        val dcState = driver.dualConnectState.value
        assertEquals(2, dcState.devices.size)
        assertEquals("Pixel 9 Pro", dcState.devices[0].name)
        assertEquals(DualConnectConnectionState.PLAYING, dcState.devices[0].connectionState)
        assertTrue(dcState.devices[0].isPreferred)
        assertEquals("AA:BB:CC:11:22:33", dcState.devices[0].formattedMac)

        // Connect second device and set it preferred
        driver.connectDualConnectDevice("11:22:33:44:55:66")
        driver.setDualConnectPreferred("11:22:33:44:55:66", true)
        advanceUntilIdle()

        val updatedState = driver.dualConnectState.value
        assertEquals(DualConnectConnectionState.CONNECTED, updatedState.devices[1].connectionState)
        assertTrue(updatedState.devices[1].isPreferred)
        assertFalse(updatedState.devices[0].isPreferred)

        // Unpair second device
        driver.unpairDualConnectDevice("11:22:33:44:55:66")
        advanceUntilIdle()
        assertEquals(1, driver.dualConnectState.value.devices.size)

        driver.close()
    }

    @Test
    fun testHoldAncModesAndDeviceInfoUpdate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeTransport = FakeTransport()
        val info = DeviceInfo("HUAWEI FreeBuds 5i", "00:11:22:33:44:55")
        val driver = FreeBuds5iDriver(
            transport = fakeTransport,
            initialDeviceInfo = info,
            enableWatchdog = false,
            scope = CoroutineScope(testDispatcher)
        )

        driver.start()

        val holdModesPacket = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 43,
                commandId = HuaweiConstants.COMMAND_HOLD_MODES_GET,
                args = mapOf(1 to byteArrayOf(3)) // ANC_AND_AWARENESS
            )
        )
        val infoPacket = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 1,
                commandId = HuaweiConstants.COMMAND_DEVICE_INFO_EXT,
                args = mapOf(
                    3 to "Ver.A".encodeToByteArray(),
                    7 to "1.0.0.178".encodeToByteArray(),
                    15 to "T0014".encodeToByteArray()
                )
            )
        )

        fakeTransport.incomingFlow.emit(holdModesPacket)
        fakeTransport.incomingFlow.emit(infoPacket)
        advanceUntilIdle()

        assertEquals(HoldAncModes.ANC_AND_AWARENESS, driver.settings.value.gestures.holdAncModes)
        assertEquals("1.0.0.178", driver.deviceInfo.value.firmwareVersion)
        assertEquals("Ver.A", driver.deviceInfo.value.hardwareVersion)

        driver.close()
    }

    @Test
    fun testFitTestExecutionAndResults() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeTransport = FakeTransport()
        val info = DeviceInfo("HUAWEI FreeBuds 5i", "00:11:22:33:44:55")
        val driver = FreeBuds5iDriver(
            transport = fakeTransport,
            initialDeviceInfo = info,
            enableWatchdog = false,
            scope = CoroutineScope(testDispatcher)
        )

        driver.start()
        assertEquals(FitTestStatus.IDLE, driver.fitTestResult.value.status)

        // Start fit test
        driver.startFitTest()
        advanceUntilIdle()
        assertEquals(FitTestStatus.TESTING, driver.fitTestResult.value.status)

        // Emit fit test results packet (Left = Good, Right = Poor)
        val resultPacket = MbbSerializer.serialize(
            MbbCommand(
                serviceId = 43,
                commandId = HuaweiConstants.COMMAND_FIT_TEST,
                args = mapOf(
                    1 to byteArrayOf(1), // Good
                    2 to byteArrayOf(2)  // Poor
                )
            )
        )
        fakeTransport.incomingFlow.emit(resultPacket)
        advanceUntilIdle()

        val result = driver.fitTestResult.value
        assertEquals(FitTestStatus.COMPLETED, result.status)
        assertEquals(FitResult.GOOD, result.leftResult)
        assertEquals(FitResult.POOR, result.rightResult)

        // Stop/reset fit test
        driver.stopFitTest()
        advanceUntilIdle()
        assertEquals(FitTestStatus.IDLE, driver.fitTestResult.value.status)

        driver.close()
    }
}
