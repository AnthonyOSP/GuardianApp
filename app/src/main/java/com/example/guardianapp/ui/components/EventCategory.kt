package com.example.guardianapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.guardianapp.SimulatedEvent
import com.example.guardianapp.ui.icons.BathroomGlyph
import com.example.guardianapp.ui.icons.HelpGlyph
import com.example.guardianapp.ui.icons.RestaurantGlyph
import com.example.guardianapp.ui.theme.CategoryBlue
import com.example.guardianapp.ui.theme.CategoryGreen
import com.example.guardianapp.ui.theme.CategoryOrange

/**
 * Ícono vectorial + color por tipo de evento, para las insignias circulares
 * de "Accesos rápidos" y del Historial. Reemplaza el emoji que usaba esta
 * clase antes del rediseño (`design/guardianapp-ui-reference.png` pide
 * íconos vectoriales reales, no emoji, como ícono principal) — ver
 * [CategoryIcon].
 *
 * Reutiliza [SimulatedEvent] para type/label — no duplica esos datos.
 * Cualquier `type` que no sea uno de los 4 conocidos (por ahora, el
 * `ESP32_EVENT` real de las Fases 4-6) cae en el genérico [CategoryKind.GENERIC].
 */
enum class CategoryKind { EMERGENCY, FOOD, BATHROOM, HELP, GENERIC }

data class EventCategory(val kind: CategoryKind, val label: String, val color: Color)

@Composable
fun categoryFor(type: String?): EventCategory {
    val simulated = SimulatedEvent.fromType(type)
    return when (simulated) {
        SimulatedEvent.EMERGENCY -> EventCategory(CategoryKind.EMERGENCY, simulated.label, MaterialTheme.colorScheme.error)
        SimulatedEvent.FOOD -> EventCategory(CategoryKind.FOOD, simulated.label, CategoryOrange)
        SimulatedEvent.BATHROOM -> EventCategory(CategoryKind.BATHROOM, simulated.label, CategoryBlue)
        SimulatedEvent.HELP -> EventCategory(CategoryKind.HELP, simulated.label, CategoryGreen)
        null -> EventCategory(CategoryKind.GENERIC, "Evento", MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Dibuja el ícono correspondiente a [kind], tintado con [tint]. */
@Composable
fun CategoryIcon(kind: CategoryKind, tint: Color, modifier: Modifier = Modifier) {
    when (kind) {
        CategoryKind.EMERGENCY -> Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = tint, modifier = modifier)
        CategoryKind.FOOD -> RestaurantGlyph(tint = tint, modifier = modifier)
        CategoryKind.BATHROOM -> BathroomGlyph(tint = tint, modifier = modifier)
        CategoryKind.HELP -> HelpGlyph(tint = tint, modifier = modifier)
        CategoryKind.GENERIC -> Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = tint, modifier = modifier)
    }
}
