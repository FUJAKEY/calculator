package com.example.advancedcalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.Black,
    secondary = Color(0xFF1E88E5),
    onSecondary = Color.White,
    background = Color(0xFF101418),
    onBackground = Color.White,
    surface = Color(0xFF1C2226),
    onSurface = Color.White
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    secondary = Color(0xFF1E88E5),
    onSecondary = Color.White,
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF1A1D21),
    surface = Color.White,
    onSurface = Color(0xFF1A1D21)
)

@Composable
fun AdvancedCalculatorTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
