package com.openbuds.driver.huawei.handler

import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus
import com.openbuds.core.model.DualConnectConnectionState
import com.openbuds.core.model.DualConnectDevice
import com.openbuds.core.model.EqPreset
import com.openbuds.core.model.FitResult
import com.openbuds.core.model.FitTestResult
import com.openbuds.core.model.FitTestStatus
import com.openbuds.core.model.GestureAction
import com.openbuds.core.model.HoldAncModes
import com.openbuds.core.model.InEarStatus
import com.openbuds.core.protocol.MbbCommand
import com.openbuds.driver.huawei.codec.HuaweiAncCodec
import com.openbuds.driver.huawei.codec.HuaweiEqCodec
import com.openbuds.driver.huawei.codec.HuaweiGestureCodec
import com.openbuds.driver.huawei.codec.MacAddress

/** Noise control state as reported by command 42. */
data class AncStatus(val mode: AncMode, val level: AncLevel)

/** Partial device info; null fields mean "the buds did not report this one". */
data class DeviceInfoPatch(
    val firmwareVersion: String? = null,
    val hardwareVersion: String? = null,
    val serialNumber: String? = null,
    val subModel: String? = null,
    val modelName: String? = null
)

/** Left/right pair reported by the tap gesture queries. */
data class GesturePair(val left: GestureAction, val right: GestureAction)

/** In-ear flags; null means the corresponding bud was not reported. */
data class InEarPatch(val left: Boolean? = null, val right: Boolean? = null)

/**
 * Pure MBB response decoding: packet in, model value out.
 *
 * Every parser returns `null` when the packet is missing the fields it needs, so the driver
 * can skip the update instead of writing a default value over known-good state.
 */
object HuaweiResponseParser {

    fun parseBattery(cmd: MbbCommand): BatteryStatus? {
        val levels = cmd.args[2] ?: return null
        val statuses = cmd.args[3] ?: return null
        if (levels.size < 3 || statuses.size < 3) return null

        val leftLevel = levels[0].toInt() and 0xFF
        val rightLevel = levels[1].toInt() and 0xFF
        val caseLevel = levels[2].toInt() and 0xFF

        return BatteryStatus(
            leftPercent = if (leftLevel == 0) null else leftLevel,
            rightPercent = if (rightLevel == 0) null else rightLevel,
            casePercent = if (caseLevel == 0) null else caseLevel,
            isLeftCharging = statuses[0].toInt() == 1,
            isRightCharging = statuses[1].toInt() == 1,
            isCaseCharging = statuses[2].toInt() == 1
        )
    }

    fun parseAncStatus(cmd: MbbCommand): AncStatus? {
        val arg = cmd.args[1] ?: return null
        if (arg.size < 2) return null
        val levelCode = arg[0].toInt() and 0xFF
        val modeCode = arg[1].toInt() and 0xFF
        return AncStatus(
            mode = HuaweiAncCodec.modeFromWire(modeCode),
            level = HuaweiAncCodec.levelFromWire(levelCode)
        )
    }

    fun parseInEar(cmd: MbbCommand): InEarPatch? {
        if (cmd.args[8] == null && cmd.args[9] == null) return null
        return InEarPatch(
            left = cmd.args[8]?.firstOrNull()?.let { it.toInt() == 1 },
            right = cmd.args[9]?.firstOrNull()?.let { it.toInt() == 1 }
        )
    }

    fun parseFitTest(cmd: MbbCommand): FitTestResult {
        val leftCode = cmd.args[1]?.firstOrNull()?.toInt() ?: 0
        val rightCode = cmd.args[2]?.firstOrNull()?.toInt() ?: 0
        return FitTestResult(
            status = FitTestStatus.COMPLETED,
            leftResult = fitResultFromWire(leftCode),
            rightResult = fitResultFromWire(rightCode)
        )
    }

    private fun fitResultFromWire(code: Int): FitResult = when (code) {
        1 -> FitResult.GOOD
        2 -> FitResult.POOR
        else -> FitResult.UNKNOWN
    }

