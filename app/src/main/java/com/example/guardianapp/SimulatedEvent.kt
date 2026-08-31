package com.example.guardianapp

/**
 * FASE 7: eventos que el rol Usuario puede generar manualmente desde los
 * botones de "Enviar alerta" en [UsuarioBleScreen], simulando lo que más
 * adelante enviará el ESP32 por BLE (todavía no hay hardware — ver
 * `esp32/README.md` y `ble/`, que esta fase no toca).
 *
 * [type]/[message] viajan tal cual al backend
 * ([com.example.guardianapp.backend.BackendEventRepository.notifyEvent]);
 * [emoji]/[label] arman el texto de los botones y, cuando la app del
 * Apoderado está en primer plano, el título de la notificación (ver
 * [com.example.guardianapp.fcm.GuardianFirebaseMessagingService]).
 */
enum class SimulatedEvent(
    val type: String,
    val message: String,
    val emoji: String,
    val label: String,
    /** Subtítulo corto de la tarjeta en "Accesos rápidos" (Fase 9) — no viaja al backend, ese es [message]. */
    val actionLabel: String,
    /** Presentación únicamente (no viaja al backend). */
    val isCritical: Boolean = false
) {
    EMERGENCY(
        "EMERGENCY", "El Usuario ha enviado una alerta de emergencia.", "🚨", "Emergencia",
        actionLabel = "Enviar alerta", isCritical = true
    ),
    FOOD("FOOD", "El Usuario necesita comida.", "🍽️", "Comida", actionLabel = "Necesito comida"),
    BATHROOM("BATHROOM", "El Usuario necesita ayuda para ir al baño.", "🚻", "Baño", actionLabel = "Necesito ir al baño"),
    HELP("HELP", "El Usuario necesita ayuda.", "🆘", "Ayuda", actionLabel = "Necesito ayuda");

    /** Título de notificación para este tipo de evento, ej. "🚨 Emergencia". */
    val notificationTitle: String get() = "$emoji $label"

    companion object {
        /** `null` si [type] no corresponde a ninguno de estos 4 eventos (p. ej. el `ESP32_EVENT` real). */
        fun fromType(type: String?): SimulatedEvent? = entries.find { it.type == type }
    }
}
