package com.example.guardianapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.example.guardianapp.MainActivity
import com.example.guardianapp.R

/**
 * Canal y construcción de la notificación visible de "nuevo evento" (Fase 5).
 *
 * IMPORTANTE: [CHANNEL_ID] debe coincidir con el meta-data
 * `com.google.firebase.messaging.default_notification_channel_id` en
 * `AndroidManifest.xml` — ese meta-data es el canal que usa Android cuando
 * el sistema muestra la notificación automáticamente (app en segundo plano
 * o cerrada); [ensureChannel] + [buildEventNotification] son para cuando la
 * construimos nosotros a mano (app en primer plano). Ambos casos deben usar
 * el mismo canal para que el usuario los vea/configure como una sola cosa.
 */
object GuardianNotifications {
    const val CHANNEL_ID = "guardian_events"
    private const val CHANNEL_NAME = "Eventos de GuardianApp"
    private const val CHANNEL_DESCRIPTION = "Avisos cuando el ESP32 genera un evento"
    const val NOTIFICATION_ID = 1001

    const val EXTRA_EVENT_TYPE = "com.example.guardianapp.extra.EVENT_TYPE"
    const val EXTRA_EVENT_MESSAGE = "com.example.guardianapp.extra.EVENT_MESSAGE"
    const val EXTRA_EVENT_DEVICE_ID = "com.example.guardianapp.extra.EVENT_DEVICE_ID"

    /**
     * Crear un canal con un id ya existente es un no-op; seguro de llamar
     * siempre. No hace falta comprobar `Build.VERSION.SDK_INT`: los canales
     * de notificación existen desde API 26 (Android 8, `O`), que ya es el
     * `minSdk` del proyecto.
     */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = CHANNEL_DESCRIPTION }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * Construye la notificación para el caso "app en primer plano" (cuando
     * [GuardianFirebaseMessagingService.onMessageReceived] se ejecuta y el
     * sistema NO la muestra automáticamente). Al tocarla, abre
     * [MainActivity] con los datos del evento como extras.
     */
    fun buildEventNotification(
        context: Context,
        title: String,
        body: String,
        event: NotifiedEvent
    ): android.app.Notification {
        ensureChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_EVENT_TYPE, event.type)
            putExtra(EXTRA_EVENT_MESSAGE, event.message)
            putExtra(EXTRA_EVENT_DEVICE_ID, event.deviceId)
        }
        val pendingIntent = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(contentIntent)
            .getPendingIntent(
                NOTIFICATION_ID,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
    }
}
