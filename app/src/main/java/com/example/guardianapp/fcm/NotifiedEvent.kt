package com.example.guardianapp.fcm

/**
 * Datos de un evento tal como llegan en el payload `data` del mensaje FCM
 * (ver [GuardianFirebaseMessagingService] y `firebase/README.md` para el
 * contrato de campos con el backend). Se muestra en [ApoderadoScreen][
 * com.example.guardianapp.ApoderadoScreen] apenas está disponible, sin
 * importar si llegó con la app en primer plano (`onMessageReceived`) o se
 * reconstruyó de los extras del Intent al tocar la notificación del sistema.
 */
data class NotifiedEvent(
    val type: String,
    val message: String,
    val deviceId: String
)
