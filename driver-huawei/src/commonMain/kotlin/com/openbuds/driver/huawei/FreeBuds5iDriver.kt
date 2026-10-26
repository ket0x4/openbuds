package com.openbuds.driver.huawei

import com.openbuds.core.driver.BluetoothTransport
import com.openbuds.core.driver.HeadphonesDriver
import com.openbuds.core.model.AncLevel
import com.openbuds.core.model.AncMode
import com.openbuds.core.model.BatteryStatus
import com.openbuds.core.model.ConnectionState
import com.openbuds.core.model.DeviceInfo
import com.openbuds.core.model.DeviceSettings
import com.openbuds.core.model.DualConnectConnectionState
import com.openbuds.core.model.DualConnectDevice
import com.openbuds.core.model.DualConnectState
import com.openbuds.core.model.EqPreset
import com.openbuds.core.model.GestureAction
import com.openbuds.core.model.GestureType
import com.openbuds.core.model.HoldAncModes
import com.openbuds.core.model.InEarStatus
import com.openbuds.core.protocol.MbbCommand
import com.openbuds.core.protocol.MbbFrameAccumulator
import com.openbuds.core.protocol.MbbSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FreeBuds5iDriver(
    private val transport: BluetoothTransport,
    initialDeviceInfo: DeviceInfo,
    private val enableWatchdog: Boolean = true,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : HeadphonesDriver {

    private val accumulator = MbbFrameAccumulator()
    private var readerJob: Job? = null
    private var watchdogJob: Job? = null

    private val _deviceInfo = MutableStateFlow(initialDeviceInfo)
    override val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connected(initialDeviceInfo))
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _batteryStatus = MutableStateFlow(BatteryStatus())
    override val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private val _ancMode = MutableStateFlow(AncMode.OFF)
    override val ancMode: StateFlow<AncMode> = _ancMode.asStateFlow()

    private val _ancLevel = MutableStateFlow(AncLevel.GENERAL)
    override val ancLevel: StateFlow<AncLevel> = _ancLevel.asStateFlow()

    private val _inEarStatus = MutableStateFlow(InEarStatus())
    override val inEarStatus: StateFlow<InEarStatus> = _inEarStatus.asStateFlow()

    private val _settings = MutableStateFlow(DeviceSettings())
    override val settings: StateFlow<DeviceSettings> = _settings.asStateFlow()

    private val _dualConnectState = MutableStateFlow(DualConnectState())
    override val dualConnectState: StateFlow<DualConnectState> = _dualConnectState.asStateFlow()

    override suspend fun start() {
        readerJob = scope.launch {
            transport.incoming.collect { bytes ->
                val commands = accumulator.push(bytes)
                for (command in commands) {
                    handleCommand(command)
                }
            }
        }

        refreshAll()

        if (enableWatchdog) {
            watchdogJob = scope.launch {
                while (isActive) {
                    delay(10_000)
                    refreshBattery()
                }
            }
        }
    }

    private suspend fun sendCommand(cmd: MbbCommand) {
        val payload = MbbSerializer.serialize(cmd)
        transport.send(payload)
    }

    override suspend fun refreshAll() {
        refreshDeviceInfo()
        refreshBattery()
        refreshAnc()
        refreshAutoPause()
        refreshGestures()
        refreshAudio()
        refreshDualConnect()
    }

    private suspend fun refreshDeviceInfo() {
        val requestedArgs = (1..24).associateWith { byteArrayOf() }
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_DEVICE_INFO,
                HuaweiConstants.COMMAND_DEVICE_INFO_EXT,
                requestedArgs
            )
        )
    }

    private suspend fun refreshBattery() {
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_BATTERY, HuaweiConstants.COMMAND_BATTERY_QUERY))
    }

    private suspend fun refreshAnc() {
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_SETTINGS, HuaweiConstants.COMMAND_ANC_STATUS))
    }

    private suspend fun refreshAutoPause() {
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_SETTINGS, HuaweiConstants.COMMAND_AUTO_PAUSE_GET))
    }

    private suspend fun refreshGestures() {
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_SETTINGS, HuaweiConstants.COMMAND_DOUBLE_TAP_GET))
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_SETTINGS, HuaweiConstants.COMMAND_TRIPLE_TAP_GET))
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_SETTINGS, HuaweiConstants.COMMAND_HOLD_GET))
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_HOLD_SPLIT_GET,
                mapOf(1 to byteArrayOf(), 2 to byteArrayOf())
            )
        )
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_HOLD_MODES_GET,
                mapOf(1 to byteArrayOf(), 2 to byteArrayOf())
            )
        )
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_SETTINGS, HuaweiConstants.COMMAND_SWIPE_GET))
    }

    private suspend fun refreshAudio() {
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_SETTINGS, HuaweiConstants.COMMAND_EQ_GET))
        sendCommand(MbbCommand(HuaweiConstants.SERVICE_SETTINGS, HuaweiConstants.COMMAND_LOW_LATENCY, mapOf(2 to byteArrayOf())))
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_SOUND_QUALITY_GET,
                mapOf(1 to byteArrayOf())
            )
        )
    }

    override suspend fun refreshDualConnect() {
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_DUAL_CONNECT_ENABLED_GET,
                mapOf(1 to byteArrayOf())
            )
        )
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_DUAL_CONNECT_ENUMERATE,
                mapOf(1 to byteArrayOf())
            )
        )
    }

    override suspend fun setAncMode(mode: AncMode) {
        _ancMode.value = mode
        val code = when (mode) {
            AncMode.OFF -> 0
            AncMode.NOISE_CANCELLATION -> 1
            AncMode.TRANSPARENCY -> 2
        }
        val currentLevel = _ancLevel.value
        val levelCode = ancLevelToCode(currentLevel)
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_ANC_SET,
                mapOf(1 to byteArrayOf(code.toByte(), levelCode.toByte()))
            )
        )
    }

    override suspend fun setAncLevel(level: AncLevel) {
        _ancLevel.value = level
        val levelCode = ancLevelToCode(level)
        val currentMode = _ancMode.value
        val modeCode = when (currentMode) {
            AncMode.OFF -> 0
            AncMode.NOISE_CANCELLATION -> 1
            AncMode.TRANSPARENCY -> 2
        }
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_ANC_SET,
                mapOf(1 to byteArrayOf(modeCode.toByte(), levelCode.toByte()))
            )
        )
    }

    override suspend fun setAutoPause(enabled: Boolean) {
        _settings.update { it.copy(autoPause = enabled) }
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_AUTO_PAUSE_SET,
                mapOf(1 to byteArrayOf(if (enabled) 1 else 0))
            )
        )
    }

    override suspend fun setEqPreset(preset: EqPreset) {
        _settings.update { it.copy(audio = it.audio.copy(eqPreset = preset)) }
        val code = when (preset) {
            EqPreset.DEFAULT -> 1
            EqPreset.BASS_BOOST -> 2
            EqPreset.TREBLE_BOOST -> 3
            EqPreset.VOICES -> 9
        }
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_EQ_SET,
                mapOf(1 to byteArrayOf(code.toByte()))
            )
        )
    }

    override suspend fun setLowLatency(enabled: Boolean) {
        _settings.update { it.copy(audio = it.audio.copy(lowLatencyMode = enabled)) }
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_LOW_LATENCY,
                mapOf(1 to byteArrayOf(if (enabled) 1 else 0))
            )
        )
    }

    override suspend fun setSoundQualityPreference(preferQuality: Boolean) {
        _settings.update { it.copy(audio = it.audio.copy(soundQualityPreference = preferQuality)) }
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_SOUND_QUALITY_SET,
                mapOf(1 to byteArrayOf(if (preferQuality) 1 else 0))
            )
        )
    }

    override suspend fun setHoldAncModes(modes: HoldAncModes) {
        val codeByte = modes.code.toByte()
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_HOLD_MODES_SET,
                mapOf(
                    1 to byteArrayOf(codeByte),
                    2 to byteArrayOf(codeByte)
                )
            )
        )
        _settings.update { it.copy(gestures = it.gestures.copy(holdAncModes = modes)) }
    }

    override suspend fun setDualConnectEnabled(enabled: Boolean) {
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_DUAL_CONNECT_ENABLED_SET,
                mapOf(1 to byteArrayOf(if (enabled) 1 else 0))
            )
        )
        _dualConnectState.update { it.copy(enabled = enabled) }
    }

    override suspend fun connectDualConnectDevice(macAddress: String) {
        executeDualConnectCommand(HuaweiConstants.DUAL_CONNECT_ACTION_CONNECT, macAddress)
        _dualConnectState.update { state ->
            val cleanTarget = normalizeMac(macAddress)
            state.copy(
                devices = state.devices.map { dev ->
                    if (normalizeMac(dev.macAddress) == cleanTarget) {
                        dev.copy(connectionState = DualConnectConnectionState.CONNECTED)
                    } else {
                        dev
                    }
                }
            )
        }
    }

    override suspend fun disconnectDualConnectDevice(macAddress: String) {
        executeDualConnectCommand(HuaweiConstants.DUAL_CONNECT_ACTION_DISCONNECT, macAddress)
        _dualConnectState.update { state ->
            val cleanTarget = normalizeMac(macAddress)
            state.copy(
                devices = state.devices.map { dev ->
                    if (normalizeMac(dev.macAddress) == cleanTarget) {
                        dev.copy(connectionState = DualConnectConnectionState.DISCONNECTED)
                    } else {
                        dev
                    }
                }
            )
        }
    }

    override suspend fun setDualConnectPreferred(macAddress: String, preferred: Boolean) {
        val targetBytes = if (preferred) macToBytes(macAddress) else ByteArray(6) { 0 }
        if (targetBytes.size != 6) return
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_DUAL_CONNECT_PREFERRED_SET,
                mapOf(1 to targetBytes)
            )
        )
        val cleanTarget = normalizeMac(macAddress)
        _dualConnectState.update { state ->
            state.copy(
                preferredDeviceMac = if (preferred) cleanTarget else "",
                devices = state.devices.map { dev ->
                    val isMatch = normalizeMac(dev.macAddress) == cleanTarget
                    dev.copy(isPreferred = preferred && isMatch)
                }
            )
        }
    }

    override suspend fun setDualConnectAutoConnect(macAddress: String, enabled: Boolean) {
        val action = if (enabled) {
            HuaweiConstants.DUAL_CONNECT_ACTION_ENABLE_AUTO
        } else {
            HuaweiConstants.DUAL_CONNECT_ACTION_DISABLE_AUTO
        }
        executeDualConnectCommand(action, macAddress)
        val cleanTarget = normalizeMac(macAddress)
        _dualConnectState.update { state ->
            state.copy(
                devices = state.devices.map { dev ->
                    if (normalizeMac(dev.macAddress) == cleanTarget) {
                        dev.copy(autoConnect = enabled)
                    } else {
                        dev
                    }
                }
            )
        }
    }

    override suspend fun unpairDualConnectDevice(macAddress: String) {
        executeDualConnectCommand(HuaweiConstants.DUAL_CONNECT_ACTION_UNPAIR, macAddress)
        val cleanTarget = normalizeMac(macAddress)
        _dualConnectState.update { state ->
            state.copy(
                devices = state.devices.filterNot { normalizeMac(it.macAddress) == cleanTarget }
            )
        }
    }

    private suspend fun executeDualConnectCommand(actionCode: Int, macAddress: String) {
        val macBytes = macToBytes(macAddress)
        if (macBytes.size != 6) return
        sendCommand(
            MbbCommand(
                HuaweiConstants.SERVICE_SETTINGS,
                HuaweiConstants.COMMAND_DUAL_CONNECT_EXECUTE,
                mapOf(actionCode to macBytes)
            )
        )
    }

    override suspend fun setGesture(type: GestureType, action: GestureAction) {
        val actionCode = gestureActionToCode(action)
        when (type) {
            GestureType.DOUBLE_TAP_LEFT -> {
                val right = gestureActionToCode(_settings.value.gestures.doubleTapRight)
                sendCommand(
                    MbbCommand(
                        HuaweiConstants.SERVICE_SETTINGS,
                        HuaweiConstants.COMMAND_DOUBLE_TAP_SET,
                        mapOf(1 to byteArrayOf(actionCode.toByte()), 2 to byteArrayOf(right.toByte()))
                    )
                )
                _settings.update { it.copy(gestures = it.gestures.copy(doubleTapLeft = action)) }
            }
            GestureType.DOUBLE_TAP_RIGHT -> {
                val left = gestureActionToCode(_settings.value.gestures.doubleTapLeft)
                sendCommand(
                    MbbCommand(
                        HuaweiConstants.SERVICE_SETTINGS,
                        HuaweiConstants.COMMAND_DOUBLE_TAP_SET,
                        mapOf(1 to byteArrayOf(left.toByte()), 2 to byteArrayOf(actionCode.toByte()))
                    )
                )
                _settings.update { it.copy(gestures = it.gestures.copy(doubleTapRight = action)) }
            }
            GestureType.TRIPLE_TAP_LEFT -> {
                val right = gestureActionToCode(_settings.value.gestures.tripleTapRight)
                sendCommand(
                    MbbCommand(
                        HuaweiConstants.SERVICE_SETTINGS,
                        HuaweiConstants.COMMAND_TRIPLE_TAP_SET,
                        mapOf(1 to byteArrayOf(actionCode.toByte()), 2 to byteArrayOf(right.toByte()))
                    )
                )
                _settings.update { it.copy(gestures = it.gestures.copy(tripleTapLeft = action)) }
            }
            GestureType.TRIPLE_TAP_RIGHT -> {
                val left = gestureActionToCode(_settings.value.gestures.tripleTapLeft)
                sendCommand(
                    MbbCommand(
                        HuaweiConstants.SERVICE_SETTINGS,
                        HuaweiConstants.COMMAND_TRIPLE_TAP_SET,
                        mapOf(1 to byteArrayOf(left.toByte()), 2 to byteArrayOf(actionCode.toByte()))
                    )
                )
                _settings.update { it.copy(gestures = it.gestures.copy(tripleTapRight = action)) }
            }
            GestureType.HOLD_BOTH -> {
                sendCommand(
                    MbbCommand(
                        HuaweiConstants.SERVICE_SETTINGS,
                        HuaweiConstants.COMMAND_HOLD_SET,
                        mapOf(1 to byteArrayOf(actionCode.toByte()))
                    )
                )
                sendCommand(
                    MbbCommand(
                        HuaweiConstants.SERVICE_SETTINGS,
                        HuaweiConstants.COMMAND_HOLD_SPLIT_SET,
                        mapOf(1 to byteArrayOf(actionCode.toByte()), 2 to byteArrayOf(actionCode.toByte()))
                    )
                )
                _settings.update { it.copy(gestures = it.gestures.copy(holdBoth = action)) }
            }
            GestureType.SWIPE -> {
                val swipeCode = if (action == GestureAction.ADJUST_VOLUME) 0 else 255
                sendCommand(
                    MbbCommand(
                        HuaweiConstants.SERVICE_SETTINGS,
                        HuaweiConstants.COMMAND_SWIPE_SET,
                        mapOf(1 to byteArrayOf(swipeCode.toByte()))
                    )
                )
                _settings.update { it.copy(gestures = it.gestures.copy(swipe = action)) }
            }
        }
    }

    private fun handleCommand(cmd: MbbCommand) {
        if (cmd.serviceId == HuaweiConstants.SERVICE_BATTERY &&
            (cmd.commandId == HuaweiConstants.COMMAND_BATTERY_EVENT || cmd.commandId == HuaweiConstants.COMMAND_BATTERY_QUERY)
        ) {
            handleBattery(cmd)
            return
        }

        if (cmd.serviceId == HuaweiConstants.SERVICE_DEVICE_INFO &&
            (cmd.commandId == HuaweiConstants.COMMAND_DEVICE_INFO_EXT || cmd.commandId == HuaweiConstants.COMMAND_DEVICE_INFO)
        ) {
            handleDeviceInfo(cmd)
            return
        }

        if (cmd.serviceId == HuaweiConstants.SERVICE_SETTINGS) {
            when (cmd.commandId) {
                HuaweiConstants.COMMAND_ANC_STATUS -> handleAncStatus(cmd)
                HuaweiConstants.COMMAND_IN_EAR -> handleInEar(cmd)
                HuaweiConstants.COMMAND_AUTO_PAUSE_GET -> handleAutoPause(cmd)
                HuaweiConstants.COMMAND_DOUBLE_TAP_GET -> handleDoubleTap(cmd)
                HuaweiConstants.COMMAND_TRIPLE_TAP_GET -> handleTripleTap(cmd)
                HuaweiConstants.COMMAND_HOLD_GET,
                HuaweiConstants.COMMAND_HOLD_SPLIT_GET -> handleHold(cmd)
                HuaweiConstants.COMMAND_HOLD_MODES_GET -> handleHoldModes(cmd)
                HuaweiConstants.COMMAND_SWIPE_GET -> handleSwipe(cmd)
                HuaweiConstants.COMMAND_EQ_GET -> handleEq(cmd)
                HuaweiConstants.COMMAND_LOW_LATENCY -> handleLowLatency(cmd)
                HuaweiConstants.COMMAND_SOUND_QUALITY_GET -> handleSoundQuality(cmd)
                HuaweiConstants.COMMAND_DUAL_CONNECT_ENABLED_GET -> handleDualConnectEnabled(cmd)
                HuaweiConstants.COMMAND_DUAL_CONNECT_ENUMERATE -> handleDualConnectDeviceRow(cmd)
                HuaweiConstants.COMMAND_DUAL_CONNECT_CHANGE_EVENT -> {
                    scope.launch { refreshDualConnect() }
                }
            }
        }
    }

    private fun handleDeviceInfo(cmd: MbbCommand) {
        val hwVer = cmd.args[3]?.decodeToString()?.trimEnd('\u0000')
        val swVer = cmd.args[7]?.decodeToString()?.trimEnd('\u0000')
        val serial = cmd.args[9]?.decodeToString()?.trimEnd('\u0000')
        val subModel = cmd.args[10]?.decodeToString()?.trimEnd('\u0000')
        val model = cmd.args[15]?.decodeToString()?.trimEnd('\u0000')

        _deviceInfo.update { current ->
            current.copy(
                firmwareVersion = if (!swVer.isNullOrBlank()) swVer else current.firmwareVersion,
                hardwareVersion = if (!hwVer.isNullOrBlank()) hwVer else current.hardwareVersion,
                serialNumber = if (!serial.isNullOrBlank()) serial else current.serialNumber,
                subModel = if (!subModel.isNullOrBlank()) subModel else current.subModel,
                modelName = if (!model.isNullOrBlank()) model else current.modelName
            )
        }
        _connectionState.value = ConnectionState.Connected(_deviceInfo.value)
    }

    private fun handleBattery(cmd: MbbCommand) {
        val levels = cmd.args[2] ?: return
        val statuses = cmd.args[3] ?: return
        if (levels.size < 3 || statuses.size < 3) return

        val leftLvl = levels[0].toInt() and 0xFF
        val rightLvl = levels[1].toInt() and 0xFF
        val caseLvl = levels[2].toInt() and 0xFF

        val leftCharge = statuses[0].toInt() == 1
        val rightCharge = statuses[1].toInt() == 1
        val caseCharge = statuses[2].toInt() == 1

        _batteryStatus.value = BatteryStatus(
            leftPercent = if (leftLvl == 0) null else leftLvl,
            rightPercent = if (rightLvl == 0) null else rightLvl,
            casePercent = if (caseLvl == 0) null else caseLvl,
            isLeftCharging = leftCharge,
            isRightCharging = rightCharge,
            isCaseCharging = caseCharge
        )
    }

    private fun handleAncStatus(cmd: MbbCommand) {
        val arg = cmd.args[1] ?: return
        if (arg.size >= 2) {
            val levelCode = arg[0].toInt() and 0xFF
            val modeCode = arg[1].toInt() and 0xFF

            _ancMode.value = when (modeCode) {
                1 -> AncMode.NOISE_CANCELLATION
                2 -> AncMode.TRANSPARENCY
                else -> AncMode.OFF
            }

            _ancLevel.value = when (levelCode) {
                1 -> AncLevel.COZY
                2 -> AncLevel.ULTRA
                3 -> AncLevel.DYNAMIC
                else -> AncLevel.GENERAL
            }
        }
    }

    private fun handleInEar(cmd: MbbCommand) {
        val left = cmd.args[8]?.firstOrNull()?.toInt()
        val right = cmd.args[9]?.firstOrNull()?.toInt()

        _inEarStatus.update { current ->
            current.copy(
                leftInEar = if (left != null) left == 1 else current.leftInEar,
                rightInEar = if (right != null) right == 1 else current.rightInEar
            )
        }
    }

    private fun handleAutoPause(cmd: MbbCommand) {
        val value = cmd.args[1]?.firstOrNull()?.toInt() ?: return
        _settings.update { it.copy(autoPause = value == 1) }
    }

    private fun handleDoubleTap(cmd: MbbCommand) {
        val left = cmd.args[1]?.firstOrNull()?.toInt() ?: return
        val right = cmd.args[2]?.firstOrNull()?.toInt() ?: return
        _settings.update {
            it.copy(
                gestures = it.gestures.copy(
                    doubleTapLeft = codeToGestureAction(left),
                    doubleTapRight = codeToGestureAction(right)
                )
            )
        }
    }

    private fun handleTripleTap(cmd: MbbCommand) {
        val left = cmd.args[1]?.firstOrNull()?.toInt() ?: return
        val right = cmd.args[2]?.firstOrNull()?.toInt() ?: return
        _settings.update {
            it.copy(
                gestures = it.gestures.copy(
                    tripleTapLeft = codeToGestureAction(left),
                    tripleTapRight = codeToGestureAction(right)
                )
            )
        }
    }

    private fun handleHold(cmd: MbbCommand) {
        val hold = cmd.args[1]?.firstOrNull()?.toInt() ?: return
        _settings.update {
            it.copy(
                gestures = it.gestures.copy(
                    holdBoth = if (hold == 10) GestureAction.CYCLE_ANC else GestureAction.NONE
                )
            )
        }
    }

    private fun handleHoldModes(cmd: MbbCommand) {
        val modeCode = cmd.args[1]?.firstOrNull()?.toInt() ?: return
        _settings.update {
            it.copy(
                gestures = it.gestures.copy(
                    holdAncModes = HoldAncModes.fromCode(modeCode)
                )
            )
        }
    }

    private fun handleSwipe(cmd: MbbCommand) {
        val swipe = cmd.args[1]?.firstOrNull()?.toInt() ?: return
        _settings.update {
            it.copy(
                gestures = it.gestures.copy(
                    swipe = if (swipe == 0) GestureAction.ADJUST_VOLUME else GestureAction.NONE
                )
            )
        }
    }

    private fun handleEq(cmd: MbbCommand) {
        val eq = cmd.args[2]?.firstOrNull()?.toInt() ?: return
        val preset = when (eq) {
            2 -> EqPreset.BASS_BOOST
            3 -> EqPreset.TREBLE_BOOST
            9 -> EqPreset.VOICES
            else -> EqPreset.DEFAULT
        }
        _settings.update { it.copy(audio = it.audio.copy(eqPreset = preset)) }
    }

    private fun handleLowLatency(cmd: MbbCommand) {
        val lowLatency = cmd.args[2]?.firstOrNull()?.toInt() ?: return
        _settings.update { it.copy(audio = it.audio.copy(lowLatencyMode = lowLatency == 1)) }
    }

    private fun handleSoundQuality(cmd: MbbCommand) {
        val qualityByte = (cmd.args[2] ?: cmd.args[1])?.firstOrNull()?.toInt() ?: return
        _settings.update {
            it.copy(audio = it.audio.copy(soundQualityPreference = qualityByte == 1))
        }
    }

    private fun handleDualConnectEnabled(cmd: MbbCommand) {
        val enabledByte = cmd.args[1]?.firstOrNull()?.toInt() ?: return
        _dualConnectState.update { it.copy(enabled = enabledByte == 1) }
    }

    private fun handleDualConnectDeviceRow(cmd: MbbCommand) {
        val macBytes = cmd.args[4] ?: return
        if (macBytes.size < 6) return
        val macHex = macBytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        val connRaw = cmd.args[5]?.firstOrNull()?.toInt()?.and(0xFF) ?: 0
        val preferredRaw = cmd.args[7]?.firstOrNull()?.toInt()?.and(0xFF) ?: 0
        val autoConnectRaw = cmd.args[8]?.firstOrNull()?.toInt()?.and(0xFF) ?: 1
        val nameBytes = cmd.args[9]
        val name = nameBytes?.decodeToString()?.trimEnd('\u0000')?.ifBlank { "Bluetooth Device" }
            ?: "Bluetooth Device"

        val connState = when {
            connRaw == 9 -> DualConnectConnectionState.PLAYING
            connRaw > 0 -> DualConnectConnectionState.CONNECTED
            else -> DualConnectConnectionState.DISCONNECTED
        }

        val deviceRow = DualConnectDevice(
            macAddress = macHex,
            name = name,
            connectionState = connState,
            isPreferred = preferredRaw == 1,
            autoConnect = autoConnectRaw == 1
        )

        _dualConnectState.update { current ->
            val updatedList = current.devices.toMutableList()
            val existingIdx = updatedList.indexOfFirst { normalizeMac(it.macAddress) == macHex }
            if (existingIdx >= 0) {
                updatedList[existingIdx] = deviceRow
            } else {
                updatedList.add(deviceRow)
            }
            val preferredMac = updatedList.firstOrNull { it.isPreferred }?.macAddress ?: current.preferredDeviceMac
            current.copy(
                devices = updatedList,
                preferredDeviceMac = preferredMac
            )
        }
    }

    private fun normalizeMac(mac: String): String =
        mac.replace(":", "").lowercase()

    private fun macToBytes(mac: String): ByteArray {
        val clean = normalizeMac(mac)
        if (clean.length != 12) return byteArrayOf()
        return ByteArray(6) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun ancLevelToCode(level: AncLevel): Int = when (level) {
        AncLevel.COZY -> 1
        AncLevel.GENERAL -> 0
        AncLevel.ULTRA -> 2
        AncLevel.DYNAMIC -> 3
    }

    private fun gestureActionToCode(action: GestureAction): Int = when (action) {
        GestureAction.NONE -> 255
        GestureAction.VOICE_ASSISTANT -> 0
        GestureAction.PLAY_PAUSE -> 1
        GestureAction.NEXT_TRACK -> 2
        GestureAction.PREVIOUS_TRACK -> 7
        GestureAction.CYCLE_ANC -> 10
        GestureAction.ADJUST_VOLUME -> 0
    }

    private fun codeToGestureAction(code: Int): GestureAction = when (code) {
        0 -> GestureAction.VOICE_ASSISTANT
        1 -> GestureAction.PLAY_PAUSE
        2 -> GestureAction.NEXT_TRACK
        7 -> GestureAction.PREVIOUS_TRACK
        10 -> GestureAction.CYCLE_ANC
        else -> GestureAction.NONE
    }

    override fun close() {
        watchdogJob?.cancel()
        readerJob?.cancel()
        transport.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        scope.cancel()
    }
}
