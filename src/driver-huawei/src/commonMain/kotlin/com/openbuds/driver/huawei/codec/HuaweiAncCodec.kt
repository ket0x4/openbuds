package com.openbuds.driver.huawei.codec

import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode

/** Wire encoding for noise control (MBB service 43, commands 4 and 42). */
object HuaweiAncCodec {

    fun modeToWire(mode: AncMode): Int = when (mode) {
        AncMode.OFF -> 0
        AncMode.NOISE_CANCELLATION -> 1
        AncMode.TRANSPARENCY -> 2
    }

    fun modeFromWire(code: Int): AncMode = when (code) {
        1 -> AncMode.NOISE_CANCELLATION
        2 -> AncMode.TRANSPARENCY
        else -> AncMode.OFF
    }

    fun levelToWire(level: AncLevel): Int = when (level) {
        AncLevel.GENERAL -> 0
        AncLevel.COZY -> 1
        AncLevel.ULTRA -> 2
        AncLevel.DYNAMIC -> 3
    }

    fun levelFromWire(code: Int): AncLevel = when (code) {
        1 -> AncLevel.COZY
        2 -> AncLevel.ULTRA
        3 -> AncLevel.DYNAMIC
        else -> AncLevel.GENERAL
    }
}
