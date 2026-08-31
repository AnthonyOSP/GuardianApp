package com.example.guardianapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FASE 9: una fila del historial (Usuario o Apoderado) — insignia circular
 * de color por categoría + tipo + mensaje + hora, con un espacio opcional
 * al final ([trailing]) para un estado de envío (✓/✕, solo lo usa el
 * historial del Usuario). Reutilizada por ambas pantallas de Historial en
 * vez de duplicar este layout.
 */
@Composable
fun HistorialEntryCard(
    category: EventCategory,
    message: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = category.color.copy(alpha = 0.14f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = category.emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.label, color = category.color, fontWeight = FontWeight.Bold)
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }
    }
}
