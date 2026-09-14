package com.example.guardianapp.fcm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Extraído de `ApoderadoInicioScreen.kt` (Fase 9) al rediseñarla — ahora
 * `ApoderadoConectarScreen.kt` también necesita este mismo chequeo, sin
 * duplicar la lógica. Comportamiento sin cambios.
 */

/** Antes de Android 13 (API 33), las notificaciones no requieren permiso en tiempo de ejecución. */
fun needsNotificationsPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun hasNotificationsPermission(context: Context): Boolean {
    if (!needsNotificationsPermission()) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}
