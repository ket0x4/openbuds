package com.openbuds.app.ui.screens.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.CaretDown
import com.adamglin.phosphoricons.duotone.Star
import com.adamglin.phosphoricons.duotone.Trash
import com.adamglin.phosphoricons.fill.CheckCircle
import com.adamglin.phosphoricons.fill.Star
import com.adamglin.phosphoricons.regular.Link
import com.adamglin.phosphoricons.regular.LinkBreak
import com.openbuds.app.ui.theme.BatteryLow
import com.openbuds.app.ui.theme.ExpressiveIconBadge
import com.openbuds.app.ui.theme.expressiveExpandTransition
import com.openbuds.app.ui.theme.expressiveShrinkTransition
import com.openbuds.core.model.DualConnectDevice

/**
 * Expandable card for one paired host: identity and connection status on the surface, with
 * primary/auto-connect switches and unpair tucked behind the disclosure caret.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
internal fun DualConnectDeviceCard(
    device: DualConnectDevice,
    isExpanded: Boolean,
    isConnected: Boolean,
    onExpandToggle: () -> Unit,
    onConnectToggle: () -> Unit,
    onPreferredToggle: (Boolean) -> Unit,
    onAutoConnectToggle: (Boolean) -> Unit,
    onRequestUnpair: () -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val caretRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = motion.fastSpatialSpec(),
        label = "DeviceCaretRotation"
    )
    val cardCornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 28.dp else 22.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "DeviceCardRadius"
    )
    val visuals = rememberDeviceStatusVisuals(device)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cardCornerRadius))
            .clickable(onClick = onExpandToggle),
        shape = RoundedCornerShape(cardCornerRadius),
        color = visuals.cardContainerColor,
        border = BorderStroke(
            width = if (device.isPreferred) 1.5.dp else 1.dp,
            color = visuals.cardBorderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeviceIdentity(device = device, visuals = visuals)

                DeviceActions(
                    device = device,
                    isConnected = isConnected,
                    caretRotation = caretRotation,
                    onConnectToggle = onConnectToggle
                )
            }

            DeviceSettings(
                visible = isExpanded,
                device = device,
                isConnected = isConnected,
                onPreferredToggle = onPreferredToggle,
                onAutoConnectToggle = onAutoConnectToggle,
                onRequestUnpair = onRequestUnpair
            )
        }
    }
}

@Composable
private fun RowScope.DeviceIdentity(
    device: DualConnectDevice,
    visuals: DeviceStatusVisuals
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpressiveIconBadge(
            icon = visuals.icon,
            polygon = visuals.polygon,
            containerColor = visuals.badgeContainerColor,
            contentColor = visuals.badgeContentColor,
            size = 40.dp,
            iconSize = 20.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (device.isPreferred) {
                    PrimaryDeviceBadge()
                }
            }

            Text(
                text = visuals.label,
                style = MaterialTheme.typography.labelSmallEmphasized,
                color = visuals.labelColor
            )
        }
    }
}

/** Small tertiary capsule marking the preferred host. */
@Composable
private fun PrimaryDeviceBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = PhosphorIcons.Fill.Star,
                contentDescription = "Primary",
                modifier = Modifier.size(9.dp)
            )
            Text(
                text = "Primary",
                style = MaterialTheme.typography.labelSmallEmphasized,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun DeviceActions(
    device: DualConnectDevice,
    isConnected: Boolean,
    caretRotation: Float,
    onConnectToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (device.isConnected) {
            FilledTonalButton(
                onClick = onConnectToggle,
                enabled = isConnected,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.LinkBreak,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "Disconnect",
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }
        } else {
            Button(
                onClick = onConnectToggle,
                enabled = isConnected,
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Link,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "Connect",
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }
        }

        Icon(
            imageVector = PhosphorIcons.Bold.CaretDown,
            contentDescription = "Options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(16.dp)
                .rotate(caretRotation)
        )
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun DeviceSettings(
    visible: Boolean,
    device: DualConnectDevice,
    isConnected: Boolean,
    onPreferredToggle: (Boolean) -> Unit,
    onAutoConnectToggle: (Boolean) -> Unit,
    onRequestUnpair: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expressiveExpandTransition(),
        exit = expressiveShrinkTransition()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            DeviceSettingSwitchRow(
                title = "Primary device",
                checked = device.isPreferred,
                onCheckedChange = onPreferredToggle,
                enabled = isConnected,
                index = 0,
                count = 2,
                icon = PhosphorIcons.Duotone.Star,
                iconTint = MaterialTheme.colorScheme.tertiary,
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
            )

            DeviceSettingSwitchRow(
                title = "Auto-connect",
                checked = device.autoConnect,
                onCheckedChange = onAutoConnectToggle,
                enabled = isConnected,
                index = 1,
                count = 2,
                icon = PhosphorIcons.Fill.CheckCircle,
                iconTint = MaterialTheme.colorScheme.primary,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )

            MacAddressAndUnpair(
                mac = device.formattedMac,
                enabled = isConnected,
                onRequestUnpair = onRequestUnpair
            )
        }
    }
}

/** One switch row inside the connected settings list group. */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
private fun DeviceSettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    index: Int,
    count: Int,
    icon: ImageVector,
    iconTint: Color,
    selectedContainerColor: Color
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
            selectedContainerColor = selectedContainerColor
        ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMediumEmphasized
            )
        }
    )
}

@Composable
private fun MacAddressAndUnpair(
    mac: String,
    enabled: Boolean,
    onRequestUnpair: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = mac,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )

        FilledTonalButton(
            onClick = onRequestUnpair,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = BatteryLow.copy(alpha = 0.16f),
                contentColor = BatteryLow
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = PhosphorIcons.Duotone.Trash,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.size(5.dp))
            Text(
                text = "Unpair",
                style = MaterialTheme.typography.labelSmallEmphasized
            )
        }
    }
}
