package com.example.guardianapp.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * FASE 9: agrupa una lista (ya ordenada de más reciente a más antiguo, como
 * ya vienen [com.example.guardianapp.fcm.GuardianNotificationCenter.history]
 * y [com.example.guardianapp.backend.BackendEventRepository.sentHistory])
 * bajo etiquetas "Hoy"/"Ayer"/fecha, para las pantallas de Historial de
 * ambos roles. Función pura (no Compose) para no duplicar esta lógica en
 * cada pantalla — `Map.groupBy` conserva el orden de aparición de las
 * claves, así que como la lista de entrada ya viene ordenada, los grupos
 * salen en el orden correcto sin tener que reordenar nada.
 */
fun <T> groupByDayLabel(items: List<T>, timestampMillisOf: (T) -> Long): List<Pair<String, List<T>>> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))

    return items
        .groupBy { item ->
            val date = Instant.ofEpochMilli(timestampMillisOf(item)).atZone(ZoneId.systemDefault()).toLocalDate()
            when (date) {
                today -> "Hoy"
                yesterday -> "Ayer"
                else -> date.format(formatter)
            }
        }
        .toList()
}

/** Formatea una hora tipo "07:42" a partir de un timestamp en milisegundos. */
fun formatTime(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("HH:mm").format(time)
}
