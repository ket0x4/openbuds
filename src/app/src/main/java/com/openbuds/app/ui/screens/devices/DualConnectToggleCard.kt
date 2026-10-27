package com.openbuds.app.ui.screens.devices

import androidx.compose.runtime.Composable
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.duotone.Devices
import com.openbuds.app.ui.components.ExpressiveToggleCard

/** Master switch for connecting to two hosts at once. */
@Composable
internal fun DualConnectToggleCard(
    enabled: Boolean,
    isConnected: Boolean,
    onDualConnectToggled: (Boolean) -> Unit
) {
    ExpressiveToggleCard(
        checked = enabled,
        onCheckedChange = onDualConnectToggled,
        title = "Dual Connect",
        subtitle = "Connect to two devices simultaneously",
        icon = PhosphorIcons.Duotone.Devices,
        enabled = isConnected
    )
}
