package com.openbuds.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Duotone
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.duotone.Ear
import com.adamglin.phosphoricons.duotone.SpeakerHigh
import com.adamglin.phosphoricons.fill.CheckCircle
import com.adamglin.phosphoricons.fill.Ear
import com.adamglin.phosphoricons.fill.WarningCircle
import com.adamglin.phosphoricons.regular.EarSlash
import com.openbuds.app.audio.FitTestTonePlayer
import com.openbuds.app.ui.icons.FreeBudsEarbudIllustration
import com.openbuds.app.ui.performLongPress
import com.openbuds.app.ui.performTick
import com.openbuds.app.ui.theme.BatteryLow
import com.openbuds.app.ui.theme.BatteryWarning
import com.openbuds.app.ui.theme.ChargingGreen
import com.openbuds.core.model.FitResult
import com.openbuds.core.model.FitTestResult
import com.openbuds.core.model.FitTestStatus
import com.openbuds.core.model.InEarStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Interactive full-fidelity modal dialog for running the Ear Tip Fit Test diagnostic.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun EartipFitTestDialog(
    inEarStatus: InEarStatus,
    fitTestResult: FitTestResult,
    onStartTest: suspend () -> Unit,
    onStopTest: suspend () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tonePlayer = remember { FitTestTonePlayer() }
    var testJob by remember { mutableStateOf<Job?>(null) }
    var localIsTesting by remember { mutableStateOf(false) }

    // Predictive back gesture intercept
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationState,
        onBackCompleted = {
            tonePlayer.stop()
            scope.launch { onStopTest() }
            onDismiss()
        }
    )

    // Cleanup audio resources if dismissed abruptly
    DisposableEffect(Unit) {
        onDispose {
            tonePlayer.stop()
            scope.launch { onStopTest() }
        }
    }

    val isActuallyTesting = localIsTesting || fitTestResult.status == FitTestStatus.TESTING
    val isCompleted = !isActuallyTesting && fitTestResult.status == FitTestStatus.COMPLETED

    fun startDiagnostic() {
        haptic.performLongPress()
        localIsTesting = true
        testJob?.cancel()
        testJob = scope.launch {
            onStartTest()
            tonePlayer.play {
                // When tone finishes, wait a brief window for final packet if still testing
            }
            // Timeout safeguard (5.5 seconds) so the UI never stays trapped in a spinning state
            delay(5500L)
            if (localIsTesting) {
                localIsTesting = false
            }
        }
    }

    fun stopDiagnostic() {
        haptic.performTick()
        testJob?.cancel()
        tonePlayer.stop()
        localIsTesting = false
        scope.launch { onStopTest() }
    }

    Dialog(
        onDismissRequest = {
            stopDiagnostic()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = PhosphorIcons.Duotone.Ear,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "Ear Tip Fit Test",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = when {
                        isActuallyTesting -> FitUiState.TESTING
                        isCompleted -> FitUiState.COMPLETED
                        else -> FitUiState.IDLE
                    },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "FitTestState"
                ) { state ->
                    when (state) {
                        FitUiState.IDLE -> {
                            IdleStateContent(
                                inEarStatus = inEarStatus,
                                onStartClick = { startDiagnostic() },
                                onCancelClick = onDismiss
                            )
                        }
                        FitUiState.TESTING -> {
                            TestingStateContent(
                                onStopClick = { stopDiagnostic() }
                            )
                        }
                        FitUiState.COMPLETED -> {
                            CompletedStateContent(
                                result = fitTestResult,
                                onRetestClick = { startDiagnostic() },
                                onDoneClick = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class FitUiState {
    IDLE,
    TESTING,
    COMPLETED
}

@Composable
private fun IdleStateContent(
    inEarStatus: InEarStatus,
    onStartClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val bothInEar = inEarStatus.leftInEar && inEarStatus.rightInEar

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Check the acoustic seal of your silicone tips to ensure optimal noise cancellation and deep bass.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // In-ear status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InEarPodCard(
                label = "Left Bud",
                isLeft = true,
                isInEar = inEarStatus.leftInEar,
                modifier = Modifier.weight(1f)
            )
            InEarPodCard(
                label = "Right Bud",
                isLeft = false,
                isInEar = inEarStatus.rightInEar,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!bothInEar) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BatteryWarning.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, BatteryWarning.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Fill.WarningCircle,
                        contentDescription = null,
                        tint = BatteryWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Insert both earbuds snugly before running the acoustic diagnostic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onStartClick,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Test")
            }
        }
    }
}

@Composable
private fun InEarPodCard(
    label: String,
    isLeft: Boolean,
    isInEar: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (isInEar) ChargingGreen.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FreeBudsEarbudIllustration(
                isLeft = isLeft,
                isActive = true,
                isInEar = isInEar,
                primaryColor = if (isInEar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                secondaryColor = MaterialTheme.colorScheme.secondary,
                surfaceColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isInEar) PhosphorIcons.Fill.Ear else PhosphorIcons.Regular.EarSlash,
                    contentDescription = null,
                    tint = if (isInEar) ChargingGreen else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isInEar) "In ear" else "Not detected",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isInEar) ChargingGreen else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun TestingStateContent(
    onStopClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AcousticPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                // Outer pulsing ripples
                val ripple1 = (pulseProgress * maxRadius)
                val alpha1 = (1f - pulseProgress).coerceIn(0f, 1f) * 0.45f
                drawCircle(
                    color = primaryColor.copy(alpha = alpha1),
                    radius = ripple1,
                    center = centerOffset,
                    style = Stroke(width = 3.dp.toPx())
                )

                val delayedProgress = (pulseProgress + 0.5f) % 1f
                val ripple2 = (delayedProgress * maxRadius)
                val alpha2 = (1f - delayedProgress).coerceIn(0f, 1f) * 0.45f
                drawCircle(
                    color = primaryColor.copy(alpha = alpha2),
                    radius = ripple2,
                    center = centerOffset,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = PhosphorIcons.Duotone.SpeakerHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Testing acoustic seal…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Playing acoustic test tone. Please remain still and do not speak.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onStopClick,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Stop")
        }
    }
}

