package com.example.guardianapp.fcm

/**
 * Estado del registro del token FCM de este teléfono en Firestore, tal como
 * lo ve la pantalla de Apoderado. Ver [FcmTokenRepository.registerCurrentToken].
 */
sealed class DeviceRegistrationState {
    /** Todavía no se intentó registrar el dispositivo. */
    data object Idle : DeviceRegistrationState()

    /** Obteniendo el token FCM y/o subiéndolo a Firestore. */
    data object Registering : DeviceRegistrationState()

    /** El token quedó guardado en la colección "apoderadoTokens". */
    data object Registered : DeviceRegistrationState()

    /** No se pudo registrar el dispositivo; [message] es apto para mostrar en pantalla. */
    data class Error(val message: String) : DeviceRegistrationState()
}
