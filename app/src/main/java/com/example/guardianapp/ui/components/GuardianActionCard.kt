package com.example.guardianapp.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Tarjeta de "Accesos rápidos": insignia circular sólida + título + subtítulo
 * + una pequeña flecha, sobre un fondo pastel del color de la categoría —
 * unifica lo que antes eran dos componentes distintos (`AlertaTile` +
 * `EmergencyTile`, con la Emergencia como un degradado rojo aparte) en uno
 * solo, como en `design/guardianapp-ui-reference.png` (los 4 accesos
 * comparten el mismo layout, solo cambia el color). Emergencia sigue
 * destacando un poco más vía [emphasized] (fondo un poco más saturado), sin
 * volver a un tratamiento visual completamente distinto.
 */
@Composable
fun GuardianActionCard(
    title: String,
    subtitle: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    icon: @Composable (tint: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    val backgroundAlpha = if (emphasized) 0.16f else 0.10f

    Box(
        modifier = modifier
            .pressScale(interactionSource)
            .clip(shape)
            .background(color = color.copy(alpha = backgroundAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.5f)
            .semantics(mergeDescendants = true) {}
    ) {
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(16.dp)
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = color, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon(Color.White)
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
