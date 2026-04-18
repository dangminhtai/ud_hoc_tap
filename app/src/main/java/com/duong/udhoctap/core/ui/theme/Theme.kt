package com.duong.udhoctap.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple60,
    onPrimary = TextWhite,
    primaryContainer = Purple40,
    onPrimaryContainer = Purple80,
    secondary = Coral60,
    onSecondary = TextWhite,
    secondaryContainer = Coral40,
    onSecondaryContainer = Coral80,
    tertiary = Teal60,
    onTertiary = TextWhite,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGray,
    error = Red60,
    onError = TextWhite,
    outline = TextGray
)

private val LightColorScheme = lightColorScheme(
    primary = Purple60,
    onPrimary = Color.White,
    primaryContainer = Purple80,
    onPrimaryContainer = Purple20,
    secondary = Coral60,
    onSecondary = Color.White,
    secondaryContainer = Coral80,
    onSecondaryContainer = Coral40,
    tertiary = Teal40,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextMuted,
    error = Red60,
    onError = Color.White,
    outline = TextMuted
)

@Composable
fun UdHocTapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = view.context
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
