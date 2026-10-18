package com.example.guardianapp.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** Preferencia de tema elegida en Ajustes. */
enum class ThemeMode(val label: String) {
    SYSTEM("Según el sistema"),
    LIGHT("Claro"),
    DARK("Oscuro")
}

/**
 * Tema elegido por el usuario en Ajustes (opción "Modo oscuro"), persistido
 * en SharedPreferences para que sobreviva a cerrar la app — a diferencia
 * del rol, que sigue sin persistirse a propósito (ver `MainActivity`).
 *
 * Es un `object` (como [com.example.guardianapp.fcm.GuardianNotificationCenter])
 * porque lo leen dos lugares que no comparten un `remember`: `MainActivity`
 * (para elegir el `ColorScheme` y el color de los íconos de la barra de
 * estado) y las pantallas de Ajustes de ambos roles (para cambiarlo).
 * [mode] es `mutableStateOf`, así que cambiarlo recompone toda la app al
 * instante, sin recrear la Activity (que reiniciaría el rol elegido).
 */
object ThemeSettings {
    private const val PREFS_NAME = "guardian_settings"
    private const val KEY_THEME_MODE = "theme_mode"

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    /** Debe llamarse una vez en `MainActivity.onCreate`, antes de `setContent`. */
    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, null)
        mode = ThemeMode.entries.find { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun setMode(context: Context, newMode: ThemeMode) {
        mode = newMode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_THEME_MODE, newMode.name) }
    }
}
