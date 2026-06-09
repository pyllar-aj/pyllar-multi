package com.pyllar.consumer.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color



val V2Cream = Color(0xFFFBF9F4)
val V2Ink = Color(0xFF3E2723)
val V2InkSoft = Color(0xFF6D4C41)
val V2Obsidian = Color(0xFF0A2415)
val V2Gold = Color(0xFFD4AF37)
val V2GoldDeep = Color(0xFF8B6B25)
val V2SuccessGreen = Color(0xFF2E7D32)
val V2HelpText = Color(0xFF1A7A42)
val V2SubtleBorder = Color(0xFFEFEBE9)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF26533E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F5CB),
    onPrimaryContainer = Color(0xFF00391A),
    secondary = Color(0xFF2E7D32), // --v2-success-green
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0F8CE),
    onSecondaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1B),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF2E7D32), // --v2-success-green
    surfaceVariant = Color(0xFFF5F5F5),
    inverseSurface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6DD39E),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF00522C),
    onPrimaryContainer = Color(0xFFB7F5CB),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFD0F8CE),
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    error = Color(0xFFCF6679),
    onError = Color(0xFF1B1B1B),
    outline = V2SuccessGreen,
    surfaceVariant = Color(0xFF232323),
    inverseSurface = Color.Black
)

@Composable
fun PyllarTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography(),
        content = content
    )
}

val MaterialTheme.lightGreyBackground: Color
    @Composable get() = colorScheme.surfaceVariant
val MaterialTheme.cardBackground: Color
    @Composable get() = colorScheme.inverseSurface
val MaterialTheme.cardDescriptionColor: Color
    @Composable get() = colorScheme.onSurface
