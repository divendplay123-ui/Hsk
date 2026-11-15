package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = VermilionSeal,
    onPrimary = Color.White,
    primaryContainer = VermilionSoft,
    onPrimaryContainer = VermilionDeep,
    secondary = JadeCeladon,
    onSecondary = Color.White,
    secondaryContainer = JadeSoft,
    onSecondaryContainer = Color(0xFF143C32),
    tertiary = AmberGold,
    onTertiary = Color.White,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = Color(0xFF533308),
    background = RicePaper,
    onBackground = InkBlack,
    surface = RicePaperSurface,
    onSurface = InkBlack,
    surfaceVariant = RicePaperWarm,
    onSurfaceVariant = InkMuted,
    surfaceContainerLow = RicePaperSurface,
    surfaceContainer = RicePaperWarm,
    surfaceContainerHigh = Color(0xFFEAE1D4),
    outline = RicePaperStroke,
    outlineVariant = Color(0xFFECE4D8),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = NightVermilion,
    onPrimary = Color(0xFF4F130B),
    primaryContainer = NightVermilionContainer,
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = NightJade,
    onSecondary = Color(0xFF0A3328),
    secondaryContainer = NightJadeContainer,
    onSecondaryContainer = Color(0xFFC0EDE0),
    tertiary = NightAmber,
    onTertiary = Color(0xFF3E2605),
    tertiaryContainer = NightAmberContainer,
    onTertiaryContainer = Color(0xFFFFDFB4),
    background = NightInkBg,
    onBackground = NightRiceText,
    surface = NightInkSurface,
    onSurface = NightRiceText,
    surfaceVariant = NightInkContainer,
    onSurfaceVariant = NightMutedText,
    surfaceContainerLow = NightInkSurface,
    surfaceContainer = NightInkContainer,
    surfaceContainerHigh = Color(0xFF342F29),
    outline = NightInkOutline,
    outlineVariant = Color(0xFF38322B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun HanziHskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
