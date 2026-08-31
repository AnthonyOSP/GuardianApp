package com.example.guardianapp.fcm

/**
 * Datos de un evento tal como llegan en el payload `data` del mensaje FCM
 * (ver [GuardianFirebaseMessagingService] y `firebase/README.md` para el
 * contrato de campos con el backend). Se muestra en [ApoderadoScreen][
 * com.example.guardianapp.ApoderadoScreen] apenas está disponible, sin
 * importar si llegó con la app en primer plano (`onMessageReceived`) o se
 * reconstruyó de los extras del Intent al tocar la notificación del sistema.
 *
 * [receivedAtMillis] (FASE 9) es la hora local en que *este teléfono*
 * registró el evento — no un timestamp de servidor. Para el caso de
 * primer plano coincide con el instante real de llegada; para el caso
 * "se tocó la notificación del sistema" (segundo plano/cerrada) es la hora
 * en que se abrió la app, no la hora exacta de envío — diferencia mínima y
 * aceptable para el historial local, documentada acá. El valor por
 * defecto evita tener que tocar los dos call-sites que ya construyen este
 * data class (`GuardianFirebaseMessagingService`, `MainActivity`).
 */
data class NotifiedEvent(
    val type: String,
    val message: String,
    val deviceId: String,
    val receivedAtMillis: Long = System.currentTimeMillis()
)
