package com.lovenote.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.lovenote.app.settings.AppSettings

@Composable
fun LoveNoteTheme(content: @Composable () -> Unit) {
    val dark = when (AppSettings.themeMode) {
        AppSettings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppSettings.ThemeMode.LIGHT -> false
        AppSettings.ThemeMode.DARK -> true
    }
    val accent = AppSettings.accentColor

    val scheme = if (dark) {
        darkColorScheme(
            primary = lerp(accent, Color.White, 0.15f),
            onPrimary = Color.White,
            primaryContainer = lerp(accent, Color.Black, 0.55f),
            onPrimaryContainer = lerp(accent, Color.White, 0.85f),
            secondary = lerp(accent, Color.Gray, 0.4f),
            surface = Color(0xFF17131A),
            background = Color(0xFF17131A),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = lerp(accent, Color.White, 0.8f),
            onPrimaryContainer = lerp(accent, Color.Black, 0.6f),
            secondary = lerp(accent, Color.Gray, 0.45f),
            surface = Color(0xFFFFF8F8),
            background = Color(0xFFFFF8F8),
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
