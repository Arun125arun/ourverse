package com.lovenote.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lovenote.app.settings.AppSettings

private data class Duo(val light: ColorScheme, val dark: ColorScheme)

/**
 * Hand-tuned Material 3 tonal pairs for each accent. Light bubbles get a
 * vibrant primary with soft containers; dark mode uses light pastel primaries
 * with deep containers (like Google's own apps) instead of blended grays.
 */
private fun duo(
    lightPrimary: Long,
    lightContainer: Long,
    lightOnContainer: Long,
    lightSecondary: Long,
    darkPrimary: Long,
    darkOnPrimary: Long,
    darkContainer: Long,
    darkSecondary: Long,
) = Duo(
    light = lightColorScheme(
        primary = Color(lightPrimary),
        onPrimary = Color.White,
        primaryContainer = Color(lightContainer),
        onPrimaryContainer = Color(lightOnContainer),
        secondary = Color(lightSecondary),
        background = Color(0xFFFDF8F8),
        surface = Color(0xFFFDF8F8),
    ),
    dark = darkColorScheme(
        primary = Color(darkPrimary),
        onPrimary = Color(darkOnPrimary),
        primaryContainer = Color(darkContainer),
        onPrimaryContainer = Color(lightContainer),
        secondary = Color(darkSecondary),
        background = Color(0xFF141218),
        surface = Color(0xFF141218),
    ),
)

private val PALETTES = mapOf(
    "pink" to duo(
        0xFFD81B60, 0xFFFFD9E2, 0xFF3E001D, 0xFF74565F,
        0xFFFFB1C8, 0xFF5E1133, 0xFF7B2949, 0xFFE3BDC6,
    ),
    "purple" to duo(
        0xFF6A4FDB, 0xFFE9DDFF, 0xFF22005D, 0xFF615B71,
        0xFFCFBCFF, 0xFF3B2483, 0xFF5236B0, 0xFFC9C3DC,
    ),
    "blue" to duo(
        0xFF1976D2, 0xFFD3E4FF, 0xFF001C38, 0xFF545F71,
        0xFFA2C9FF, 0xFF00325B, 0xFF0F4C81, 0xFFBCC7DC,
    ),
    "green" to duo(
        0xFF2E7D32, 0xFFC8E6C9, 0xFF002204, 0xFF52634F,
        0xFF8BD88E, 0xFF00390C, 0xFF205F24, 0xFFB9CCB4,
    ),
    "orange" to duo(
        0xFFC25E00, 0xFFFFDCC2, 0xFF311300, 0xFF745944,
        0xFFFFB77C, 0xFF4A2800, 0xFF6F3800, 0xFFE3C0A5,
    ),
)

@Composable
fun LoveNoteTheme(content: @Composable () -> Unit) {
    val dark = when (AppSettings.themeMode) {
        AppSettings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppSettings.ThemeMode.LIGHT -> false
        AppSettings.ThemeMode.DARK -> true
    }

    val scheme = if (
        AppSettings.accentName == AppSettings.DYNAMIC && Build.VERSION.SDK_INT >= 31
    ) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val duo = PALETTES[AppSettings.accentName]
            ?: PALETTES.getValue(AppSettings.DEFAULT_ACCENT)
        if (dark) duo.dark else duo.light
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
