package com.openbuds.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCodecConfig
import android.bluetooth.BluetoothCodecStatus
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.openbuds.app.log.OpenBudsLog
import com.openbuds.core.driver.HeadphonesMatcher
import com.openbuds.core.model.A2dpCodecStatus
import com.openbuds.core.model.CodecPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reads and changes the A2DP codec used by the earbuds.
 *
 * The platform does not expose optional codec selection, so this class drives it through
 * `BluetoothA2dp` reflection (suppressed where the API is hidden) and observes
 * `CODEC_CONFIG_CHANGED` broadcasts for the resulting configuration.
 *
 * Lifecycle is explicit: [start] registers the receiver and binds the profile proxy, [close]
 * releases both. Nothing happens in the constructor.
 */
class A2dpCodecController(
    context: Context,
    private val matcher: HeadphonesMatcher
) {
    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _codecStatus = MutableStateFlow(A2dpCodecStatus(isEstimated = true))
    val codecStatus: StateFlow<A2dpCodecStatus> = _codecStatus.asStateFlow()

    private var a2dpProxy: BluetoothA2dp? = null
    private var targetDevice: BluetoothDevice? = null
    private var receiverRegistered = false
    private var preferredLdac = true

    private val codecReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                ACTION_CODEC_CONFIG_CHANGED -> onCodecConfigChanged(intent)
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> refresh()
            }
        }
    }

    // region Lifecycle

    fun start() {
        registerReceiver()
        bindProfileProxy()
    }

    fun close() {
        if (receiverRegistered) {
            runCatching { appContext.unregisterReceiver(codecReceiver) }
                .onFailure { OpenBudsLog.w("Unable to unregister the A2DP codec receiver", it) }
            receiverRegistered = false
        }
        a2dpProxy?.let { proxy ->
            runCatching { adapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy) }
                .onFailure { OpenBudsLog.w("Unable to release the A2DP profile proxy", it) }
        }
        a2dpProxy = null
        targetDevice = null
    }

    /** Points the controller at the device whose codec should be managed. */
    fun setTarget(device: BluetoothDevice?) {
        targetDevice = device
        if (device != null) refresh()
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(ACTION_CODEC_CONFIG_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            }
            ContextCompat.registerReceiver(
                appContext,
                codecReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            receiverRegistered = true
        } catch (e: Exception) {
            OpenBudsLog.w("Unable to register the A2DP codec receiver", e)
        }
    }

    private fun bindProfileProxy() {
        try {
            adapter?.getProfileProxy(
                appContext,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == BluetoothProfile.A2DP) {
                            a2dpProxy = proxy as? BluetoothA2dp
                            refresh()
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
        } catch (e: Exception) {
            OpenBudsLog.w("Unable to bind the A2DP profile proxy", e)
        }
    }

    // endregion

    // region Public operations

    /** Applies the preference and waits for the platform to confirm it. */
    suspend fun apply(preference: CodecPreference) {
        val preferLdac = preference == CodecPreference.LDAC_QUALITY
        preferredLdac = preferLdac
        _codecStatus.value = estimate(preferLdac)
        applyCodecPreference(preferLdac)

        val confirmed = withTimeoutOrNull(CONFIRM_TIMEOUT_MS) {
            codecStatus.first { it.isConfirmedFor(preference) }
        }
        if (confirmed == null) {
            OpenBudsLog.w("A2DP codec confirmation timed out for $preference")
            refresh(preferLdac)
        }
    }

    /** Re-reads the live codec using the last known preference. */
    fun refresh() {
        refresh(preferredLdac)
    }

    /** Re-reads the live codec, falling back to a preference-derived estimate. */
    @SuppressLint("MissingPermission")
    fun refresh(preferLdac: Boolean) {
        preferredLdac = preferLdac
        val proxy = a2dpProxy
        if (proxy != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val device = resolveTarget(proxy)
            if (device != null) {
                val config = runCatching {
                    val getCodecStatus = proxy.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java)
                    (getCodecStatus.invoke(proxy, device) as? BluetoothCodecStatus)?.codecConfig
                }.onFailure {
                    OpenBudsLog.w("Unable to read the A2DP codec status", it)
                }.getOrNull()

                if (config != null && applyCodecConfig(config)) return
            }
        }
        _codecStatus.value = estimate(preferLdac)
    }

    // endregion

    @SuppressLint("MissingPermission")
    private fun onCodecConfigChanged(intent: Intent) {
        val eventDevice = IntentCompat.getParcelableExtra(
            intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        )
        val currentAddress = targetDevice?.address
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
            if (config != null && applyCodecConfig(config)) return
        }

        refresh()
    }

    @SuppressLint("MissingPermission")
    private fun resolveTarget(proxy: BluetoothA2dp): BluetoothDevice? {
        targetDevice?.let { return it }
        return runCatching {
            proxy.connectedDevices.firstOrNull { matcher.matches(it.name) }
        }.onFailure {
            OpenBudsLog.w("Unable to list connected A2DP devices", it)
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun applyCodecPreference(preferLdac: Boolean) {
        val proxy = a2dpProxy ?: return
        val device = resolveTarget(proxy) ?: return

        val preference = if (preferLdac) OPTIONAL_CODECS_PREF_ENABLED else OPTIONAL_CODECS_PREF_DISABLED
        runCatching {
            val setOptionalCodecs = proxy.javaClass.getMethod(
                "setOptionalCodecsEnabled",
                BluetoothDevice::class.java,
                Int::class.javaPrimitiveType
            )
            setOptionalCodecs.invoke(proxy, device, preference)
        }.onFailure { OpenBudsLog.w("Unable to set the optional-codec preference", it) }

        val toggle = if (preferLdac) "enableOptionalCodecs" else "disableOptionalCodecs"
        runCatching {
            val toggleMethod = proxy.javaClass.getMethod(toggle, BluetoothDevice::class.java)
            toggleMethod.invoke(proxy, device)
        }.onFailure { OpenBudsLog.w("Unable to toggle optional codecs", it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val preferredConfig = buildPreferredCodecConfig(proxy, device, preferLdac) ?: return@runCatching
                val setConfig = proxy.javaClass.getMethod(
                    "setCodecConfigPreference",
                    BluetoothDevice::class.java,
                    BluetoothCodecConfig::class.java
                )
                setConfig.invoke(proxy, device, preferredConfig)
            }.onFailure { OpenBudsLog.w("Unable to set the A2DP codec preference", it) }
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
            val getCodecStatus = proxy.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java)
            val status = getCodecStatus.invoke(proxy, device) as? BluetoothCodecStatus
            status?.codecsSelectableCapabilities?.firstOrNull { it.codecType == targetCodecType }
        }.onFailure {
            OpenBudsLog.w("Unable to read selectable codec capabilities", it)
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

    /** Stores a configuration read from A2DP. Returns false when [config] is not usable. */
    @Suppress("DEPRECATION")
    private fun applyCodecConfig(config: BluetoothCodecConfig): Boolean {
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

        _codecStatus.value = A2dpCodecStatus(
            activeCodecName = codecName,
            sampleRateLabel = sampleRate,
            bitsPerSampleLabel = bitDepth,
            bitrateLabel = bitrate,
            isLdacActive = isLdac,
            isEstimated = false
        )
        return true
    }

    /** Preference-derived values used until the platform reports the real configuration. */
    private fun estimate(preferLdac: Boolean): A2dpCodecStatus =
        if (preferLdac) {
            A2dpCodecStatus(
                activeCodecName = "LDAC",
                sampleRateLabel = "96.0 kHz",
                bitsPerSampleLabel = "24-bit",
                bitrateLabel = "990 kbps Hi-Res",
                isLdacActive = true,
                isEstimated = true
            )
        } else {
            A2dpCodecStatus(
                activeCodecName = "AAC",
                sampleRateLabel = "48.0 kHz",
                bitsPerSampleLabel = "16-bit",
                bitrateLabel = "256 kbps Stable",
                isLdacActive = false,
                isEstimated = true
            )
        }

    private companion object {
        const val ACTION_CODEC_CONFIG_CHANGED =
            "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED"
        const val EXTRA_CODEC_STATUS = "android.bluetooth.extra.CODEC_STATUS"
        const val OPTIONAL_CODECS_PREF_DISABLED = 0
        const val OPTIONAL_CODECS_PREF_ENABLED = 1
        const val CONFIRM_TIMEOUT_MS = 1_500L
    }
}