    fun parseDeviceInfoPatch(cmd: MbbCommand): DeviceInfoPatch = DeviceInfoPatch(
        hardwareVersion = cmd.args[3].textOrNull(),
        firmwareVersion = cmd.args[7].textOrNull(),
        serialNumber = cmd.args[9].textOrNull(),
        subModel = cmd.args[10].textOrNull(),
        modelName = cmd.args[15].textOrNull()
    )

    fun parseAutoPause(cmd: MbbCommand): Boolean? =
        cmd.args[1]?.firstOrNull()?.let { it.toInt() == 1 }

    fun parseDoubleTap(cmd: MbbCommand): GesturePair? {
        val left = cmd.args[1]?.firstOrNull()?.toInt() ?: return null
        val right = cmd.args[2]?.firstOrNull()?.toInt() ?: return null
        return GesturePair(
            left = HuaweiGestureCodec.actionFromWire(left),
            right = HuaweiGestureCodec.actionFromWire(right)
        )
    }

    fun parseTripleTap(cmd: MbbCommand): GesturePair? {
        val left = cmd.args[1]?.firstOrNull()?.toInt() ?: return null
        val right = cmd.args[2]?.firstOrNull()?.toInt() ?: return null
        return GesturePair(
            left = HuaweiGestureCodec.actionFromWire(left),
            right = HuaweiGestureCodec.actionFromWire(right)
        )
    }

    fun parseHold(cmd: MbbCommand): GestureAction? =
        cmd.args[1]?.firstOrNull()?.let { HuaweiGestureCodec.holdFromWire(it.toInt()) }

    fun parseHoldModes(cmd: MbbCommand): HoldAncModes? =
        cmd.args[1]?.firstOrNull()?.let { HoldAncModes.fromCode(it.toInt()) }

    fun parseSwipe(cmd: MbbCommand): GestureAction? =
        cmd.args[1]?.firstOrNull()?.let { HuaweiGestureCodec.swipeFromWire(it.toInt()) }

    fun parseEq(cmd: MbbCommand): EqPreset? =
        cmd.args[2]?.firstOrNull()?.let { HuaweiEqCodec.presetFromWire(it.toInt()) }

    fun parseLowLatency(cmd: MbbCommand): Boolean? =
        cmd.args[2]?.firstOrNull()?.let { it.toInt() == 1 }

    fun parseSoundQuality(cmd: MbbCommand): Boolean? =
        (cmd.args[2] ?: cmd.args[1])?.firstOrNull()?.let { it.toInt() == 1 }

    fun parseDualConnectEnabled(cmd: MbbCommand): Boolean? =
        cmd.args[1]?.firstOrNull()?.let { it.toInt() == 1 }

    fun parseDualConnectRow(cmd: MbbCommand): DualConnectDevice? {
        val macBytes = cmd.args[4] ?: return null
        if (macBytes.size < 6) return null

        val connectionRaw = cmd.args[5]?.firstOrNull()?.toInt()?.and(0xFF) ?: 0
        val preferredRaw = cmd.args[7]?.firstOrNull()?.toInt()?.and(0xFF) ?: 0
        val autoConnectRaw = cmd.args[8]?.firstOrNull()?.toInt()?.and(0xFF) ?: 1

        return DualConnectDevice(
            macAddress = MacAddress.fromBytes(macBytes),
            name = cmd.args[9].textOrNull() ?: "Bluetooth Device",
            connectionState = when {
                connectionRaw == 9 -> DualConnectConnectionState.PLAYING
                connectionRaw > 0 -> DualConnectConnectionState.CONNECTED
                else -> DualConnectConnectionState.DISCONNECTED
            },
            isPreferred = preferredRaw == 1,
            autoConnect = autoConnectRaw == 1
        )
    }

    private fun ByteArray?.textOrNull(): String? =
        this?.decodeToString()?.trimEnd('\u0000')?.ifBlank { null }
}
