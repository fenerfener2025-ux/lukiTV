package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AuroraPurple,
    secondary = AuroraCyan,
    tertiary = LiveRed,
    background = DeepSpaceBlue,
    surface = SurfaceBlue,
    onPrimary = Color.White,
    onSecondary = DeepSpaceBlue,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Always use dark color scheme for AuroraTV to ensure cinematic TV style
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
