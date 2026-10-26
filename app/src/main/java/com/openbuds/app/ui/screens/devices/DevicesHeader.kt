package com.openbuds.app.ui.screens.devices

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.ArrowsClockwise
import com.openbuds.app.ui.components.ExpressiveScreenHeader
import com.openbuds.app.ui.performTick

/**
 * Devices screen title with the dual-connect refresh action.
 *
 * [refreshRotation] is supplied by the caller so the spin survives recomposition and
 * reconnecting state changes.
 */
@Composable
internal fun DevicesHeader(
    isConnected: Boolean,
    refreshRotation: Float,
    onRefresh: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val refreshAction: (@Composable () -> Unit)? = if (isConnected) {
        {
            FilledTonalIconButton(
                onClick = {
                    haptic.performTick()
                    onRefresh()
                },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Bold.ArrowsClockwise,
                    contentDescription = "Refresh devices",
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(refreshRotation)
                )
            }
        }
    } else {
        null
    }

    ExpressiveScreenHeader(
        title = "Devices",
        trailing = refreshAction
    )
}
