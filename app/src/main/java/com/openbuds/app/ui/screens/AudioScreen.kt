package com.openbuds.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openbuds.app.ui.components.ExpressiveScreenScaffold
import com.openbuds.app.ui.performLongPress
import com.openbuds.app.ui.performTick
import com.openbuds.app.ui.screens.audio.AudioHeader
import com.openbuds.app.ui.screens.audio.CodecSelectionCard
import com.openbuds.app.ui.screens.audio.EqualizerSectionCard
import com.openbuds.app.ui.screens.audio.LowLatencyCard
import com.openbuds.app.ui.theme.OpenBudsTheme
import com.openbuds.core.model.A2dpCodecStatus
import com.openbuds.core.model.AudioSettings
import com.openbuds.core.model.CodecPreference
import com.openbuds.core.model.EqPreset

/**
 * Audio tab: equalizer presets, Bluetooth codec choice and low-latency gaming mode.
 */
@Composable
fun AudioScreen(
    audioSettings: AudioSettings,
    isConnected: Boolean,
    onEqPresetSelected: (EqPreset) -> Unit,
    onLowLatencyToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    a2dpCodecStatus: A2dpCodecStatus = A2dpCodecStatus(
        activeCodecName = if (audioSettings.soundQualityPreference) "LDAC" else "AAC",
        sampleRateLabel = if (audioSettings.soundQualityPreference) "96.0 kHz" else "48.0 kHz",
        bitsPerSampleLabel = if (audioSettings.soundQualityPreference) "24-bit" else "16-bit",
        bitrateLabel = if (audioSettings.soundQualityPreference) "990 kbps" else "256 kbps",
        isLdacActive = audioSettings.soundQualityPreference
    ),
    onCodecPreferenceSelected: (CodecPreference) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    ExpressiveScreenScaffold(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp)
    ) {
        AudioHeader(
            a2dpCodecStatus = a2dpCodecStatus,
            isConnected = isConnected
        )

        EqualizerSectionCard(
            selectedPreset = audioSettings.eqPreset,
            isConnected = isConnected,
            onEqPresetSelected = { preset ->
                haptic.performLongPress()
                onEqPresetSelected(preset)
            }
        )

        CodecSelectionCard(
            codecPreference = audioSettings.codecPreference,
            a2dpCodecStatus = a2dpCodecStatus,
            isConnected = isConnected,
            onCodecSelected = { preference ->
                haptic.performLongPress()
                onCodecPreferenceSelected(preference)
            }
        )

        LowLatencyCard(
            enabled = audioSettings.lowLatencyMode,
            isConnected = isConnected,
            onLowLatencyToggled = { enabled ->
                haptic.performTick()
                onLowLatencyToggled(enabled)
            }
        )
    }
}

@Preview(name = "Audio Screen", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun AudioScreenPreview() {
    OpenBudsTheme(darkTheme = true, dynamicColor = false) {
        AudioScreen(
            audioSettings = AudioSettings(
                eqPreset = EqPreset.BASS_BOOST,
                lowLatencyMode = false,
                soundQualityPreference = true
            ),
            isConnected = true,
            onEqPresetSelected = {},
            onCodecPreferenceSelected = {},
            onLowLatencyToggled = {}
        )
    }
}
