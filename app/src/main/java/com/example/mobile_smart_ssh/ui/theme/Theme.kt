package com.example.mobile_smart_ssh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = IndigoSecondary,
    onSecondaryContainer = Color.White,
    tertiary = IndigoSecondary,
    background = SlateBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = SlateSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = SlateBorder,
    error = RubyRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = Color.Black,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
    error = RubyRed,
    onError = Color.White
)

@Composable
fun SmartSSHTheme(
    darkTheme: Boolean = true, // Domyślnie preferowany nowoczesny motyw ciemny
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}