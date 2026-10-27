package com.openbuds.app.ui.theme

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography as Material3Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GlacierPrimaryDark,
    onPrimary = GlacierOnPrimaryDark,
    primaryContainer = GlacierPrimaryContainerDark,
    onPrimaryContainer = GlacierOnPrimaryContainerDark,
    secondary = MintSecondaryDark,
    onSecondary = MintOnSecondaryDark,
    secondaryContainer = MintSecondaryContainerDark,
    onSecondaryContainer = MintOnSecondaryContainerDark,
    tertiary = LilacTertiaryDark,
    onTertiary = LilacOnTertiaryDark,
    tertiaryContainer = LilacTertiaryContainerDark,
    onTertiaryContainer = LilacOnTertiaryContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = GlacierPrimaryLight,
    onPrimary = GlacierOnPrimaryLight,
    primaryContainer = GlacierPrimaryContainerLight,
    onPrimaryContainer = GlacierOnPrimaryContainerLight,
    secondary = MintSecondaryLight,
    onSecondary = MintOnSecondaryLight,
    secondaryContainer = MintSecondaryContainerLight,
    onSecondaryContainer = MintOnSecondaryContainerLight,
    tertiary = LilacTertiaryLight,
    onTertiary = LilacOnTertiaryLight,
    tertiaryContainer = LilacTertiaryContainerLight,
    onTertiaryContainer = LilacOnTertiaryContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

@Immutable
class OpenBudsTheme(
    val colorScheme: ColorScheme = DarkColorScheme,
    val typography: Material3Typography = Typography,
    val shapes: Shapes = OpenBudsShapes,
    val motionScheme: MotionScheme = MotionScheme.expressive()
) {
    companion object {
        val colors: ColorScheme
            @Composable @ReadOnlyComposable
            get() = LocalOpenBudsTheme.current.colorScheme

        val typography: Material3Typography
            @Composable @ReadOnlyComposable
            get() = LocalOpenBudsTheme.current.typography

        val shapes: Shapes
            @Composable @ReadOnlyComposable
            get() = LocalOpenBudsTheme.current.shapes

        val motionScheme: MotionScheme
            @Composable @ReadOnlyComposable
            get() = MaterialTheme.motionScheme

        val styles: ComponentStyles = ComponentStyles
    }
}

internal val LocalOpenBudsTheme = staticCompositionLocalOf { OpenBudsTheme() }

@Composable
fun expressiveExpandTransition(): EnterTransition {
    val motion = MaterialTheme.motionScheme
    return expandVertically(animationSpec = motion.defaultSpatialSpec()) +
        fadeIn(animationSpec = motion.defaultEffectsSpec())
}

@Composable
fun expressiveShrinkTransition(): ExitTransition {
    val motion = MaterialTheme.motionScheme
    return shrinkVertically(animationSpec = motion.defaultSpatialSpec()) +
        fadeOut(animationSpec = motion.fastEffectsSpec())
}

@Composable
fun OpenBudsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    shapes: Shapes = OpenBudsShapes,
    motionScheme: MotionScheme = MotionScheme.expressive(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val openBudsThemeInstance = OpenBudsTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        motionScheme = motionScheme
    )

    CompositionLocalProvider(
        LocalOpenBudsTheme provides openBudsThemeInstance,
        LocalContentColor provides colorScheme.onBackground
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            shapes = shapes,
            typography = Typography,
            content = content
        )
    }
}
