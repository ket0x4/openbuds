package com.openbuds.core.model

data class InEarStatus(
    val leftInEar: Boolean = false,
    val rightInEar: Boolean = false
)

enum class GestureType {
    DOUBLE_TAP_LEFT,
    DOUBLE_TAP_RIGHT,
    TRIPLE_TAP_LEFT,
    TRIPLE_TAP_RIGHT,
    HOLD_BOTH,
    SWIPE
}

enum class GestureAction {
    NONE,
    PLAY_PAUSE,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    VOICE_ASSISTANT,
    CYCLE_ANC,
    ADJUST_VOLUME
}

/**
 * Which ANC modes are cycled when pinching/holding the earbud stem.
 * Maps to Huawei MBB command 0x2b 0x18 / 0x19:
 * 1 = Off & ANC
 * 2 = Off, ANC & Awareness (All 3)
 * 3 = ANC & Awareness
 * 4 = Off & Awareness
 */
enum class HoldAncModes(val code: Int) {
    OFF_AND_ANC(1),
    ALL_MODES(2),
    ANC_AND_AWARENESS(3),
    OFF_AND_AWARENESS(4);

    companion object {
        fun fromCode(code: Int): HoldAncModes =
            entries.firstOrNull { it.code == code } ?: ALL_MODES
    }
}

data class GestureSettings(
    val doubleTapLeft: GestureAction = GestureAction.PLAY_PAUSE,
    val doubleTapRight: GestureAction = GestureAction.PLAY_PAUSE,
    val tripleTapLeft: GestureAction = GestureAction.NEXT_TRACK,
    val tripleTapRight: GestureAction = GestureAction.NEXT_TRACK,
    val holdBoth: GestureAction = GestureAction.CYCLE_ANC,
    val holdAncModes: HoldAncModes = HoldAncModes.ALL_MODES,
    val swipe: GestureAction = GestureAction.ADJUST_VOLUME
)

enum class EqPreset {
    DEFAULT,
    BASS_BOOST,
    TREBLE_BOOST,
    VOICES
}

/**
 * Codec / Sound Quality preference stored on the earbuds (MBB 0x2b 0xa2 / 0xa3).
 * - AAC_CONNECTION (0): Prioritize stable Bluetooth connection (AAC / SBC)
 * - LDAC_QUALITY (1): Prioritize Hi-Res sound quality (LDAC up to 990 kbps)
 */
enum class CodecPreference(val code: Int) {
    AAC_CONNECTION(0),
    LDAC_QUALITY(1);

    companion object {
        fun fromCode(code: Int): CodecPreference =
            if (code == 1) LDAC_QUALITY else AAC_CONNECTION
    }
}

data class A2dpCodecStatus(
    val activeCodecName: String = "AAC",
    val sampleRateLabel: String = "48.0 kHz",
    val bitsPerSampleLabel: String = "16-bit",
    val bitrateLabel: String = "256 kbps",
    val isLdacActive: Boolean = false
)

data class AudioSettings(
    val eqPreset: EqPreset = EqPreset.DEFAULT,
    val lowLatencyMode: Boolean = false,
    val soundQualityPreference: Boolean = true
) {
    val codecPreference: CodecPreference
        get() = if (soundQualityPreference) CodecPreference.LDAC_QUALITY else CodecPreference.AAC_CONNECTION
}

enum class DualConnectConnectionState {
    DISCONNECTED,
    CONNECTED,
    PLAYING
}

data class DualConnectDevice(
    val macAddress: String,
    val name: String,
    val connectionState: DualConnectConnectionState = DualConnectConnectionState.DISCONNECTED,
    val isPreferred: Boolean = false,
    val autoConnect: Boolean = true
) {
    val isConnected: Boolean
        get() = connectionState != DualConnectConnectionState.DISCONNECTED

    val formattedMac: String
        get() {
            val clean = macAddress.replace(":", "").uppercase()
            return if (clean.length == 12) {
                clean.chunked(2).joinToString(":")
            } else {
                macAddress.uppercase()
            }
        }
}

data class DualConnectState(
    val enabled: Boolean = true,
    val devices: List<DualConnectDevice> = emptyList(),
    val preferredDeviceMac: String = ""
) {
    val connectedCount: Int
        get() = devices.count { it.isConnected }
}

data class DeviceSettings(
    val autoPause: Boolean = true,
    val gestures: GestureSettings = GestureSettings(),
    val audio: AudioSettings = AudioSettings()
)

data class DeviceInfo(
    val name: String,
    val macAddress: String,
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val serialNumber: String = "",
    val subModel: String = "",
    val modelName: String = "HUAWEI FreeBuds 5i"
)

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val info: DeviceInfo) : ConnectionState
    data class Error(val message: String) : ConnectionState
}
