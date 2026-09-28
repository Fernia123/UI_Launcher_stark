package com.example.prubea_01_bloat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = HudCyan,
    secondary = HudGold,
    tertiary = HudCyanSoft,
    background = HudBlack,
    surface = HudDarkBackground
)

@Composable
fun Prubea_01_bloatTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = HudTypography,
        content = content
    )
}