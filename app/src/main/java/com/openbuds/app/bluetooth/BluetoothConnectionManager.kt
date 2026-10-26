package com.openbuds.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCodecConfig
import android.bluetooth.BluetoothCodecStatus
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.openbuds.core.driver.HeadphonesDriver
import com.openbuds.core.model.A2dpCodecStatus
import com.openbuds.core.model.CodecPreference
import com.openbuds.core.model.ConnectionState
import com.openbuds.core.model.DeviceInfo
import com.openbuds.driver.huawei.FreeBuds5iDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        private const val ACTION_CODEC_CONFIG_CHANGED =
            "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED"
        private const val EXTRA_CODEC_STATUS = "android.bluetooth.extra.CODEC_STATUS"
        private const val OPTIONAL_CODECS_PREF_DISABLED = 0
        private const val OPTIONAL_CODECS_PREF_ENABLED = 1
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeDriver = MutableStateFlow<HeadphonesDriver?>(null)
    val activeDriver: StateFlow<HeadphonesDriver?> = _activeDriver.asStateFlow()

    private val _a2dpCodecStatus = MutableStateFlow(A2dpCodecStatus())
    val a2dpCodecStatus: StateFlow<A2dpCodecStatus> = _a2dpCodecStatus.asStateFlow()

    private var activeSocket: BluetoothSocket? = null
    private var activeDevice: BluetoothDevice? = null
    private var a2dpProxy: BluetoothA2dp? = null
    private var connectionJob: Job? = null
    private var batteryObserverJob: Job? = null
    private var codecObserverJob: Job? = null

    var onBatteryChangedListener: ((com.openbuds.core.model.BatteryStatus) -> Unit)? = null

    private val a2dpCodecReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == ACTION_CODEC_CONFIG_CHANGED) {
                val eventDevice = IntentCompat.getParcelableExtra(
                    intent,
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )
                val currentAddress = activeDevice?.address
                if (currentAddress != null && eventDevice != null && eventDevice.address != currentAddress) {
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val status = IntentCompat.getParcelableExtra(
                        intent,
                        EXTRA_CODEC_STATUS,
                        BluetoothCodecStatus::class.java
                    )
                    val config = status?.codecConfig
                    if (config != null && updateFromCodecConfig(config)) {
                        return
                    }
                }
                refreshA2dpCodecStatus()
            } else if (action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
                refreshA2dpCodecStatus()
            }
        }
    }

    init {
        try {
            val filter = IntentFilter().apply {
                addAction(ACTION_CODEC_CONFIG_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            }
            ContextCompat.registerReceiver(
                context,
                a2dpCodecReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {}

        try {
            bluetoothAdapter?.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == BluetoothProfile.A2DP) {
                            a2dpProxy = proxy as? BluetoothA2dp
                            refreshA2dpCodecStatus()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.A2DP) {
                            a2dpProxy = null
                        }
                    }
                },
                BluetoothProfile.A2DP
            )
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun getPairedFreeBuds(): List<DiscoveredDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return adapter.bondedDevices
            .filter { device ->
                val name = device.name ?: ""
                name.contains("FreeBuds", ignoreCase = true)
            }
            .map { device ->
                DiscoveredDevice(
                    name = device.name ?: "HUAWEI FreeBuds 5i",
                    address = device.address,
                    device = device
                )
            }
    }

    @SuppressLint("MissingPermission")
    fun getAllBondedDevices(): List<DiscoveredDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices.map { device ->
            DiscoveredDevice(
                name = device.name ?: "Unknown Device",
                address = device.address,
                device = device
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            _connectionState.value = ConnectionState.Connecting

            val socket = createSocket(device)
            if (socket == null) {
                _connectionState.value = ConnectionState.Error("Failed to create socket")
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    socket.connect()
                }
            } catch (e: IOException) {
                try {
                    socket.close()
                } catch (_: IOException) {}
                _connectionState.value = ConnectionState.Error("Connection error: ${e.message}")
                return@launch
            }

            activeSocket = socket
            activeDevice = device
            val transport = AndroidBluetoothTransport(socket)
            val info = DeviceInfo(
                name = device.name ?: "HUAWEI FreeBuds 5i",
                macAddress = device.address,
                modelName = "HUAWEI FreeBuds 5i"
            )

            val driver = FreeBuds5iDriver(
                transport = transport,
                initialDeviceInfo = info,
                scope = scope
            )
            _activeDriver.value = driver
            _connectionState.value = ConnectionState.Connected(info)

            batteryObserverJob?.cancel()
            batteryObserverJob = scope.launch {
                driver.batteryStatus.collect { battery ->
                    onBatteryChangedListener?.invoke(battery)
                }
            }

            codecObserverJob?.cancel()
            codecObserverJob = scope.launch {
                driver.settings.collect { settings ->
                    refreshA2dpCodecStatus(preferLdac = settings.audio.soundQualityPreference)
                }
            }

            driver.start()
        }
    }

    suspend fun setCodecPreference(preference: CodecPreference) {
        val preferLdac = preference == CodecPreference.LDAC_QUALITY
        _a2dpCodecStatus.value = fallbackCodecStatus(preferLdac)
        _activeDriver.value?.setCodecPreference(preference)
        applyA2dpCodecPreference(preferLdac)
        delay(600)
        refreshA2dpCodecStatus(preferLdac)
    }

    private fun fallbackCodecStatus(preferLdac: Boolean): A2dpCodecStatus =
        if (preferLdac) {
            A2dpCodecStatus(
                activeCodecName = "LDAC",
                sampleRateLabel = "96.0 kHz",
                bitsPerSampleLabel = "24-bit",
                bitrateLabel = "990 kbps Hi-Res",
                isLdacActive = true
            )
        } else {
            A2dpCodecStatus(
                activeCodecName = "AAC",
                sampleRateLabel = "48.0 kHz",
                bitsPerSampleLabel = "16-bit",
                bitrateLabel = "256 kbps Stable",
                isLdacActive = false
            )
        }

    @SuppressLint("MissingPermission")
    private fun resolveTargetA2dpDevice(proxy: BluetoothA2dp): BluetoothDevice? {
        activeDevice?.let { return it }
        return runCatching {
            proxy.connectedDevices.firstOrNull {
                it.name?.contains("FreeBuds", ignoreCase = true) == true
            } ?: proxy.connectedDevices.firstOrNull()
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun applyA2dpCodecPreference(preferLdac: Boolean) {
        val proxy = a2dpProxy ?: return
        val device = resolveTargetA2dpDevice(proxy) ?: return

        val prefValue = if (preferLdac) OPTIONAL_CODECS_PREF_ENABLED else OPTIONAL_CODECS_PREF_DISABLED
        runCatching {
            val setOptMethod = proxy.javaClass.getMethod(
                "setOptionalCodecsEnabled",
                BluetoothDevice::class.java,
                Int::class.javaPrimitiveType
            )
            setOptMethod.invoke(proxy, device, prefValue)
        }

        val toggleMethodName = if (preferLdac) "enableOptionalCodecs" else "disableOptionalCodecs"
        runCatching {
            val toggleMethod = proxy.javaClass.getMethod(toggleMethodName, BluetoothDevice::class.java)
            toggleMethod.invoke(proxy, device)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val preferredConfig = buildPreferredCodecConfig(proxy, device, preferLdac) ?: return@runCatching
                val setConfigMethod = proxy.javaClass.getMethod(
                    "setCodecConfigPreference",
                    BluetoothDevice::class.java,
                    BluetoothCodecConfig::class.java
                )
                setConfigMethod.invoke(proxy, device, preferredConfig)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun buildPreferredCodecConfig(
        proxy: BluetoothA2dp,
        device: BluetoothDevice,
        preferLdac: Boolean
    ): BluetoothCodecConfig? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val targetCodecType = if (preferLdac) {
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC
        } else {
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC
        }

        val selectable = runCatching {
            val getStatusMethod = proxy.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java)
            val status = getStatusMethod.invoke(proxy, device) as? BluetoothCodecStatus
            status?.codecsSelectableCapabilities?.firstOrNull { it.codecType == targetCodecType }
        }.getOrNull()

        val sampleRate = when {
            selectable != null && (selectable.sampleRate and BluetoothCodecConfig.SAMPLE_RATE_96000) != 0 && preferLdac ->
                BluetoothCodecConfig.SAMPLE_RATE_96000
            selectable != null && (selectable.sampleRate and BluetoothCodecConfig.SAMPLE_RATE_48000) != 0 ->
                BluetoothCodecConfig.SAMPLE_RATE_48000
            selectable != null && (selectable.sampleRate and BluetoothCodecConfig.SAMPLE_RATE_44100) != 0 ->
                BluetoothCodecConfig.SAMPLE_RATE_44100
            preferLdac -> BluetoothCodecConfig.SAMPLE_RATE_96000
            else -> BluetoothCodecConfig.SAMPLE_RATE_48000
        }

        val bitsPerSample = when {
            selectable != null && (selectable.bitsPerSample and BluetoothCodecConfig.BITS_PER_SAMPLE_24) != 0 && preferLdac ->
                BluetoothCodecConfig.BITS_PER_SAMPLE_24
            selectable != null && (selectable.bitsPerSample and BluetoothCodecConfig.BITS_PER_SAMPLE_32) != 0 && preferLdac ->
                BluetoothCodecConfig.BITS_PER_SAMPLE_32
            selectable != null && (selectable.bitsPerSample and BluetoothCodecConfig.BITS_PER_SAMPLE_16) != 0 ->
                BluetoothCodecConfig.BITS_PER_SAMPLE_16
            preferLdac -> BluetoothCodecConfig.BITS_PER_SAMPLE_24
            else -> BluetoothCodecConfig.BITS_PER_SAMPLE_16
        }

        return BluetoothCodecConfig.Builder()
            .setCodecType(targetCodecType)
            .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
            .setSampleRate(sampleRate)
            .setBitsPerSample(bitsPerSample)
            .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO)
            .setCodecSpecific1(if (preferLdac) 1003L else 0L)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun updateFromCodecConfig(config: BluetoothCodecConfig): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (config.codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID) return false

        val isLdac = config.codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC
        val codecName = when (config.codecType) {
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC -> "LDAC"
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC -> "AAC"
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC -> "SBC"
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX -> "aptX"
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD -> "aptX HD"
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3 -> "LC3"
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS -> "Opus"
            else -> return false
        }
        val sampleRate = when (config.sampleRate) {
            BluetoothCodecConfig.SAMPLE_RATE_96000 -> "96.0 kHz"
            BluetoothCodecConfig.SAMPLE_RATE_88200 -> "88.2 kHz"
            BluetoothCodecConfig.SAMPLE_RATE_48000 -> "48.0 kHz"
            BluetoothCodecConfig.SAMPLE_RATE_44100 -> "44.1 kHz"
            else -> if (isLdac) "96.0 kHz" else "48.0 kHz"
        }
        val bitDepth = when (config.bitsPerSample) {
            BluetoothCodecConfig.BITS_PER_SAMPLE_32 -> "32-bit"
            BluetoothCodecConfig.BITS_PER_SAMPLE_24 -> "24-bit"
            BluetoothCodecConfig.BITS_PER_SAMPLE_16 -> "16-bit"
            else -> if (isLdac) "24-bit" else "16-bit"
        }
        val bitrate = if (isLdac) {
            when (config.codecSpecific1) {
                1000L -> "990 kbps"
                1001L -> "660 kbps"
                1002L -> "330 kbps"
                else -> "up to 990 kbps"
            }
        } else {
            "256 kbps"
        }
        _a2dpCodecStatus.value = A2dpCodecStatus(
            activeCodecName = codecName,
            sampleRateLabel = sampleRate,
            bitsPerSampleLabel = bitDepth,
            bitrateLabel = bitrate,
            isLdacActive = isLdac
        )
        return true
    }

    @SuppressLint("MissingPermission")
    fun refreshA2dpCodecStatus(
        preferLdac: Boolean = _activeDriver.value?.settings?.value?.audio?.soundQualityPreference ?: true
    ) {
        val proxy = a2dpProxy
        val device = if (proxy != null) resolveTargetA2dpDevice(proxy) else activeDevice
        if (device != null && proxy != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val getCodecStatusMethod = proxy.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java)
                val codecStatus = getCodecStatusMethod.invoke(proxy, device) as? BluetoothCodecStatus
                val config = codecStatus?.codecConfig
                if (config != null && updateFromCodecConfig(config)) {
                    return
                }
            } catch (_: Exception) {
                // Fallback to MBB preference profile below
            }
        }

        _a2dpCodecStatus.value = fallbackCodecStatus(preferLdac)
    }

    @SuppressLint("MissingPermission")
    fun autoConnect() {
        val freeBuds = getPairedFreeBuds()
        val target = freeBuds.firstOrNull { it.name.contains("5i", ignoreCase = true) }
            ?: freeBuds.firstOrNull()

        if (target != null) {
            connectToDevice(target.device)
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        batteryObserverJob?.cancel()
        codecObserverJob?.cancel()
        _activeDriver.value?.close()
        _activeDriver.value = null
        activeDevice = null
        try {
            activeSocket?.close()
        } catch (_: IOException) {}
        activeSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    private fun createSocket(device: BluetoothDevice): BluetoothSocket? {
        try {
            return device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
        } catch (_: Exception) {}

        try {
            return device.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (_: Exception) {}

        try {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            return method.invoke(device, 16) as? BluetoothSocket
        } catch (_: Exception) {}

        return null
    }
}
