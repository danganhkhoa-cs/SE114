package com.example.se114.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = AppTealDark,
    onPrimary = Color.White,
    primaryContainer = AppTealLight,
    onPrimaryContainer = AppTealDark,

    secondary = AppTealLight,
    onSecondary = AppTealDark,
    secondaryContainer = AppTealLight.copy(alpha = 0.3f),
    onSecondaryContainer = AppTealDark,

    tertiary = AppTealBlob,
    onTertiary = Color.White,

    background = Color.White,
    onBackground = Color(0xFF1C1B1F),

    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF44464F),

    outline = Color(0xFFCAC4D0),
    outlineVariant = AppTealLight.copy(alpha = 0.3f),

    error = Color(0xFFB3261E),
    onError = Color.White,
)

// Dark Theme Colors - Tinh chỉnh để không bị "đen thui"
private val DarkColorScheme = darkColorScheme(
    primary = AppTealNeon,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = AppTealNeon,

    secondary = AppTealBlob,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF1E4E55),
    onSecondaryContainer = AppTealNeon,

    tertiary = AppTealLight,
    onTertiary = Color(0xFF00363D),

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkOutline,
    outlineVariant = Color(0xFF3F484A),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun SE114Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    @Suppress("NewApi")
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}