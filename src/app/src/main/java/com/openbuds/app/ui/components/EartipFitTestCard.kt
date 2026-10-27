package com.openbuds.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.duotone.Ear
import com.adamglin.phosphoricons.fill.CheckCircle
import com.adamglin.phosphoricons.fill.WarningCircle
import com.openbuds.app.ui.performTick
import com.openbuds.app.ui.theme.BatteryWarning
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.app.ui.theme.ExpressiveStyleableCard
import com.openbuds.core.model.FitResult
import com.openbuds.core.model.FitTestResult
import com.openbuds.core.model.FitTestStatus

/**
 * Material 3 Expressive dashboard card for the ear tip fit test diagnostic.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun ExpressiveEartipFitCard(
    fitTestResult: FitTestResult,
    onOpenFitTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    ExpressiveStyleableCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable {
                haptic.performTick()
                onOpenFitTest()
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExpressiveSectionHeader(
                icon = PhosphorIcons.Duotone.Ear,
                title = "Ear Tip Fit Test",
                polygon = MaterialShapes.Cookie4Sided,
                trailing = {
                    if (fitTestResult.status == FitTestStatus.COMPLETED) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FitResultMiniBadge(label = "L", result = fitTestResult.leftResult)
                            FitResultMiniBadge(label = "R", result = fitTestResult.rightResult)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (fitTestResult.status == FitTestStatus.COMPLETED) {
                    when {
                        fitTestResult.leftResult == FitResult.GOOD && fitTestResult.rightResult == FitResult.GOOD ->
                            "Airtight seal confirmed on both earbuds for maximum ANC depth and bass punch."
                        fitTestResult.leftResult == FitResult.POOR || fitTestResult.rightResult == FitResult.POOR ->
                            "Seal leakage detected on one or both earbuds. Adjust fit or test another tip size."
                        else ->
                            "Acoustic seal tested. Tap to re-test anytime."
                    }
                } else {
                    "Test the acoustic seal of your silicone tips to ensure optimal noise cancellation and punchy bass."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            FilledTonalButton(
                onClick = {
                    haptic.performTick()
                    onOpenFitTest()
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (fitTestResult.status == FitTestStatus.COMPLETED) "Test Again" else "Test Fit",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun FitResultMiniBadge(
    label: String,
    result: FitResult
) {
    val (bgColor, contentColor, icon) = when (result) {
        FitResult.GOOD -> Triple(
            ChargingGreen.copy(alpha = 0.2f),
            ChargingGreen,
            PhosphorIcons.Fill.CheckCircle
        )
        FitResult.POOR -> Triple(
            BatteryWarning.copy(alpha = 0.2f),
            BatteryWarning,
            PhosphorIcons.Fill.WarningCircle
        )
        FitResult.UNKNOWN -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            null
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = "$label: ${result.name.lowercase().replaceFirstChar { it.uppercase() }}",
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
