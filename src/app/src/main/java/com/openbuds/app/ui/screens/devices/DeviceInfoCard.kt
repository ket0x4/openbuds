package com.openbuds.app.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.duotone.Cpu
import com.openbuds.app.ui.components.ExpressiveSectionHeader
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.core.model.DeviceInfo

/** Firmware, hardware and Bluetooth address readout for the connected earbuds. */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
internal fun DeviceInfoCard(
    deviceInfo: DeviceInfo,
    isConnected: Boolean
) {
    ExpressiveStyleableCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpressiveSectionHeader(
                icon = PhosphorIcons.Duotone.Cpu,
                title = "Device Info",
                polygon = MaterialShapes.Sunny,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                titleStyle = MaterialTheme.typography.titleSmallEmphasized
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoMetricPill(
                        label = "Firmware",
                        value = if (isConnected && deviceInfo.firmwareVersion.isNotBlank()) {
                            deviceInfo.firmwareVersion
                        } else {
                            "--"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InfoMetricPill(
                        label = "Hardware",
                        value = if (isConnected && deviceInfo.hardwareVersion.isNotBlank()) {
                            deviceInfo.hardwareVersion
                        } else {
                            "--"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                InfoMetricPill(
                    label = "Bluetooth MAC",
                    value = if (
                        isConnected &&
                        deviceInfo.macAddress.isNotBlank() &&
                        deviceInfo.macAddress != "00:00:00:00:00:00"
                    ) {
                        deviceInfo.macAddress
                    } else {
                        "--"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun InfoMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
