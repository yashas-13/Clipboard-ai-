package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF0F172A), // Deep Slate-900 background
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC), // Slate-50 text
    onSurface = Color(0xFFF8FAFC),
    primaryContainer = Color(0xFF4C1D95), // Deep purple container
    onPrimaryContainer = Color(0xFFEDE9FE),
    surfaceContainer = Color(0xFF1E293B), // Slate-800 card container background
    surfaceVariant = Color(0xFF1E293B),
    outlineVariant = Color(0xFF334155) // Slate-700 card border
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = BackgroundLight,
    surface = BackgroundLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = PrimaryPurpleContainer,
    onPrimaryContainer = OnPrimaryPurpleContainer,
    surfaceContainer = CardBackgroundLight,
    surfaceVariant = CardBackgroundLight,
    outlineVariant = CardBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
