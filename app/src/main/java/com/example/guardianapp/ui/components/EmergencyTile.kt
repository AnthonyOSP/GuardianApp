package com.example.guardianapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.guardianapp.SimulatedEvent
import com.example.guardianapp.ui.theme.EmergencyRedBright
import com.example.guardianapp.ui.theme.EmergencyRedDeep
import com.example.guardianapp.ui.theme.ErrorRedLight
import com.example.guardianapp.ui.theme.GuardianAppTheme

/**
 * Celda de "Accesos rápidos" para el evento crítico (Emergencia). Ocupa el
 * mismo hueco de la matriz 2x2 que [com.example.guardianapp] `AlertaTile` y
 * comparte su estructura (insignia 40dp + título + subtítulo de acción, con
 * `pressScale`), pero rellena con un degradado rojo y dibuja una sirena
 * blanca con [Canvas] en vez del emoji, para que destaque de las otras tres
 * sin salirse de la grilla.
 *
 * No es un `Button`/`Card` de Material: es un [Box] con degradado, glow rojo
 * sutil ([shadow] con `spotColor`) y ripple. Sin lógica propia: [onClick] se
 * conecta al mismo `BackendEventRepository.notifyEvent(...)` que el resto de
 * los accesos rápidos. Todo el diseño es código, sin PNG/JPG/SVG.
 */
@Composable
fun EmergencyTile(
    evento: SimulatedEvent,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .pressScale(interactionSource)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = EmergencyRedDeep,
                spotColor = EmergencyRedDeep
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (enabled) {
                        listOf(EmergencyRedBright, ErrorRedLight, EmergencyRedDeep)
                    } else {
                        listOf(
                            EmergencyRedDeep.copy(alpha = 0.5f),
                            EmergencyRedDeep.copy(alpha = 0.5f)
                        )
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClickLabel = "Enviar alerta de emergencia"
            ) { onClick() }
            .semantics(mergeDescendants = true) {}
    ) {
        // Reflejo sutil en la mitad superior
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.15f),
                        0.5f to Color.Transparent
                    )
                )
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SirenGlyph(tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = evento.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = evento.actionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Sirena de emergencia dibujada con [Canvas]: perilla + cúpula + base + dos
 * rayos de luz. Silueta blanca sólida (no un signo de exclamación).
 */
@Composable
private fun SirenGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        fun x(f: Float) = ox + s * f
        fun y(f: Float) = oy + s * f

        val domeBaseY = 0.60f
        val domeTopY = 0.17f
        val vRadius = domeBaseY - domeTopY

        // Cúpula (media elipse superior, lado plano hacia abajo)
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(x(0.26f), y(domeTopY)),
            size = Size(s * 0.48f, s * (vRadius * 2f))
        )
        // Base
        drawRoundRect(
            color = tint,
            topLeft = Offset(x(0.18f), y(0.58f)),
            size = Size(s * 0.64f, s * 0.20f),
            cornerRadius = CornerRadius(s * 0.05f, s * 0.05f)
        )
        // Perilla superior
        drawCircle(
            color = tint,
            radius = s * 0.065f,
            center = Offset(x(0.50f), y(domeTopY - 0.02f))
        )
        // Rayos de luz
        val ray = s * 0.05f
        drawLine(tint, Offset(x(0.18f), y(0.34f)), Offset(x(0.03f), y(0.24f)), ray, StrokeCap.Round)
        drawLine(tint, Offset(x(0.82f), y(0.34f)), Offset(x(0.97f), y(0.24f)), ray, StrokeCap.Round)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5FBL)
@Composable
private fun EmergencyTilePreview() {
    GuardianAppTheme {
        Box(Modifier.padding(16.dp).width(170.dp)) {
            EmergencyTile(evento = SimulatedEvent.EMERGENCY, enabled = true, onClick = {})
        }
    }
}
