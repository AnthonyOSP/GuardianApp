package com.example.guardianapp.fcm

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.guardianapp.firebase.describeFirebaseError
import com.example.guardianapp.firebase.isInternetAvailable
import com.example.guardianapp.identity.LocalIdentity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * FASE 5/6: registra el token FCM de este teléfono (rol Apoderado) en
 * Firestore, etiquetado con el [com.example.guardianapp.identity.LocalIdentity]
 * de este Apoderado, para que el backend sepa a qué Apoderado específico
 * enviar la notificación de un evento (sin broadcast, ver
 * `backend/src/routes/events.js`). Mismo patrón que
 * [com.example.guardianapp.firebase.FirebaseRepository]: expone
 * `mutableStateOf`, la UI ([ApoderadoScreen][com.example.guardianapp.ApoderadoScreen])
 * solo lee [registrationState] y llama a [registerCurrentToken].
 *
 * Cada documento en "apoderadoTokens" usa el propio token como ID de
 * documento (prueba mínima de que quien escribe conoce ese token) y lleva
 * un campo `apoderadoId` (el ID local persistido de este Apoderado, ver
 * `LocalIdentity`). Cuando exista Firebase Authentication, `apoderadoId`
 * pasará a ser el `uid` real en vez de un ID generado localmente — el
 * esquema de Firestore no cambia.
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
    fun registerCurrentToken(apoderadoId: String) {
        registrationState = DeviceRegistrationState.Registering

        val messaging = try {
            FirebaseMessaging.getInstance()
        } catch (e: IllegalStateException) {
            registrationState = DeviceRegistrationState.Error("Firebase no está configurado en la app.")
            return
        }

        try {
            messaging.token
                .addOnSuccessListener { token -> uploadToken(token, apoderadoId) }
                .addOnFailureListener { e ->
                    registrationState = DeviceRegistrationState.Error(
                        "No se pudo obtener el token de notificaciones: ${e.message ?: "error desconocido"}."
                    )
                }
        } catch (e: Exception) {
            registrationState = DeviceRegistrationState.Error(describeFirebaseError(e))
        }
    }

    private fun uploadToken(token: String, apoderadoId: String) {
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
                .set(tokenDocument(token, apoderadoId))
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

        private fun tokenDocument(token: String, apoderadoId: String) = hashMapOf(
            "fcmToken" to token,
            "apoderadoId" to apoderadoId,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        /**
         * Sube [token] sin exponer estado a ninguna UI. Llamado desde
         * [GuardianFirebaseMessagingService.onNewToken], que el sistema
         * dispara fuera del ciclo de vida de cualquier pantalla — por eso
         * no reutiliza una instancia de [FcmTokenRepository] ligada a
         * `remember`. El `apoderadoId` se lee directo de [LocalIdentity]
         * (persistido, no depende de ninguna pantalla abierta). Falla en
         * silencio (queda registrado solo en logcat): no hay pantalla
         * visible a la que reportarle el error en ese momento, y el usuario
         * puede volver a intentarlo reabriendo la pantalla de Apoderado.
         */
        fun uploadTokenFireAndForget(context: Context, token: String) {
            if (token.isBlank() || !isInternetAvailable(context)) return
            val apoderadoId = LocalIdentity.getOrCreateApoderadoId(context)
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection(TOKENS_COLLECTION)
                    .document(token)
                    .set(tokenDocument(token, apoderadoId))
            }
        }
    }
}