@Composable
private fun CompletedStateContent(
    result: FitTestResult,
    onRetestClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val allGood = result.leftResult == FitResult.GOOD && result.rightResult == FitResult.GOOD

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultPodCard(
                label = "Left Bud",
                isLeft = true,
                result = result.leftResult,
                modifier = Modifier.weight(1f)
            )
            ResultPodCard(
                label = "Right Bud",
                isLeft = false,
                result = result.rightResult,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (allGood) ChargingGreen.copy(alpha = 0.12f) else BatteryWarning.copy(alpha = 0.12f),
            border = BorderStroke(
                1.dp,
                if (allGood) ChargingGreen.copy(alpha = 0.35f) else BatteryWarning.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (allGood) "Optimal Acoustic Seal" else "Seal Adjustment Advised",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (allGood) ChargingGreen else BatteryWarning
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (allGood) {
                        "Both earbuds have an airtight seal. You are experiencing the full noise cancellation depth and rich low-end frequency response."
                    } else {
                        "Sound leakage was detected. Try gently adjusting the earbud angle or swapping to a different ear tip size (S / M / L) from your packaging."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onRetestClick,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Test Again")
            }
            Button(
                onClick = onDoneClick,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun ResultPodCard(
    label: String,
    isLeft: Boolean,
    result: FitResult,
    modifier: Modifier = Modifier
) {
    val isGood = result == FitResult.GOOD
    val isPoor = result == FitResult.POOR

    val accentColor = when {
        isGood -> ChargingGreen
        isPoor -> BatteryWarning
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FreeBudsEarbudIllustration(
                isLeft = isLeft,
                isActive = true,
                isInEar = true,
                primaryColor = accentColor,
                secondaryColor = MaterialTheme.colorScheme.secondary,
                surfaceColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when {
                        isGood -> PhosphorIcons.Fill.CheckCircle
                        isPoor -> PhosphorIcons.Fill.WarningCircle
                        else -> PhosphorIcons.Regular.EarSlash
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = when {
                        isGood -> "Good seal"
                        isPoor -> "Poor seal"
                        else -> "Inconclusive"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
