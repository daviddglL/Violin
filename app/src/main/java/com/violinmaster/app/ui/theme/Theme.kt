package com.violinmaster.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable


private val SophisticatedDarkColorScheme = darkColorScheme(
  primary = PrimaryPurple,
  onPrimary = OnPrimaryPurple,
  primaryContainer = PrimaryContainer,
  onPrimaryContainer = OnPrimaryContainer,
  secondary = SecondaryLav,
  background = DarkBg,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onBackground = TextLight,
  onSurface = TextLight,
  onSurfaceVariant = TextMuted,
  outline = DarkSurfaceVariant,
)

// We define a matching theme or fall back to Sophisticated Dark to satisfy the design theme
private val SophisticatedLightColorScheme = lightColorScheme(
  primary = PrimaryPurple,
  onPrimary = OnPrimaryPurple,
  primaryContainer = PrimaryContainer,
  onPrimaryContainer = OnPrimaryContainer,
  secondary = SecondaryLav,
  background = DarkBg,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onBackground = TextLight,
  onSurface = TextLight,
  onSurfaceVariant = TextMuted,
  outline = DarkSurfaceVariant
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the exact Sophisticated Dark brand colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) {
    SophisticatedDarkColorScheme
  } else {
    // Also use sophisticated dark for this app since the theme is requested globally
    SophisticatedDarkColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

