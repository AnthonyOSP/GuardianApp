package com.example.guardianapp.fcm

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.guardianapp.SimulatedEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FASE 5: recibe los mensajes FCM enviados por el backend cuando se crea un
 * evento nuevo en Firestore.
 *
 * Comportamiento de FCM según el estado de la app (ver `firebase/README.md`
 * para el detalle completo):
 * - **App en segundo plano o cerrada**: el sistema operativo muestra la
 *   notificación automáticamente a partir del payload `notification` del
 *   mensaje — este método ([onMessageReceived]) **no se ejecuta**. Al tocar
 *   la notificación, Android abre `MainActivity` y entrega el payload
 *   `data` como extras del Intent (sin código adicional de nuestra parte).
 * - **App en primer plano**: el sistema NO muestra nada solo; FCM entrega
 *   todo (`notification` + `data`) a [onMessageReceived], y somos nosotros
 *   quienes construimos y mostramos la notificación con
 *   [GuardianNotifications.buildEventNotification].
 *
 * En ambos casos el mensaje incluye el mismo payload `data`, así que
 * [GuardianNotificationCenter] queda actualizado apenas la pantalla de
 * Apoderado puede leerlo (directo aquí si está en primer plano; desde los
 * extras del Intent en `MainActivity` si no).
 *
 * FASE 8: desde esta fase, `backend/src/routes/events.js` ya arma
 * `notification.title`/`body` según `data.type` (tabla `EVENT_TITLES`, ver
 * ese archivo) — por eso el límite que existía en la Fase 7 (el título por
 * tipo de evento solo se veía con la app en primer plano) ya no aplica: en
 * segundo plano/cerrada, Android muestra directamente lo que manda el
 * backend, que ya es correcto por tipo. El cálculo de `title`/`body` de
 * abajo (usando [SimulatedEvent]) queda como algo redundante pero
 * inofensivo para el caso primer plano — calcula exactamente el mismo
 * texto que ya viene en `notification`, así que no hace falta quitarlo.
 */
class GuardianFirebaseMessagingService : FirebaseMessagingService() {

    // Ver el comentario en FcmTokenRepository.registerCurrentToken(): el SDK
    // instalado marca onNewToken() como obsoleto a favor de onRegistered()/
    // onUnregistered(), un modelo nuevo deshabilitado por defecto. Se
    // mantiene deliberadamente esta ruta, todavía soportada.
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        // El token puede cambiar (reinstalación, restauración de backup,
        // limpieza de datos, rotación interna de FCM). Lo resubimos de
        // inmediato para no depender de que el usuario vuelva a abrir la
        // pantalla de Apoderado.
        FcmTokenRepository.uploadTokenFireAndForget(applicationContext, token)
    }

    // hasNotificationPermission() ya comprueba el permiso dinámicamente,
    // pero lint no rastrea esa verificación a través de un método propio
    // (mismo caso que BlePermissions.hasRequiredPermissions() en
    // ble/BleManager.kt, suprimido allí por la misma razón).
    @SuppressLint("MissingPermission")
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"]
        val eventMessage = data["message"]
        val deviceId = data["deviceId"]
        if (type.isNullOrBlank() || eventMessage.isNullOrBlank() || deviceId.isNullOrBlank()) {
            // Evento sin información válida: no hay nada útil que mostrar.
            return
        }
        val event = NotifiedEvent(type = type, message = eventMessage, deviceId = deviceId)
        GuardianNotificationCenter.onEventReceived(event)

        val notification = message.notification ?: return
        // FASE 7: si el tipo corresponde a uno de los botones de "Enviar
        // alerta", el título/cuerpo reflejan ese evento en vez del texto
        // genérico que manda hoy el backend (ver nota de límite conocido en
        // el doc de la clase). Para el evento real del ESP32 (`ESP32_EVENT`,
        // Fase 4/5/6), fromType(type) da `null` y el comportamiento no cambia.
        val simulatedEvent = SimulatedEvent.fromType(type)
        val title = simulatedEvent?.notificationTitle ?: notification.title ?: "Nuevo evento"
        val body = simulatedEvent?.let { eventMessage } ?: notification.body ?: "Se recibió un evento desde $deviceId"

        if (!hasNotificationPermission()) {
            // Permiso denegado (solo posible desde Android 13/API 33):
            // GuardianNotificationCenter ya se actualizó arriba, así que la
            // pantalla de Apoderado igual muestra el evento si está abierta,
            // simplemente no hay notificación visible en la barra de estado.
            return
        }
        val built = GuardianNotifications.buildEventNotification(
            context = applicationContext,
            title = title,
            body = body,
            event = event
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(GuardianNotifications.NOTIFICATION_ID, built)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
