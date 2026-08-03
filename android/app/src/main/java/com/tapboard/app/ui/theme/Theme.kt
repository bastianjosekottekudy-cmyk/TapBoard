package com.tapboard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand: deep ink + teal accent
private val Ink = Color(0xFF07141C)
private val InkElevated = Color(0xFF0E2430)
private val Teal = Color(0xFF2DD4BF)
private val TealDim = Color(0xFF149E8C)
private val Sand = Color(0xFFE7F2F0)
private val Mist = Color(0xFFB7CFC9)
private val Alert = Color(0xFFFF7A59)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Ink,
    primaryContainer = TealDim,
    onPrimaryContainer = Sand,
    secondary = Mist,
    onSecondary = Ink,
    background = Ink,
    onBackground = Sand,
    surface = InkElevated,
    onSurface = Sand,
    surfaceVariant = Color(0xFF143342),
    onSurfaceVariant = Mist,
    error = Alert,
    onError = Ink
)

private val LightColors = lightColorScheme(
    primary = TealDim,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F5EF),
    onPrimaryContainer = Ink,
    secondary = Color(0xFF3A5A56),
    onSecondary = Color.White,
    background = Color(0xFFF3FAF8),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFDCEAE6),
    onSurfaceVariant = Color(0xFF3A5A56),
    error = Alert,
    onError = Color.White
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

@Composable
fun TapBoardTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
