package com.kreggscode.bmr.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = PrimaryTeal,
    onPrimaryContainer = TextPrimaryLight,
    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = PrimaryPink,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = AccentCoral,
    onTertiary = Color.White,
    background = BackgroundLight1,
    onBackground = TextPrimaryLight,
    surface = GlassSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardBackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = Error,
    outline = GlassBorderLight,
    outlineVariant = GlassBorderLight.copy(alpha = 0.5f),
    scrim = Color.Black.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = PrimaryTeal.copy(alpha = 0.8f),
    onPrimaryContainer = TextPrimaryDark,
    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = PrimaryPink.copy(alpha = 0.8f),
    onSecondaryContainer = TextPrimaryDark,
    tertiary = AccentCoral,
    onTertiary = Color.White,
    background = BackgroundDark1,
    onBackground = TextPrimaryDark,
    surface = GlassSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardBackgroundDark,
    onSurfaceVariant = TextSecondaryDark,
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.2f),
    onErrorContainer = Error.copy(alpha = 0.9f),
    outline = GlassBorderDark,
    outlineVariant = GlassBorderDark.copy(alpha = 0.5f),
    scrim = Color.Black.copy(alpha = 0.7f)
)

@Composable
fun BMRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController?.isAppearanceLightStatusBars = !darkTheme
            insetsController?.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
