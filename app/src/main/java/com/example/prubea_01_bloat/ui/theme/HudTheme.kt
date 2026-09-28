package com.example.prubea_01_bloat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val HudColorScheme = darkColorScheme(
    primary = HudCyan,
    secondary = HudGold,
    background = HudBlack,
    surface = HudDarkBackground,
    onPrimary = HudBlack,
    onSecondary = HudBlack,
    onBackground = HudWhite,
    onSurface = HudWhite
)

val HudTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = 1.sp,
        color = HudWhite
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 2.sp,
        color = HudGold
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = HudWhiteDim
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        color = HudCyan
    )
)

@Composable
fun HudTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HudColorScheme,
        typography = HudTypography,
        content = content
    )
}
