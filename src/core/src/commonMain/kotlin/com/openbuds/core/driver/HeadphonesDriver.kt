package com.openbuds.core.driver

import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus
import com.openbuds.core.model.CodecPreference
import com.openbuds.core.model.ConnectionState
import com.openbuds.core.model.DeviceInfo
import com.openbuds.core.model.DeviceSettings
import com.openbuds.core.model.DualConnectState
import com.openbuds.core.model.EqPreset
import com.openbuds.core.model.FitTestResult
import com.openbuds.core.model.GestureAction
import com.openbuds.core.model.GestureType
import com.openbuds.core.model.HoldAncModes
import com.openbuds.core.model.InEarStatus
import kotlinx.coroutines.flow.StateFlow

interface HeadphonesDriver {
    val connectionState: StateFlow<ConnectionState>
    val deviceInfo: StateFlow<DeviceInfo>
    val batteryStatus: StateFlow<BatteryStatus>
    val ancMode: StateFlow<AncMode>
    val ancLevel: StateFlow<AncLevel>
    val inEarStatus: StateFlow<InEarStatus>
    val settings: StateFlow<DeviceSettings>
    val dualConnectState: StateFlow<DualConnectState>
    val fitTestResult: StateFlow<FitTestResult>

    suspend fun start()
    suspend fun refreshAll()
    suspend fun setAncMode(mode: AncMode)
    suspend fun setAncLevel(level: AncLevel)
    suspend fun setAutoPause(enabled: Boolean)
    suspend fun setEqPreset(preset: EqPreset)
    suspend fun setLowLatency(enabled: Boolean)
    suspend fun setSoundQualityPreference(preferQuality: Boolean)
    suspend fun setCodecPreference(preference: CodecPreference) {
        setSoundQualityPreference(preference == CodecPreference.LDAC_QUALITY)
    }
    suspend fun setGesture(type: GestureType, action: GestureAction)
    suspend fun setHoldAncModes(modes: HoldAncModes)
    suspend fun startFitTest()
    suspend fun stopFitTest()

    // Multi-device (Dual Connect) operations
    suspend fun refreshDualConnect()
    suspend fun setDualConnectEnabled(enabled: Boolean)
    suspend fun connectDualConnectDevice(macAddress: String)
    suspend fun disconnectDualConnectDevice(macAddress: String)
    suspend fun setDualConnectPreferred(macAddress: String, preferred: Boolean)
    suspend fun setDualConnectAutoConnect(macAddress: String, enabled: Boolean)
    suspend fun unpairDualConnectDevice(macAddress: String)

    fun close()
}
