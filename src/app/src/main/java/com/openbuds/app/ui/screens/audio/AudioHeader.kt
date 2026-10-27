package com.openbuds.app.ui.screens.audio

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.openbuds.app.ui.components.ExpressiveScreenHeader
import com.openbuds.app.ui.components.ExpressiveStatusPill
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.core.model.A2dpCodecStatus

/** Audio screen title with a live codec telemetry capsule. */
@Composable
internal fun AudioHeader(
    a2dpCodecStatus: A2dpCodecStatus,
    isConnected: Boolean
) {
    ExpressiveScreenHeader(
        title = "Audio",
        trailing = {
            ExpressiveStatusPill(
                text = "${a2dpCodecStatus.activeCodecName} • ${a2dpCodecStatus.sampleRateLabel} • ${a2dpCodecStatus.bitsPerSampleLabel}",
                dotColor = if (isConnected) ChargingGreen else MaterialTheme.colorScheme.outline,
                containerColor = if (isConnected && a2dpCodecStatus.isLdacActive) {
                    ChargingGreen.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    )
}
