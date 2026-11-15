package com.openbuds.driver.huawei

import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.EqPreset
import com.openbuds.core.model.GestureAction
import com.openbuds.core.model.GestureType
import com.openbuds.driver.huawei.codec.HuaweiAncCodec
import com.openbuds.driver.huawei.codec.HuaweiEqCodec
import com.openbuds.driver.huawei.codec.HuaweiGestureCodec
import com.openbuds.driver.huawei.codec.MacAddress
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HuaweiCodecTest {

    @Test
    fun testAncModeRoundTrip() {
        AncMode.entries.forEach { mode ->
            assertEquals(mode, HuaweiAncCodec.modeFromWire(HuaweiAncCodec.modeToWire(mode)))
        }
        assertEquals(AncMode.OFF, HuaweiAncCodec.modeFromWire(99))
    }

    @Test
    fun testAncLevelRoundTrip() {
        AncLevel.entries.forEach { level ->
            assertEquals(level, HuaweiAncCodec.levelFromWire(HuaweiAncCodec.levelToWire(level)))
        }
        assertEquals(AncLevel.GENERAL, HuaweiAncCodec.levelFromWire(42))
    }

    @Test
    fun testEqPresetRoundTrip() {
        EqPreset.entries.forEach { preset ->
            assertEquals(preset, HuaweiEqCodec.presetFromWire(HuaweiEqCodec.presetToWire(preset)))
        }
        assertEquals(EqPreset.DEFAULT, HuaweiEqCodec.presetFromWire(7))
    }

    @Test
    fun testTapGestureCodes() {
        assertEquals(0, HuaweiGestureCodec.actionToWire(GestureAction.VOICE_ASSISTANT, GestureType.DOUBLE_TAP_LEFT))
        assertEquals(1, HuaweiGestureCodec.actionToWire(GestureAction.PLAY_PAUSE, GestureType.DOUBLE_TAP_RIGHT))
        assertEquals(2, HuaweiGestureCodec.actionToWire(GestureAction.NEXT_TRACK, GestureType.TRIPLE_TAP_LEFT))
        assertEquals(7, HuaweiGestureCodec.actionToWire(GestureAction.PREVIOUS_TRACK, GestureType.TRIPLE_TAP_RIGHT))
        assertEquals(255, HuaweiGestureCodec.actionToWire(GestureAction.NONE, GestureType.DOUBLE_TAP_LEFT))
    }

    @Test
    fun testUnsupportedGestureCombinationsReturnNull() {
        assertNull(HuaweiGestureCodec.actionToWire(GestureAction.ADJUST_VOLUME, GestureType.DOUBLE_TAP_LEFT))
        assertNull(HuaweiGestureCodec.actionToWire(GestureAction.ADJUST_VOLUME, GestureType.TRIPLE_TAP_LEFT))
        assertNull(HuaweiGestureCodec.actionToWire(GestureAction.VOICE_ASSISTANT, GestureType.TRIPLE_TAP_LEFT))
        assertNull(HuaweiGestureCodec.actionToWire(GestureAction.PLAY_PAUSE, GestureType.HOLD_BOTH))
        assertNull(HuaweiGestureCodec.actionToWire(GestureAction.NEXT_TRACK, GestureType.SWIPE))
    }

    @Test
    fun testHoldAndSwipeCodes() {
        assertEquals(10, HuaweiGestureCodec.actionToWire(GestureAction.CYCLE_ANC, GestureType.HOLD_BOTH))
        assertEquals(GestureAction.CYCLE_ANC, HuaweiGestureCodec.holdFromWire(10))
        assertEquals(GestureAction.NONE, HuaweiGestureCodec.holdFromWire(255))
        assertEquals(0, HuaweiGestureCodec.swipeToWire(GestureAction.ADJUST_VOLUME))
        assertEquals(255, HuaweiGestureCodec.swipeToWire(GestureAction.NONE))
        assertEquals(GestureAction.ADJUST_VOLUME, HuaweiGestureCodec.swipeFromWire(0))
        assertEquals(GestureAction.NONE, HuaweiGestureCodec.swipeFromWire(1))
    }

    @Test
    fun testMacAddressConversions() {
        assertEquals("aabbcc112233", MacAddress.normalize("AA:BB:CC:11:22:33"))
        assertArrayEquals(
            byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0x11, 0x22, 0x33),
            MacAddress.toBytes("aa:bb:cc:11:22:33")
        )
        assertEquals(0, MacAddress.toBytes("not-a-mac").size)
        assertEquals("AA:BB:CC:11:22:33", MacAddress.format("aabbcc112233"))
        assertEquals("aabbcc112233", MacAddress.fromBytes(MacAddress.toBytes("aabbcc112233")))
    }
}
