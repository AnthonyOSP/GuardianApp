package com.example.guardianapp.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Helpers compartidos entre [FirebaseRepository] (Fase 4) y
 * [com.example.guardianapp.fcm.FcmTokenRepository] (Fase 5) — ambos hacen
 * chequeo de conectividad antes de escribir en Firestore y traducen
 * excepciones a mensajes aptos para mostrar en pantalla.
 */

internal fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

internal fun describeFirebaseError(e: Exception): String = when (e) {
    is FirebaseFirestoreException -> when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "No se pudo contactar a Firebase. Revisa tu conexión a Internet."
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore rechazó la operación (revisa las reglas de seguridad)."
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
            "Tiempo de espera agotado al hablar con Firestore."
        else -> e.message ?: "Error de Firestore."
    }
    else -> e.message ?: "Error desconocido al hablar con Firebase."
}
