package com.openbuds.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.openbuds.app.log.OpenBudsLog
import com.openbuds.app.status.DeviceStatus
import com.openbuds.core.driver.HeadphonesDriver
import com.openbuds.core.driver.HeadphonesMatcher
import com.openbuds.core.model.A2dpCodecStatus
import com.openbuds.core.model.CodecPreference
import com.openbuds.core.model.ConnectionState
import com.openbuds.driver.huawei.FreeBuds5iMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Connection facade for the headphones.
 *
 * Orchestrates the RFCOMM link and owns the active driver, delegating the supporting work:
 * bonded devices to [DeviceRegistry], socket creation to [RfcommConnector], driver selection to
 * [DriverFactory], and A2DP codec management to [A2dpCodecController].
 *
 * Call [start] once to begin observing A2DP, and [dispose] when the owner shuts down.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class BluetoothConnectionManager(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val appContext = context.applicationContext
    private val matcher: HeadphonesMatcher = FreeBuds5iMatcher

    private val deviceRegistry = DeviceRegistry(appContext, matcher)
    private val rfcommConnector = RfcommConnector()
    private val driverFactory = DriverFactory(matcher)
    private val codecController = A2dpCodecController(appContext, matcher)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeDriver = MutableStateFlow<HeadphonesDriver?>(null)
    val activeDriver: StateFlow<HeadphonesDriver?> = _activeDriver.asStateFlow()

    val a2dpCodecStatus: StateFlow<A2dpCodecStatus> = codecController.codecStatus

    /** Debounced device state shared by the widget, tile, notification and UI. */
    val status: StateFlow<DeviceStatus> = activeDriver
        .flatMapLatest { driver -> driverStatus(driver) }
        .distinctUntilChanged()
        .debounce(STATUS_DEBOUNCE_MS)
        .stateIn(scope, SharingStarted.Eagerly, DeviceStatus(isConnected = false))

    private var activeSocket: BluetoothSocket? = null
    private var activeDevice: BluetoothDevice? = null
    private var activeTransport: AndroidBluetoothTransport? = null
    private var connectionJob: Job? = null
    private var codecObserverJob: Job? = null
    private var linkMonitorJob: Job? = null

    fun start() {
        codecController.start()
    }

    fun getPairedFreeBuds(): List<DiscoveredDevice> = deviceRegistry.pairedBuds()

    fun getAllBondedDevices(): List<DiscoveredDevice> = deviceRegistry.allBonded()

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        teardownActiveConnection()
        connectionJob?.cancel()

        connectionJob = scope.launch {
            _connectionState.value = ConnectionState.Connecting

            val socket = rfcommConnector.open(device)
            if (socket == null) {
                _connectionState.value = ConnectionState.Error("Failed to create socket")
                return@launch
            }

            try {
                withContext(Dispatchers.IO) { socket.connect() }
            } catch (e: IOException) {
                OpenBudsLog.w("RFCOMM connect failed for ${device.address}", e)
                runCatching { socket.close() }
                _connectionState.value = ConnectionState.Error("Connection error: ${e.message}")
                return@launch
            }

            activeSocket = socket
            activeDevice = device
            codecController.setTarget(device)

            val transport = AndroidBluetoothTransport(socket, scope)
            val driver = driverFactory.create(device, transport, scope)
            if (driver == null) {
                transport.disconnect()
                runCatching { socket.close() }
                activeSocket = null
                activeDevice = null
                codecController.setTarget(null)
                _connectionState.value = ConnectionState.Error("Unsupported device")
                return@launch
            }

            activeTransport = transport
            _activeDriver.value = driver
            _connectionState.value = ConnectionState.Connected(driver.deviceInfo.value)

            monitorLink(socket)

            codecObserverJob?.cancel()
            codecObserverJob = scope.launch {
                driver.settings.collect { settings ->
                    codecController.refresh(settings.audio.soundQualityPreference)
                }
            }

            driver.start()
        }
    }

    suspend fun setCodecPreference(preference: CodecPreference) {
        _activeDriver.value?.setCodecPreference(preference)
        codecController.apply(preference)
    }

    fun refreshA2dpCodecStatus(preferLdac: Boolean) {
        codecController.refresh(preferLdac)
    }

    fun refreshA2dpCodecStatus() {
        val preferLdac = _activeDriver.value?.settings?.value?.audio?.soundQualityPreference ?: true
        refreshA2dpCodecStatus(preferLdac)
    }

    @SuppressLint("MissingPermission")
    fun autoConnect() {
        val target = deviceRegistry.preferredBuds() ?: return
        connectToDevice(target.device)
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        teardownActiveConnection()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun dispose() {
        disconnect()
        codecController.close()
    }

    private fun driverStatus(driver: HeadphonesDriver?) =
        if (driver == null) {
            connectionState.map {
                DeviceStatus(isConnected = false, isConnecting = it is ConnectionState.Connecting)
            }
        } else {
            combine(driver.batteryStatus, driver.ancMode) { battery, anc ->
                DeviceStatus(isConnected = true, battery = battery, ancMode = anc)
            }
        }

    /** Closes the live link, if any, without touching the connection state machine. */
    private fun teardownActiveConnection() {
        codecObserverJob?.cancel()
        codecObserverJob = null
        linkMonitorJob?.cancel()
        linkMonitorJob = null

        _activeDriver.value?.close()
        _activeDriver.value = null

        activeTransport?.disconnect()
        activeTransport = null

        runCatching { activeSocket?.close() }
            .onFailure { OpenBudsLog.w("Unable to close the active RFCOMM socket", it) }
        activeSocket = null
        activeDevice = null
        codecController.setTarget(null)
    }

    /** Drops the connection when the buds go out of range or the socket dies. */
    private fun monitorLink(socket: BluetoothSocket) {
        val transport = activeTransport ?: return
        linkMonitorJob?.cancel()
        linkMonitorJob = scope.launch {
            transport.closed.first()
            if (activeSocket === socket) {
                OpenBudsLog.w("Link to ${activeDevice?.address} was lost")
                teardownActiveConnection()
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private companion object {
        const val STATUS_DEBOUNCE_MS = 300L
    }
}
