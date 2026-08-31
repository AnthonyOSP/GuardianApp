package com.example.guardianapp.screens.usuario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.guardianapp.SimulatedEvent
import com.example.guardianapp.backend.BackendEventRepository
import com.example.guardianapp.backend.SentEvent
import com.example.guardianapp.ui.components.FilterChipsRow
import com.example.guardianapp.ui.components.HistorialEntryCard
import com.example.guardianapp.ui.components.categoryFor
import com.example.guardianapp.ui.components.formatTime
import com.example.guardianapp.ui.components.groupByDayLabel
import com.example.guardianapp.ui.theme.successColor

/**
 * FASE 9: historial de alertas que **este Usuario envió**
 * ([BackendEventRepository.sentHistory] — ver el comentario ahí sobre por
 * qué es la fuente mínima correcta, sin Firestore/backend nuevos). Mismo
 * componente de fila y misma agrupación por fecha que
 * `ApoderadoHistorialScreen`, aplicados sobre datos distintos.
 */
@Composable
fun UsuarioHistorialScreen(backendEventRepository: BackendEventRepository) {
    var filtro by remember { mutableStateOf<SimulatedEvent?>(null) }

    val entradas = backendEventRepository.sentHistory
        .filter { filtro == null || it.type == filtro?.type }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Historial",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
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
                    text = "Todavía no enviaste ninguna alerta.",
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
            groupByDayLabel(entradas) { it.sentAtMillis }.forEach { (label, grupo) ->
                item(key = "header-$label") {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(grupo, key = { "${it.sentAtMillis}-${it.type}" }) { entrada: SentEvent ->
                    HistorialEntryCard(
                        category = categoryFor(entrada.type),
                        message = entrada.message,
                        timeLabel = formatTime(entrada.sentAtMillis),
                        trailing = {
                            Text(
                                text = if (entrada.success) "✓" else "✕",
                                color = if (entrada.success) successColor() else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        }
    }
}
