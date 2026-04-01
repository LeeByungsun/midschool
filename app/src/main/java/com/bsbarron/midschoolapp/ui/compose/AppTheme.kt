package com.bsbarron.midschoolapp.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4C8BF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEBFF),
    onPrimaryContainer = Color(0xFF173B66),
    secondary = Color(0xFF173B66),
    onSecondary = Color.White,
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF1C2430),
    surface = Color.White,
    onSurface = Color(0xFF1C2430),
    surfaceVariant = Color(0xFFDDEBFF),
    onSurfaceVariant = Color(0xFF627182),
    outline = Color(0xFFD6DFEA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF0C2A4A),
    primaryContainer = Color(0xFF173B66),
    onPrimaryContainer = Color(0xFFDDEBFF),
    secondary = Color(0xFFB9D1FF),
    onSecondary = Color(0xFF10243D),
    background = Color(0xFF10161F),
    onBackground = Color(0xFFF2F5F9),
    surface = Color(0xFF18212C),
    onSurface = Color(0xFFF2F5F9),
    surfaceVariant = Color(0xFF243243),
    onSurfaceVariant = Color(0xFFB9C4D1),
    outline = Color(0xFF435365)
)

@Composable
fun MisSchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
