package com.example.guardianapp.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centraliza qué permisos de Bluetooth necesita GuardianApp según la
 * versión de Android, para no solicitar permisos innecesarios:
 *
 * - Android 12 (API 31) o superior: BLUETOOTH_SCAN y BLUETOOTH_CONNECT.
 * - Android 11 (API 30) o inferior: ACCESS_FINE_LOCATION (BLUETOOTH y
 *   BLUETOOTH_ADMIN ya están concedidos automáticamente por ser permisos
 *   "normales" en esas versiones, no se piden en tiempo de ejecución).
 */
object BlePermissions {

    fun requiredRuntimePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasRequiredPermissions(context: Context): Boolean =
        requiredRuntimePermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun isBluetoothSupported(context: Context): Boolean {
        val manager = context.getSystemService(BluetoothManager::class.java)
        return manager?.adapter != null
    }
}
