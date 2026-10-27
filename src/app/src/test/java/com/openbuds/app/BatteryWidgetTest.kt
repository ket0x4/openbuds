package com.openbuds.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.openbuds.app.status.BatteryTone
import com.openbuds.app.status.HeadphonesCommands
import com.openbuds.app.status.StatusFormat
import com.openbuds.app.widget.BatteryWidgetKeys
import com.openbuds.app.widget.WidgetFormat
import com.openbuds.app.widget.WidgetSizes
import com.openbuds.app.widget.WidgetSnapshot
import com.openbuds.app.widget.statusLabel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryWidgetTest {

    @Test
    fun testBatteryWidgetKeysExist() {
        assertEquals("left_percent", BatteryWidgetKeys.LEFT_PERCENT.name)
        assertEquals("right_percent", BatteryWidgetKeys.RIGHT_PERCENT.name)
        assertEquals("case_percent", BatteryWidgetKeys.CASE_PERCENT.name)
        assertEquals("anc_mode", BatteryWidgetKeys.ANC_MODE.name)
    }

    @Test
    fun testBatteryStatusCalculations() {
        val status = BatteryStatus(
            leftPercent = 80,
            rightPercent = 90,
            casePercent = 50,
            isLeftCharging = true
        )
        assertEquals(80, status.lowestBudPercent)
        assertTrue(status.isAnyCharging)
    }

    @Test
    fun testBatteryTone() {
        assertEquals(BatteryTone.UNKNOWN, StatusFormat.tone(null, false))
        assertEquals(BatteryTone.LOW, StatusFormat.tone(15, false))
        assertEquals(BatteryTone.NORMAL, StatusFormat.tone(16, false))
        assertEquals(BatteryTone.CHARGING, StatusFormat.tone(10, true))
    }

    @Test
    fun testChipText() {
        assertEquals("L72 R68", StatusFormat.chipText(BatteryStatus(leftPercent = 72, rightPercent = 68)))
        assertEquals("L72%", StatusFormat.chipText(BatteryStatus(leftPercent = 72)))
        assertEquals("", StatusFormat.chipText(BatteryStatus()))
    }

    @Test
    fun testPromotionRules() {
        val idle = BatteryStatus(leftPercent = 50, rightPercent = 50)
        assertFalse(StatusFormat.shouldPromote(idle, AncMode.OFF))
        assertTrue(StatusFormat.shouldPromote(idle, AncMode.NOISE_CANCELLATION))
        assertTrue(StatusFormat.shouldPromote(idle.copy(isCaseCharging = true), AncMode.OFF))
    }

    @Test
    fun testIconLevel() {
        assertEquals(0, StatusFormat.iconLevel(BatteryStatus()))
        assertEquals(40, StatusFormat.iconLevel(BatteryStatus(leftPercent = 40, rightPercent = 90)))
    }

    @Test
    fun testResponsiveLayoutBreakpoints() {
        assertEquals(WidgetFormat.TINY, WidgetFormat.forSize(WidgetSizes.TINY))
        assertEquals(WidgetFormat.ROW, WidgetFormat.forSize(WidgetSizes.ROW))
        assertEquals(WidgetFormat.SQUARE, WidgetFormat.forSize(WidgetSizes.SQUARE))
        assertEquals(WidgetFormat.WIDE, WidgetFormat.forSize(WidgetSizes.WIDE))

        // Sizes between breakpoints resolve to the largest layout that still fits.
        assertEquals(WidgetFormat.ROW, WidgetFormat.forSize(DpSize(180.dp, 60.dp)))
        assertEquals(WidgetFormat.SQUARE, WidgetFormat.forSize(DpSize(180.dp, 110.dp)))
    }

    @Test
    fun testWidgetStatusLabel() {
        assertEquals("Disconnected", statusLabel(WidgetSnapshot(BatteryStatus(), isConnected = false, ancMode = null)))
        assertEquals("Connected", statusLabel(WidgetSnapshot(BatteryStatus(), isConnected = true, ancMode = null)))
        assertEquals(
            "Noise cancelling",
            statusLabel(WidgetSnapshot(BatteryStatus(), isConnected = true, ancMode = AncMode.NOISE_CANCELLATION)),
        )
    }

    @Test
    fun testNextAncModeCycle() {
        assertEquals(AncMode.NOISE_CANCELLATION, HeadphonesCommands.nextAncMode(AncMode.OFF))
        assertEquals(AncMode.NOISE_CANCELLATION, HeadphonesCommands.nextAncMode(null))
        assertEquals(AncMode.TRANSPARENCY, HeadphonesCommands.nextAncMode(AncMode.NOISE_CANCELLATION))
        assertEquals(AncMode.OFF, HeadphonesCommands.nextAncMode(AncMode.TRANSPARENCY))
    }
}
