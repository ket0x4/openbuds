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
import com.openbuds.core.model.DualConnectState
import com.openbuds.core.model.EqPreset
import com.openbuds.core.model.FitTestResult
import com.openbuds.core.model.FitTestStatus
import com.openbuds.core.model.GestureAction
import com.openbuds.core.model.GestureType
import com.openbuds.core.model.HoldAncModes
import com.openbuds.core.model.InEarStatus
import com.openbuds.core.protocol.MbbCommand
import com.openbuds.core.protocol.MbbFrameAccumulator
import com.openbuds.core.protocol.MbbSerializer
import com.openbuds.driver.huawei.codec.HuaweiAncCodec
import com.openbuds.driver.huawei.codec.HuaweiEqCodec
import com.openbuds.driver.huawei.codec.HuaweiGestureCodec
import com.openbuds.driver.huawei.codec.MacAddress
import com.openbuds.driver.huawei.command.HuaweiCommands
import com.openbuds.driver.huawei.handler.HuaweiResponseParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * HUAWEI FreeBuds 5i driver.
 *
 * Wire encoding lives in `codec/`, request building in `command/HuaweiCommands`, and response
 * decoding in `handler/HuaweiResponseParser`. This class owns state and orchestration only.
 */
class FreeBuds5iDriver(
    private val transport: BluetoothTransport,
    initialDeviceInfo: DeviceInfo,
    private val enableWatchdog: Boolean = true,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : HeadphonesDriver {

    private val accumulator = MbbFrameAccumulator()

    // The caller's scope is a parent only: close() must stop this driver without cancelling an
    // application-wide scope that other components share.
    private val driverJob = SupervisorJob(scope.coroutineContext[Job])
    private val driverScope = CoroutineScope(scope.coroutineContext + driverJob)

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

    private val _fitTestResult = MutableStateFlow(FitTestResult())
    override val fitTestResult: StateFlow<FitTestResult> = _fitTestResult.asStateFlow()

    override suspend fun start() {
        if (readerJob?.isActive == true) return

        readerJob = driverScope.launch {
            transport.incoming.collect { bytes ->
                for (command in accumulator.push(bytes)) {
                    handleCommand(command)
                }
            }
        }

        refreshAll()

        if (enableWatchdog) {
            watchdogJob = driverScope.launch {
                while (isActive) {
                    delay(10_000)
                    refreshBattery()
                }
            }
        }
    }

    private suspend fun sendCommand(cmd: MbbCommand) {
        trySend(cmd)
    }

    /** Returns false when the transport rejected the frame, so callers avoid false state. */
    private suspend fun trySend(cmd: MbbCommand): Boolean =
        runCatching { transport.send(MbbSerializer.serialize(cmd)) }.isSuccess

    override suspend fun refreshAll() {
        refreshDeviceInfo()
        refreshBattery()
        refreshAnc()
        refreshAutoPause()
        refreshGestures()
        refreshAudio()
        refreshDualConnect()
    }

    private suspend fun refreshDeviceInfo() = sendCommand(HuaweiCommands.deviceInfoRequest())

    private suspend fun refreshBattery() = sendCommand(HuaweiCommands.batteryQuery())

    private suspend fun refreshAnc() = sendCommand(HuaweiCommands.ancStatus())

    private suspend fun refreshAutoPause() = sendCommand(HuaweiCommands.autoPauseGet())

    private suspend fun refreshGestures() {
        sendCommand(HuaweiCommands.doubleTapGet())
        sendCommand(HuaweiCommands.tripleTapGet())
        sendCommand(HuaweiCommands.holdGet())
        sendCommand(HuaweiCommands.holdSplitGet())
        sendCommand(HuaweiCommands.holdModesGet())
        sendCommand(HuaweiCommands.swipeGet())
    }

    private suspend fun refreshAudio() {
        sendCommand(HuaweiCommands.eqGet())
        sendCommand(HuaweiCommands.lowLatencyGet())
        sendCommand(HuaweiCommands.soundQualityGet())
    }

    override suspend fun refreshDualConnect() {
        sendCommand(HuaweiCommands.dualConnectEnabledGet())
        sendCommand(HuaweiCommands.dualConnectEnumerate())
    }

    // region Noise control

    override suspend fun setAncMode(mode: AncMode) {
        val sent = trySend(
            HuaweiCommands.ancSet(
                modeCode = HuaweiAncCodec.modeToWire(mode),
                levelCode = HuaweiAncCodec.levelToWire(_ancLevel.value)
            )
        )
        if (sent) _ancMode.value = mode
    }

    override suspend fun setAncLevel(level: AncLevel) {
        val sent = trySend(
            HuaweiCommands.ancSet(
                modeCode = HuaweiAncCodec.modeToWire(_ancMode.value),
                levelCode = HuaweiAncCodec.levelToWire(level)
            )
        )
        if (sent) _ancLevel.value = level
    }

    // endregion

    // region Audio

    override suspend fun setAutoPause(enabled: Boolean) {
        if (trySend(HuaweiCommands.autoPauseSet(enabled))) {
            _settings.update { it.copy(autoPause = enabled) }
        }
    }

    override suspend fun setEqPreset(preset: EqPreset) {
        if (trySend(HuaweiCommands.eqSet(HuaweiEqCodec.presetToWire(preset)))) {
            _settings.update { it.copy(audio = it.audio.copy(eqPreset = preset)) }
        }
    }

    override suspend fun setLowLatency(enabled: Boolean) {
        if (trySend(HuaweiCommands.lowLatencySet(enabled))) {
            _settings.update { it.copy(audio = it.audio.copy(lowLatencyMode = enabled)) }
        }
    }

    override suspend fun setSoundQualityPreference(preferQuality: Boolean) {
        if (trySend(HuaweiCommands.soundQualitySet(preferQuality))) {
            _settings.update { it.copy(audio = it.audio.copy(soundQualityPreference = preferQuality)) }
        }
    }

    // endregion

    // region Gestures

    override suspend fun setGesture(type: GestureType, action: GestureAction) {
        val gestures = _settings.value.gestures
        when (type) {
            GestureType.DOUBLE_TAP_LEFT -> {
                val left = HuaweiGestureCodec.actionToWire(action, type) ?: return
                val right = HuaweiGestureCodec.actionToWire(gestures.doubleTapRight, GestureType.DOUBLE_TAP_RIGHT) ?: return
                if (trySend(HuaweiCommands.doubleTapSet(left, right))) {
                    _settings.update { it.copy(gestures = it.gestures.copy(doubleTapLeft = action)) }
                }
            }

            GestureType.DOUBLE_TAP_RIGHT -> {
                val left = HuaweiGestureCodec.actionToWire(gestures.doubleTapLeft, GestureType.DOUBLE_TAP_LEFT) ?: return
                val right = HuaweiGestureCodec.actionToWire(action, type) ?: return
                if (trySend(HuaweiCommands.doubleTapSet(left, right))) {
                    _settings.update { it.copy(gestures = it.gestures.copy(doubleTapRight = action)) }
                }
            }

            GestureType.TRIPLE_TAP_LEFT -> {
                val left = HuaweiGestureCodec.actionToWire(action, type) ?: return
                val right = HuaweiGestureCodec.actionToWire(gestures.tripleTapRight, GestureType.TRIPLE_TAP_RIGHT) ?: return
                if (trySend(HuaweiCommands.tripleTapSet(left, right))) {
                    _settings.update { it.copy(gestures = it.gestures.copy(tripleTapLeft = action)) }
                }
            }

            GestureType.TRIPLE_TAP_RIGHT -> {
                val left = HuaweiGestureCodec.actionToWire(gestures.tripleTapLeft, GestureType.TRIPLE_TAP_LEFT) ?: return
                val right = HuaweiGestureCodec.actionToWire(action, type) ?: return
                if (trySend(HuaweiCommands.tripleTapSet(left, right))) {
                    _settings.update { it.copy(gestures = it.gestures.copy(tripleTapRight = action)) }
                }
            }

            GestureType.HOLD_BOTH -> {
                val code = HuaweiGestureCodec.actionToWire(action, type) ?: return
                if (!trySend(HuaweiCommands.holdSet(code))) return
                if (trySend(HuaweiCommands.holdSplitSet(code, code))) {
                    _settings.update { it.copy(gestures = it.gestures.copy(holdBoth = action)) }
                }
            }

            GestureType.SWIPE -> {
                if (trySend(HuaweiCommands.swipeSet(HuaweiGestureCodec.swipeToWire(action)))) {
                    _settings.update { it.copy(gestures = it.gestures.copy(swipe = action)) }
                }
            }
        }
    }

    override suspend fun setHoldAncModes(modes: HoldAncModes) {
        if (trySend(HuaweiCommands.holdModesSet(modes.code))) {
            _settings.update { it.copy(gestures = it.gestures.copy(holdAncModes = modes)) }
        }
    }

    // endregion

    // region Dual Connect

    override suspend fun setDualConnectEnabled(enabled: Boolean) {
        if (trySend(HuaweiCommands.dualConnectEnabledSet(enabled))) {
            _dualConnectState.update { it.copy(enabled = enabled) }
        }
    }

    override suspend fun connectDualConnectDevice(macAddress: String) {
        executeDualConnectCommand(HuaweiConstants.DUAL_CONNECT_ACTION_CONNECT, macAddress)
        _dualConnectState.update { state ->
            val target = MacAddress.normalize(macAddress)
            state.copy(
                devices = state.devices.map { dev ->
                    if (MacAddress.normalize(dev.macAddress) == target) {
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
            val target = MacAddress.normalize(macAddress)
            state.copy(
                devices = state.devices.map { dev ->
                    if (MacAddress.normalize(dev.macAddress) == target) {
                        dev.copy(connectionState = DualConnectConnectionState.DISCONNECTED)
                    } else {
                        dev
                    }
                }
            )
        }
    }

    override suspend fun setDualConnectPreferred(macAddress: String, preferred: Boolean) {
        val targetBytes = if (preferred) MacAddress.toBytes(macAddress) else ByteArray(6) { 0 }
        if (targetBytes.size != 6) return
        sendCommand(HuaweiCommands.dualConnectPreferredSet(targetBytes))

        val target = MacAddress.normalize(macAddress)
        _dualConnectState.update { state ->
            state.copy(
                preferredDeviceMac = if (preferred) target else "",
                devices = state.devices.map { dev ->
                    val isMatch = MacAddress.normalize(dev.macAddress) == target
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

        val target = MacAddress.normalize(macAddress)
        _dualConnectState.update { state ->
            state.copy(
                devices = state.devices.map { dev ->
                    if (MacAddress.normalize(dev.macAddress) == target) {
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

        val target = MacAddress.normalize(macAddress)
        _dualConnectState.update { state ->
            state.copy(
                devices = state.devices.filterNot { MacAddress.normalize(it.macAddress) == target }
            )
        }
    }

    private suspend fun executeDualConnectCommand(actionCode: Int, macAddress: String) {
        val macBytes = MacAddress.toBytes(macAddress)
        if (macBytes.size != 6) return
        sendCommand(HuaweiCommands.dualConnectExecute(actionCode, macBytes))
    }

    // endregion

    // region Fit test

    override suspend fun startFitTest() {
        if (trySend(HuaweiCommands.fitTest(start = true))) {
            _fitTestResult.value = FitTestResult(status = FitTestStatus.TESTING)
        }
    }

    override suspend fun stopFitTest() {
        if (trySend(HuaweiCommands.fitTest(start = false))) {
            _fitTestResult.value = FitTestResult(status = FitTestStatus.IDLE)
        }
    }

    // endregion

    private fun handleCommand(cmd: MbbCommand) {
        when {
            cmd.serviceId == HuaweiConstants.SERVICE_BATTERY &&
                (cmd.commandId == HuaweiConstants.COMMAND_BATTERY_EVENT ||
                    cmd.commandId == HuaweiConstants.COMMAND_BATTERY_QUERY) ->
                HuaweiResponseParser.parseBattery(cmd)?.let { _batteryStatus.value = it }

            cmd.serviceId == HuaweiConstants.SERVICE_DEVICE_INFO &&
                (cmd.commandId == HuaweiConstants.COMMAND_DEVICE_INFO_EXT ||
                    cmd.commandId == HuaweiConstants.COMMAND_DEVICE_INFO) ->
                handleDeviceInfo(cmd)

            cmd.serviceId == HuaweiConstants.SERVICE_SETTINGS -> handleSettingsCommand(cmd)
        }
    }

    private fun handleDeviceInfo(cmd: MbbCommand) {
        val patch = HuaweiResponseParser.parseDeviceInfoPatch(cmd)
        _deviceInfo.update { current ->
            current.copy(
                firmwareVersion = patch.firmwareVersion ?: current.firmwareVersion,
                hardwareVersion = patch.hardwareVersion ?: current.hardwareVersion,
                serialNumber = patch.serialNumber ?: current.serialNumber,
                subModel = patch.subModel ?: current.subModel,
                modelName = patch.modelName ?: current.modelName
            )
        }
        _connectionState.value = ConnectionState.Connected(_deviceInfo.value)
    }

    private fun handleSettingsCommand(cmd: MbbCommand) {
        when (cmd.commandId) {
            HuaweiConstants.COMMAND_ANC_STATUS -> HuaweiResponseParser.parseAncStatus(cmd)?.let { status ->
                _ancMode.value = status.mode
                _ancLevel.value = status.level
            }

            HuaweiConstants.COMMAND_IN_EAR -> HuaweiResponseParser.parseInEar(cmd)?.let { patch ->
                _inEarStatus.update { current ->
                    current.copy(
                        leftInEar = patch.left ?: current.leftInEar,
                        rightInEar = patch.right ?: current.rightInEar
                    )
                }
            }

            HuaweiConstants.COMMAND_AUTO_PAUSE_GET -> HuaweiResponseParser.parseAutoPause(cmd)?.let { enabled ->
                _settings.update { it.copy(autoPause = enabled) }
            }

            HuaweiConstants.COMMAND_DOUBLE_TAP_GET -> HuaweiResponseParser.parseDoubleTap(cmd)?.let { pair ->
                _settings.update {
                    it.copy(
                        gestures = it.gestures.copy(
                            doubleTapLeft = pair.left,
                            doubleTapRight = pair.right
                        )
                    )
                }
            }

            HuaweiConstants.COMMAND_TRIPLE_TAP_GET -> HuaweiResponseParser.parseTripleTap(cmd)?.let { pair ->
                _settings.update {
                    it.copy(
                        gestures = it.gestures.copy(
                            tripleTapLeft = pair.left,
                            tripleTapRight = pair.right
                        )
                    )
                }
            }

            HuaweiConstants.COMMAND_HOLD_GET,
            HuaweiConstants.COMMAND_HOLD_SPLIT_GET -> HuaweiResponseParser.parseHold(cmd)?.let { action ->
                _settings.update { it.copy(gestures = it.gestures.copy(holdBoth = action)) }
            }

            HuaweiConstants.COMMAND_HOLD_MODES_GET -> HuaweiResponseParser.parseHoldModes(cmd)?.let { modes ->
                _settings.update { it.copy(gestures = it.gestures.copy(holdAncModes = modes)) }
            }

            HuaweiConstants.COMMAND_SWIPE_GET -> HuaweiResponseParser.parseSwipe(cmd)?.let { action ->
                _settings.update { it.copy(gestures = it.gestures.copy(swipe = action)) }
            }

            HuaweiConstants.COMMAND_EQ_GET -> HuaweiResponseParser.parseEq(cmd)?.let { preset ->
                _settings.update { it.copy(audio = it.audio.copy(eqPreset = preset)) }
            }

            HuaweiConstants.COMMAND_LOW_LATENCY -> HuaweiResponseParser.parseLowLatency(cmd)?.let { enabled ->
                _settings.update { it.copy(audio = it.audio.copy(lowLatencyMode = enabled)) }
            }

            HuaweiConstants.COMMAND_SOUND_QUALITY_GET -> HuaweiResponseParser.parseSoundQuality(cmd)?.let { preferQuality ->
                _settings.update { it.copy(audio = it.audio.copy(soundQualityPreference = preferQuality)) }
            }

            HuaweiConstants.COMMAND_DUAL_CONNECT_ENABLED_GET -> HuaweiResponseParser.parseDualConnectEnabled(cmd)?.let { enabled ->
                _dualConnectState.update { it.copy(enabled = enabled) }
            }

            HuaweiConstants.COMMAND_DUAL_CONNECT_ENUMERATE -> handleDualConnectRow(cmd)

            HuaweiConstants.COMMAND_FIT_TEST -> {
                _fitTestResult.value = HuaweiResponseParser.parseFitTest(cmd)
            }

            HuaweiConstants.COMMAND_DUAL_CONNECT_CHANGE_EVENT -> {
                driverScope.launch { refreshDualConnect() }
            }
        }
    }

    private fun handleDualConnectRow(cmd: MbbCommand) {
        val deviceRow = HuaweiResponseParser.parseDualConnectRow(cmd) ?: return

        _dualConnectState.update { current ->
            val updatedList = current.devices.toMutableList()
            val existingIndex = updatedList.indexOfFirst {
                MacAddress.normalize(it.macAddress) == deviceRow.macAddress
            }
            if (existingIndex >= 0) {
                updatedList[existingIndex] = deviceRow
            } else {
                updatedList.add(deviceRow)
            }
            val preferredMac = updatedList.firstOrNull { it.isPreferred }?.macAddress
                ?: current.preferredDeviceMac
            current.copy(devices = updatedList, preferredDeviceMac = preferredMac)
        }
    }

    override fun close() {
        watchdogJob?.cancel()
        readerJob?.cancel()
        transport.disconnect()
        driverJob.cancel()
        _connectionState.value = ConnectionState.Disconnected
    }
}
