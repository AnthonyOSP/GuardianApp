package com.example.guardianapp.fcm

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.guardianapp.firebase.describeFirebaseError
import com.example.guardianapp.firebase.isInternetAvailable
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * FASE 5: registra el token FCM de este teléfono (rol Apoderado) en
 * Firestore, para que el backend sepa a qué dispositivo(s) enviar la
 * notificación de un evento nuevo. Mismo patrón que
 * [com.example.guardianapp.firebase.FirebaseRepository]: expone
 * `mutableStateOf`, la UI ([ApoderadoScreen][com.example.guardianapp.ApoderadoScreen])
 * solo lee [registrationState] y llama a [registerCurrentToken].
 *
 * Asociación temporal Apoderado↔dispositivo (sin cuentas todavía, ver
 * `firebase/README.md`): cada documento en la colección "apoderadoTokens"
 * usa el propio token como ID de documento. No hay "usuario dueño" — es
 * simplemente "estos son los tokens activos que deben recibir avisos".
 * Cuando exista Authentication, el documento pasará a asociarse al UID del
 * Apoderado en vez de auto-identificarse por el valor del token.
 */
class FcmTokenRepository(private val context: Context) {

    var registrationState by mutableStateOf<DeviceRegistrationState>(DeviceRegistrationState.Idle)
        private set

    /**
     * Obtiene el token FCM actual y lo sube a Firestore. No lanza excepciones.
     *
     * Nota de compatibilidad: el SDK instalado (`firebase-messaging` 25.1.2,
     * ver `firebase-bom` en `gradle/libs.versions.toml`) marca
     * `FirebaseMessaging.token` como obsoleto a favor de un modelo nuevo
     * (`register()` + `FirebaseMessagingService.onRegistered()`/
     * `onUnregistered()`, identificado por un "installation ID" en vez de
     * un token). Ese modelo nuevo está deshabilitado por defecto (solo se
     * activa agregando el meta-data
     * `firebase_messaging_installation_id_enabled=true` al manifest, que no
     * agregamos) y no hay documentación pública asentada todavía sobre cómo
     * debe apuntar a él el lado servidor (Admin SDK). Se usa deliberadamente
     * la API de token clásica — todavía soportada y es la que documentan las
     * guías actuales de Firebase — con la advertencia suprimida, mismo
     * criterio que `ble/BleManager.kt` con la API GATT pre-API-33.
     */
    @Suppress("DEPRECATION")
    fun registerCurrentToken() {
        registrationState = DeviceRegistrationState.Registering

        val messaging = try {
            FirebaseMessaging.getInstance()
        } catch (e: IllegalStateException) {
            registrationState = DeviceRegistrationState.Error("Firebase no está configurado en la app.")
            return
        }

        try {
            messaging.token
                .addOnSuccessListener { token -> uploadToken(token) }
                .addOnFailureListener { e ->
                    registrationState = DeviceRegistrationState.Error(
                        "No se pudo obtener el token de notificaciones: ${e.message ?: "error desconocido"}."
                    )
                }
        } catch (e: Exception) {
            registrationState = DeviceRegistrationState.Error(describeFirebaseError(e))
        }
    }

    private fun uploadToken(token: String) {
        if (token.isBlank()) {
            registrationState = DeviceRegistrationState.Error("El token de notificaciones está vacío.")
            return
        }
        if (!isInternetAvailable(context)) {
            registrationState = DeviceRegistrationState.Error("Sin conexión a Internet. No se pudo registrar el dispositivo.")
            return
        }

        try {
            FirebaseFirestore.getInstance()
                .collection(TOKENS_COLLECTION)
                .document(token)
                .set(tokenDocument(token))
                .addOnSuccessListener { registrationState = DeviceRegistrationState.Registered }
                .addOnFailureListener { e ->
                    registrationState = DeviceRegistrationState.Error(describeFirebaseError(e))
                }
        } catch (e: Exception) {
            registrationState = DeviceRegistrationState.Error(describeFirebaseError(e))
        }
    }

    companion object {
        const val TOKENS_COLLECTION = "apoderadoTokens"

        private fun tokenDocument(token: String) = hashMapOf(
            "fcmToken" to token,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        /**
         * Sube [token] sin exponer estado a ninguna UI. Llamado desde
         * [GuardianFirebaseMessagingService.onNewToken], que el sistema
         * dispara fuera del ciclo de vida de cualquier pantalla — por eso
         * no reutiliza una instancia de [FcmTokenRepository] ligada a
         * `remember`. Falla en silencio (queda registrado solo en logcat):
         * no hay pantalla visible a la que reportarle el error en ese
         * momento, y el usuario puede volver a intentarlo reabriendo la
         * pantalla de Apoderado.
         */
        fun uploadTokenFireAndForget(context: Context, token: String) {
            if (token.isBlank() || !isInternetAvailable(context)) return
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection(TOKENS_COLLECTION)
                    .document(token)
                    .set(tokenDocument(token))
            }
        }
    }
}
