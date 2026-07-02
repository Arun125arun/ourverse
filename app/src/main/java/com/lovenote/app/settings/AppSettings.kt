package com.lovenote.app.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** User-chosen appearance settings, persisted in SharedPreferences. */
object AppSettings {
    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    val ACCENTS = linkedMapOf(
        "pink" to Color(0xFFD81B60),
        "purple" to Color(0xFF6A4FDB),
        "blue" to Color(0xFF1976D2),
        "green" to Color(0xFF2E7D32),
        "orange" to Color(0xFFC25E00),
    )
    const val DEFAULT_ACCENT = "pink"

    /** Material You: derive colors from the phone's wallpaper (Android 12+). */
    const val DYNAMIC = "dynamic"

    fun supportsDynamic(): Boolean = android.os.Build.VERSION.SDK_INT >= 31

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var accentName by mutableStateOf(DEFAULT_ACCENT)
        private set

    val accentColor: Color
        get() = ACCENTS[accentName] ?: ACCENTS.getValue(DEFAULT_ACCENT)

    private fun prefs(context: Context) =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun load(context: Context) {
        val p = prefs(context)
        themeMode = runCatching {
            ThemeMode.valueOf(p.getString("themeMode", ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        accentName = p.getString("accent", DEFAULT_ACCENT)
            .takeIf { it in ACCENTS || (it == DYNAMIC && supportsDynamic()) }
            ?: DEFAULT_ACCENT
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        prefs(context).edit().putString("themeMode", mode.name).apply()
    }

    fun setAccent(context: Context, name: String) {
        if (name !in ACCENTS && !(name == DYNAMIC && supportsDynamic())) return
        accentName = name
        prefs(context).edit().putString("accent", name).apply()
    }
}
