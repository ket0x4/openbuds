package com.openbuds.driver.huawei.command

import com.openbuds.core.protocol.MbbCommand
import com.openbuds.driver.huawei.HuaweiConstants

/** Builders for every MBB request the FreeBuds 5i driver sends. */
object HuaweiCommands {

    private const val EMPTY = 0.toByte()

    private fun empty() = byteArrayOf()

    private fun flag(enabled: Boolean) = byteArrayOf(if (enabled) 1 else 0)

    private fun settings(commandId: Int, args: Map<Int, ByteArray> = emptyMap()) =
        MbbCommand(HuaweiConstants.SERVICE_SETTINGS, commandId, args)

    // region Queries

    fun batteryQuery() = MbbCommand(
        HuaweiConstants.SERVICE_BATTERY,
        HuaweiConstants.COMMAND_BATTERY_QUERY
    )

    /** Asks for device info fields 1..24; the buds answer with the populated subset. */
    fun deviceInfoRequest() = MbbCommand(
        HuaweiConstants.SERVICE_DEVICE_INFO,
        HuaweiConstants.COMMAND_DEVICE_INFO_EXT,
        (1..24).associateWith { empty() }
    )

    fun ancStatus() = settings(HuaweiConstants.COMMAND_ANC_STATUS)

    /**
     * Wear-detection state. Some firmwares answer with an empty payload instead of the
     * `[8]=left` / `[9]=right` tags, which the parser discards rather than misreading.
     */
    fun inEarStatus() = settings(HuaweiConstants.COMMAND_IN_EAR)

    fun autoPauseGet() = settings(HuaweiConstants.COMMAND_AUTO_PAUSE_GET)

    fun doubleTapGet() = settings(HuaweiConstants.COMMAND_DOUBLE_TAP_GET)

    fun tripleTapGet() = settings(HuaweiConstants.COMMAND_TRIPLE_TAP_GET)

    fun holdGet() = settings(HuaweiConstants.COMMAND_HOLD_GET)

    fun holdSplitGet() = settings(
        HuaweiConstants.COMMAND_HOLD_SPLIT_GET,
        mapOf(1 to empty(), 2 to empty())
    )

    fun holdModesGet() = settings(
        HuaweiConstants.COMMAND_HOLD_MODES_GET,
        mapOf(1 to empty(), 2 to empty())
    )

    fun swipeGet() = settings(HuaweiConstants.COMMAND_SWIPE_GET)

    fun eqGet() = settings(HuaweiConstants.COMMAND_EQ_GET)

    fun lowLatencyGet() = settings(
        HuaweiConstants.COMMAND_LOW_LATENCY,
        mapOf(2 to empty())
    )

    fun soundQualityGet() = settings(
        HuaweiConstants.COMMAND_SOUND_QUALITY_GET,
        mapOf(1 to empty())
    )

    fun dualConnectEnabledGet() = settings(
        HuaweiConstants.COMMAND_DUAL_CONNECT_ENABLED_GET,
        mapOf(1 to empty())
    )

    fun dualConnectEnumerate() = settings(
        HuaweiConstants.COMMAND_DUAL_CONNECT_ENUMERATE,
        mapOf(1 to empty())
    )

    // endregion

    // region Mutations

    fun ancSet(modeCode: Int, levelCode: Int) = settings(
        HuaweiConstants.COMMAND_ANC_SET,
        mapOf(1 to byteArrayOf(modeCode.toByte(), levelCode.toByte()))
    )

    fun autoPauseSet(enabled: Boolean) = settings(
        HuaweiConstants.COMMAND_AUTO_PAUSE_SET,
        mapOf(1 to flag(enabled))
    )

    fun doubleTapSet(leftCode: Int, rightCode: Int) = settings(
        HuaweiConstants.COMMAND_DOUBLE_TAP_SET,
        mapOf(1 to byteArrayOf(leftCode.toByte()), 2 to byteArrayOf(rightCode.toByte()))
    )

    fun tripleTapSet(leftCode: Int, rightCode: Int) = settings(
        HuaweiConstants.COMMAND_TRIPLE_TAP_SET,
        mapOf(1 to byteArrayOf(leftCode.toByte()), 2 to byteArrayOf(rightCode.toByte()))
    )

    fun holdSet(code: Int) = settings(
        HuaweiConstants.COMMAND_HOLD_SET,
        mapOf(1 to byteArrayOf(code.toByte()))
    )

    fun holdSplitSet(leftCode: Int, rightCode: Int) = settings(
        HuaweiConstants.COMMAND_HOLD_SPLIT_SET,
        mapOf(1 to byteArrayOf(leftCode.toByte()), 2 to byteArrayOf(rightCode.toByte()))
    )

    fun holdModesSet(code: Int) = settings(
        HuaweiConstants.COMMAND_HOLD_MODES_SET,
        mapOf(1 to byteArrayOf(code.toByte()), 2 to byteArrayOf(code.toByte()))
    )

    fun swipeSet(code: Int) = settings(
        HuaweiConstants.COMMAND_SWIPE_SET,
        mapOf(1 to byteArrayOf(code.toByte()))
    )

    fun eqSet(code: Int) = settings(
        HuaweiConstants.COMMAND_EQ_SET,
        mapOf(1 to byteArrayOf(code.toByte()))
    )

    fun lowLatencySet(enabled: Boolean) = settings(
        HuaweiConstants.COMMAND_LOW_LATENCY,
        mapOf(1 to flag(enabled))
    )

    fun soundQualitySet(preferQuality: Boolean) = settings(
        HuaweiConstants.COMMAND_SOUND_QUALITY_SET,
        mapOf(1 to flag(preferQuality))
    )

    fun dualConnectEnabledSet(enabled: Boolean) = settings(
        HuaweiConstants.COMMAND_DUAL_CONNECT_ENABLED_SET,
        mapOf(1 to flag(enabled))
    )

    fun dualConnectPreferredSet(macBytes: ByteArray) = settings(
        HuaweiConstants.COMMAND_DUAL_CONNECT_PREFERRED_SET,
        mapOf(1 to macBytes)
    )

    fun dualConnectExecute(actionCode: Int, macBytes: ByteArray) = settings(
        HuaweiConstants.COMMAND_DUAL_CONNECT_EXECUTE,
        mapOf(actionCode to macBytes)
    )

    fun fitTest(start: Boolean) = settings(
        HuaweiConstants.COMMAND_FIT_TEST,
        mapOf(1 to byteArrayOf(if (start) 1.toByte() else EMPTY))
    )

    // endregion
}
