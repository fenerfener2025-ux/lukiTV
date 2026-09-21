package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val palette = themeConfig.palette
    val dynamicColorScheme = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        tertiary = palette.liveBadge,
        background = palette.background,
        surface = palette.surface,
        surfaceVariant = palette.surfaceLight,
        onPrimary = Color.White,
        onSecondary = palette.background,
        onBackground = Color.White,
        onSurface = Color.White
    )

    CompositionLocalProvider(LocalThemeConfig provides themeConfig) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = Typography,
            content = content
        )
    }
}

