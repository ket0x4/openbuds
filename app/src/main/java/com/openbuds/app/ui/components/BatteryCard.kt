package com.openbuds.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Ear
import com.adamglin.phosphoricons.fill.Lightning
import com.adamglin.phosphoricons.regular.EarSlash
import com.openbuds.app.ui.icons.FreeBudsEarbudIllustration
import com.openbuds.app.ui.icons.FreeBudsPebbleCaseIllustration
import com.openbuds.app.ui.theme.BatteryLow
import com.openbuds.app.ui.theme.BatteryWarning
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.core.model.BatteryStatus
import com.openbuds.core.model.InEarStatus

private enum class HardwarePodType {
    LEFT_BUD,
    CASE,
    RIGHT_BUD
}

@Composable
fun ExpressiveBatterySection(
    battery: BatteryStatus,
    inEarStatus: InEarStatus,
    modifier: Modifier = Modifier,
    isConnected: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HardwarePod(
            podType = HardwarePodType.LEFT_BUD,
            label = "Left",
            percent = if (isConnected) battery.leftPercent else null,
            isCharging = isConnected && battery.isLeftCharging,
            isInEar = isConnected && inEarStatus.leftInEar,
            isConnected = isConnected,
            modifier = Modifier.weight(1f)
        )

        HardwarePod(
            podType = HardwarePodType.CASE,
            label = "Case",
            percent = if (isConnected) battery.casePercent else null,
            isCharging = isConnected && battery.isCaseCharging,
            isInEar = false,
            isConnected = isConnected,
            modifier = Modifier.weight(1f)
        )

        HardwarePod(
            podType = HardwarePodType.RIGHT_BUD,
            label = "Right",
            percent = if (isConnected) battery.rightPercent else null,
            isCharging = isConnected && battery.isRightCharging,
            isInEar = isConnected && inEarStatus.rightInEar,
            isConnected = isConnected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HardwarePod(
    podType: HardwarePodType,
    label: String,
    percent: Int?,
    isCharging: Boolean,
    isInEar: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val isBud = podType != HardwarePodType.CASE
    val isHighlighted = isConnected && (isInEar || isCharging)
    val motion = MaterialTheme.motionScheme

    val animatedProgress by animateFloatAsState(
        targetValue = if (isConnected && percent != null) (percent.coerceIn(0, 100) / 100f) else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "PodBatteryProgress"
    )

    val podCornerRadius by animateDpAsState(
        targetValue = if (isHighlighted) 20.dp else 28.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "PodCornerRadius"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            !isConnected -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
            isInEar -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            isCharging -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            percent != null -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
            else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "PodContainerColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !isConnected -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            isCharging -> ChargingGreen.copy(alpha = 0.55f)
            isInEar -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "PodBorderColor"
    )

    val accentColor = when {
        !isConnected || percent == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        isCharging -> ChargingGreen
        percent <= 15 -> BatteryLow
        percent <= 30 -> BatteryWarning
        isInEar -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val hardwarePrimaryTint = when {
        !isConnected || percent == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        isInEar -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val hardwareSecondaryTint = when {
        !isConnected || percent == null -> MaterialTheme.colorScheme.outline
        isInEar -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(podCornerRadius),
        color = containerColor,
        border = BorderStroke(
            width = if (isHighlighted) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PodStatusBadge(
                isBud = isBud,
                isConnected = isConnected,
                isCharging = isCharging,
                isInEar = isInEar,
                hasReading = percent != null
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                when (podType) {
                    HardwarePodType.LEFT_BUD -> FreeBudsEarbudIllustration(
                        isLeft = true,
                        isActive = isConnected && percent != null,
                        isInEar = isInEar,
                        primaryColor = hardwarePrimaryTint,
                        secondaryColor = hardwareSecondaryTint,
                        surfaceColor = containerColor,
                        modifier = Modifier.size(54.dp)
                    )

                    HardwarePodType.RIGHT_BUD -> FreeBudsEarbudIllustration(
                        isLeft = false,
                        isActive = isConnected && percent != null,
                        isInEar = isInEar,
                        primaryColor = hardwarePrimaryTint,
                        secondaryColor = hardwareSecondaryTint,
                        surfaceColor = containerColor,
                        modifier = Modifier.size(54.dp)
                    )

                    HardwarePodType.CASE -> FreeBudsPebbleCaseIllustration(
                        isActive = isConnected && percent != null,
                        isCharging = isCharging,
                        primaryColor = hardwarePrimaryTint,
                        secondaryColor = hardwareSecondaryTint,
                        surfaceColor = containerColor,
                        ledColor = when {
                            isCharging -> ChargingGreen
                            (percent ?: 100) <= 20 -> BatteryLow
                            (percent ?: 100) <= 40 -> BatteryWarning
                            else -> ChargingGreen
                        },
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isConnected && percent != null) "$percent" else "--",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = if (isConnected && percent != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    }
                )
                if (isConnected && percent != null) {
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 1.dp, top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                amplitude = if (isCharging) {
                    WavyProgressIndicatorDefaults.indicatorAmplitude
                } else {
                    { 0f }
                },
                wavelength = 16.dp,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .height(if (isCharging) 10.dp else 6.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                style = if (isHighlighted) {
                    MaterialTheme.typography.labelMediumEmphasized
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun PodStatusBadge(
    isBud: Boolean,
    isConnected: Boolean,
    isCharging: Boolean,
    isInEar: Boolean,
    hasReading: Boolean
) {
    if (!isConnected || (!isCharging && !hasReading)) {
        Spacer(modifier = Modifier.height(18.dp))
        return
    }

    val badgeBg: Color
    val badgeFg: Color
    val badgeText: String

    when {
        isCharging -> {
            badgeBg = ChargingGreen.copy(alpha = 0.18f)
            badgeFg = ChargingGreen
            badgeText = "Charging"
        }
        isBud && isInEar -> {
            badgeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            badgeFg = MaterialTheme.colorScheme.primary
            badgeText = "In Ear"
        }
        isBud -> {
            badgeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            badgeFg = MaterialTheme.colorScheme.onSurfaceVariant
            badgeText = "Out"
        }
        else -> {
            Spacer(modifier = Modifier.height(18.dp))
            return
        }
    }

    val motion = MaterialTheme.motionScheme
    val animatedBadgeBg by animateColorAsState(
        targetValue = badgeBg,
        animationSpec = motion.fastEffectsSpec(),
        label = "PodBadgeBg"
    )
    val animatedBadgeFg by animateColorAsState(
        targetValue = badgeFg,
        animationSpec = motion.fastEffectsSpec(),
        label = "PodBadgeFg"
    )

    Surface(
        shape = CircleShape,
        color = animatedBadgeBg,
        contentColor = animatedBadgeFg
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(animationSpec = motion.fastSpatialSpec())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            when {
                isCharging -> Icon(
                    imageVector = PhosphorIcons.Fill.Lightning,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
                isInEar -> Icon(
                    imageVector = PhosphorIcons.Fill.Ear,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
                else -> Icon(
                    imageVector = PhosphorIcons.Regular.EarSlash,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmallEmphasized,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
