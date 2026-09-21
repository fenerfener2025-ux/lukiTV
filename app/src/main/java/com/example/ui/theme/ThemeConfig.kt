package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect

/**
 * 3 Seçilebilir Renk Paleti:
 * 1. FENERBAHCE: Koyu Lacivert & Kanarya Sarısı
 * 2. CYBER_NEON: Gece Siyahı & Neon Camgöbeği (Cyberpunk)
 * 3. EMERALD_NIGHT: Koyu Zümrüt & Nane Yeşili
 */
enum class AppColorPalette(
    val title: String,
    val subtitle: String,
    val primary: Color,
    val secondary: Color,      // Vurgu rengi
    val background: Color,
    val surface: Color,
    val surfaceLight: Color,
    val focusGlow: Color,
    val liveBadge: Color = Color(0xFFFF3B5C)
) {
    FENERBAHCE(
        title = "Sarı-Lacivert",
        subtitle = "Şampiyon Gece & Altın Sarı",
        primary = Color(0xFF0F2C5F),
        secondary = Color(0xFFFED200),
        background = Color(0xFF030A1D),
        surface = Color(0xFF09142F),
        surfaceLight = Color(0xFF14244F),
        focusGlow = Color(0x4DFED200)
    ),
    CYBER_NEON(
        title = "Siber Akua",
        subtitle = "Derin İndigo & Neon Camgöbeği",
        primary = Color(0xFF101E3C),
        secondary = Color(0xFF00F2FE),
        background = Color(0xFF060C18),
        surface = Color(0xFF0C172B),
        surfaceLight = Color(0xFF162847),
        focusGlow = Color(0x4D00F2FE)
    ),
    EMERALD_NIGHT(
        title = "Zümrüt Gece",
        subtitle = "Asil Orman & Canlı Nane",
        primary = Color(0xFF09291B),
        secondary = Color(0xFF00E676),
        background = Color(0xFF030E08),
        surface = Color(0xFF081F14),
        surfaceLight = Color(0xFF113624),
        focusGlow = Color(0x4D00E676)
    )
}

/**
 * 3 Seçilebilir Arayüz Zemin Dokusu (Texture):
 * 1. COSMIC_GRADIENT: Yumuşak akışkan Aurora / Kozmik Gradyan
 * 2. DOT_MATRIX: Dijital Nokta Matrisi / Starlight Izgarası
 * 3. CYBER_GRID: Yüksek teknolojili ince siber ızgara deseni
 */
enum class AppBackgroundTexture(
    val title: String,
    val description: String
) {
    COSMIC_GRADIENT(
        title = "Kozmik Gradyan",
        description = "Yumuşak akışkan gradyan ve derin ışık huzmeleri"
    ),
    DOT_MATRIX(
        title = "Nokta Matrisi",
        description = "Fütüristik dijital nokta ızgarası ve starlight"
    ),
    CYBER_GRID(
        title = "Siber Izgara",
        description = "İnce siber çizgiler ve yüksek kontrastlı grid"
    )
}

data class ThemeConfig(
    val palette: AppColorPalette = AppColorPalette.FENERBAHCE,
    val texture: AppBackgroundTexture = AppBackgroundTexture.COSMIC_GRADIENT
)

val LocalThemeConfig = compositionLocalOf { ThemeConfig() }

/**
 * Uygulamanın seçili dokusunu (texture) ve renk paletini çizen kapsayıcı zemin
 */
@Composable
fun AppSurfaceTextureBox(
    modifier: Modifier = Modifier,
    palette: AppColorPalette = LocalThemeConfig.current.palette,
    texture: AppBackgroundTexture = LocalThemeConfig.current.texture,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        when (texture) {
            AppBackgroundTexture.COSMIC_GRADIENT -> {
                // Akışkan Kozmik Gradyan
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    palette.primary.copy(alpha = 0.45f),
                                    palette.surface.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                center = Offset(100f, 100f),
                                radius = 900f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    palette.background.copy(alpha = 0.85f),
                                    palette.background
                                )
                            )
                        )
                )
            }
            AppBackgroundTexture.DOT_MATRIX -> {
                // Dijital Nokta Matrisi / Starlight Canvas Dokusu
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dotSpacing = 28f
                    val dotRadius = 1.4f
                    val dotColor = palette.secondary.copy(alpha = 0.12f)
                    val width = size.width
                    val height = size.height

                    var x = 14f
                    while (x < width) {
                        var y = 14f
                        while (y < height) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }
                // Yumuşak üst gradyan ile derinlik katma
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    palette.primary.copy(alpha = 0.2f),
                                    Color.Transparent,
                                    palette.background.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }
            AppBackgroundTexture.CYBER_GRID -> {
                // Siber Izgara (Subtle Cyber Grid Lines)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSpacing = 44f
                    val gridColor = palette.secondary.copy(alpha = 0.07f)
                    val width = size.width
                    val height = size.height

                    // Dikey çizgiler
                    var x = 0f
                    while (x <= width) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }

                    // Yatay çizgiler
                    var y = 0f
                    while (y <= height) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }
                }
                // Üst hafif ışık huzmesi
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    palette.secondary.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(300f, 0f),
                                radius = 700f
                            )
                        )
                )
            }
        }

        // İçerik
        content()
    }
}
