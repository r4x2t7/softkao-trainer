package com.softkao.trainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F4E79),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F0),
    onPrimaryContainer = Color(0xFF0B2A43),
    secondary = Color(0xFF2E74B5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9F7),
    onSecondaryContainer = Color(0xFF10304F),
    tertiary = Color(0xFF3F7D3F),
    tertiaryContainer = Color(0xFFDCEEDC),
    onTertiaryContainer = Color(0xFF16351A),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE3E8EF),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC7EA),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF1F4E79),
    onPrimaryContainer = Color(0xFFD6E4F0),
    secondary = Color(0xFFA8C8E8),
    onSecondary = Color(0xFF0B2A44),
    tertiary = Color(0xFFA9D3A6),
    background = Color(0xFF121416),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CB),
)

@Composable
fun SoftKaoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
