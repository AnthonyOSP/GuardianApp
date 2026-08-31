package com.example.guardianapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.guardianapp.SimulatedEvent
import com.example.guardianapp.ui.theme.CategoryBlue
import com.example.guardianapp.ui.theme.CategoryGreen
import com.example.guardianapp.ui.theme.CategoryOrange

/**
 * FASE 9: emoji + color por tipo de evento, para las pantallas de
 * Historial. Es la única parte de la app con más de un color de acento —
 * a propósito, como etiquetas de categoría sobre un fondo que sigue siendo
 * monocromo (mismo criterio que usan apps de finanzas con categorías de
 * gasto de colores sobre una interfaz mayormente blanco/negro).
 *
 * Reutiliza [SimulatedEvent] para type/emoji/label — no duplica esos datos.
 * Cualquier `type` que no sea uno de los 4 conocidos (por ahora, el
 * `ESP32_EVENT` real de las Fases 4-6) cae en el genérico 🔔/"Evento".
 */
data class EventCategory(val emoji: String, val label: String, val color: Color)

@Composable
fun categoryFor(type: String?): EventCategory {
    val simulated = SimulatedEvent.fromType(type)
    return when (simulated) {
        SimulatedEvent.EMERGENCY -> EventCategory(simulated.emoji, simulated.label, MaterialTheme.colorScheme.error)
        SimulatedEvent.FOOD -> EventCategory(simulated.emoji, simulated.label, CategoryOrange)
        SimulatedEvent.BATHROOM -> EventCategory(simulated.emoji, simulated.label, CategoryBlue)
        SimulatedEvent.HELP -> EventCategory(simulated.emoji, simulated.label, CategoryGreen)
        null -> EventCategory("🔔", "Evento", MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
