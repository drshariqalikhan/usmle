package com.step2ck.scanquiz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MedicalBlue500,
    onPrimary = Color.White,
    primaryContainer = Navy800,
    onPrimaryContainer = Color.White,
    secondary = MedicalBlue600,
    onSecondary = Color.White,
    background = Navy900,
    onBackground = Color(0xFFF1F5F9),
    surface = CardDark,
    onSurface = Color(0xFFF1F5F9),
    error = BoardError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalBlue600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Navy900,
    secondary = MedicalBlue700,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = Navy900,
    surface = Color.White,
    onSurface = Navy900,
    error = BoardError,
    onError = Color.White
)

@Composable
fun Step2CKScanQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

