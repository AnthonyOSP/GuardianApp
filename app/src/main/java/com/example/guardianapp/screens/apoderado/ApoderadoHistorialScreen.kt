package com.example.guardianapp.screens.apoderado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.guardianapp.SimulatedEvent
import com.example.guardianapp.fcm.GuardianNotificationCenter
import com.example.guardianapp.fcm.NotifiedEvent
import com.example.guardianapp.ui.components.FilterChipsRow
import com.example.guardianapp.ui.components.GuardianIconButton
import com.example.guardianapp.ui.components.GuardianTopBar
import com.example.guardianapp.ui.components.HistorialEntryCard
import com.example.guardianapp.ui.components.categoryFor
import com.example.guardianapp.ui.components.formatTime
import com.example.guardianapp.ui.components.groupByDayLabel

/**
 * Historial de eventos que **este Apoderado recibió** por FCM
 * ([GuardianNotificationCenter.history] — ver el comentario ahí sobre por
 * qué es la fuente mínima correcta: `events` en Firestore no se puede leer
 * desde el cliente y ni siquiera registra a qué Apoderado pertenece cada
 * evento, así que reconstruir esto desde ahí habría exigido cambiar reglas
 * de seguridad y el esquema de datos — no era la modificación mínima).
 *
 * A diferencia del Historial del Usuario, estas filas **no** muestran una
 * insignia de estado tipo "Atendido": no existe ningún campo real que
 * registre si un Apoderado atendió una alerta (no se agregó esa función),
 * así que mostrar eso habría sido inventar un dato — se deja solo el ícono,
 * el mensaje, la fecha/hora y la flecha decorativa.
 */
@Composable
fun ApoderadoHistorialScreen(onBack: () -> Unit) {
    var filtro by remember { mutableStateOf<SimulatedEvent?>(null) }

    val entradas: List<NotifiedEvent> = GuardianNotificationCenter.history
        .filter { filtro == null || it.type == filtro?.type }

    Column(modifier = Modifier.fillMaxSize()) {
        GuardianTopBar(
            title = "Historial",
            subtitle = "Tus alertas y eventos",
            leading = { GuardianIconButton(icon = Icons.Default.ArrowBack, contentDescription = "Volver", onClick = onBack) },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        FilterChipsRow(
            options = listOf(null) + SimulatedEvent.entries,
            selected = filtro,
            label = { it?.label ?: "Todos" },
            onSelect = { filtro = it },
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (entradas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Todavía no llegó ningún evento.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupByDayLabel(entradas) { it.receivedAtMillis }.forEach { (label, grupo) ->
                item(key = "header-$label") {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(grupo, key = { "${it.receivedAtMillis}-${it.type}" }) { evento: NotifiedEvent ->
                    HistorialEntryCard(
                        category = categoryFor(evento.type),
                        message = evento.message,
                        timeLabel = formatTime(evento.receivedAtMillis)
                    )
                }
            }
        }
    }
}
