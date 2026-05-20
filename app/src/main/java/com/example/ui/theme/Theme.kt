package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HoneyAmber,
    secondary = Terracotta,
    tertiary = LinenMuted,
    background = WarmCharcoal,
    surface = ClaySurface,
    onPrimary = WarmCharcoal,
    onSecondary = LinenText,
    onBackground = LinenText,
    onSurface = LinenText,
    surfaceVariant = EarthyCard,
    onSurfaceVariant = LinenMuted
)

private val LightColorScheme = lightColorScheme(
    primary = HoneyAmber,
    secondary = Terracotta,
    tertiary = LinenDark,
    background = Color(0xFFFAF7F2), // Elegant warm oat background
    surface = Color(0xFFF0EAE1),    // Soft cream card
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFF24201A),
    onSurface = Color(0xFF24201A),
    surfaceVariant = Color(0xFFE4DAD0),
    onSurfaceVariant = Color(0xFF6E6356)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force our custom cinematic styling by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
