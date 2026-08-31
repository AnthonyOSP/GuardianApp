package com.example.guardianapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guardianapp.ui.theme.EmergencyCardSurface
import com.example.guardianapp.ui.theme.EmergencyNavy
import com.example.guardianapp.ui.theme.EmergencyRedBright
import com.example.guardianapp.ui.theme.EmergencyRedDeep
import com.example.guardianapp.ui.theme.EmergencyRingMid
import com.example.guardianapp.ui.theme.EmergencyRingOuter
import com.example.guardianapp.ui.theme.EmergencySlate
import com.example.guardianapp.ui.theme.ErrorRedLight
import com.example.guardianapp.ui.theme.GuardianAppTheme

/**
 * Tarjeta premium de EMERGENCIA para el rol Usuario: círculo rojo con una
 * sirena arriba (superpuesto), título "Emergencia", subtítulo y un botón
 * rojo grande "ENVIAR ALERTA".
 *
 * Todo el diseño es código —sin PNG/JPG/SVG—: los círculos y el degradado
 * del botón son [Brush], la sirena y el avión de papel se dibujan con
 * [Canvas]. Es un bloque de aspecto **fijo** (no cambia con el tema
 * claro/oscuro), mismo criterio que la tarjeta "hero" del código de
 * Usuario; sus colores viven en `ui/theme/Color.kt` (`Emergency*`).
 *
 * No trae lógica propia: [onEmergencyClick] se conecta desde la pantalla al
 * flujo de envío que ya existe
 * (`BackendEventRepository.notifyEvent(SimulatedEvent.EMERGENCY...)`), y el
 * resultado (Enviando/✓/✕) lo sigue mostrando la sección "Accesos rápidos"
 * de `UsuarioInicioScreen` porque comparten el mismo `notifyState`.
 *
 * @param enabled cuando es `false` (p. ej. hay un envío en curso) el botón
 *   se atenúa e ignora los toques, evitando envíos duplicados.
 * @param pulsing halo muy sutil alrededor del círculo; se puede apagar.
 */
@Composable
fun EmergencyCard(
    onEmergencyClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pulsing: Boolean = true
) {
    // `BoxWithConstraints` para escalar con el ancho REAL disponible (no con
    // el de la pantalla): funciona igual en teléfonos chicos, normales y
    // grandes, y en previews/tablets con la tarjeta más angosta.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
    ) {
        val compact = maxWidth < 330.dp
        val circleSize: Dp = if (compact) 168.dp else 184.dp
        val overlap: Dp = circleSize / 2 // cuánto del círculo cae sobre la tarjeta
        val titleSize = if (compact) 40.sp else 46.sp
        val subtitleSize = if (compact) 22.sp else 24.sp

        // ---- Tarjeta blanca. `background(shape)` en vez de Card para NO
        // recortar el círculo superpuesto ni el glow del botón. ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = overlap)
                .shadow(elevation = 12.dp, shape = EmergencyCardShape, clip = false)
                .background(color = EmergencyCardSurface, shape = EmergencyCardShape),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(overlap + 20.dp)) // deja pasar la mitad inferior del círculo

            Text(
                text = "Emergencia",
                color = EmergencyNavy,
                fontSize = titleSize,
                lineHeight = titleSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Enviar alerta inmediata",
                color = EmergencySlate,
                fontSize = subtitleSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(26.dp))

            EmergencySendButton(
                onClick = onEmergencyClick,
                enabled = enabled,
                compact = compact,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            )
        }

        // ---- Círculo con la sirena, superpuesto y centrado arriba ----
        EmergencyBeacon(
            size = circleSize,
            pulsing = pulsing && enabled,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private val EmergencyCardShape = RoundedCornerShape(36.dp)

/** Esquinas superiores apenas redondeadas, inferiores muy redondeadas. */
private val EmergencyButtonShape = RoundedCornerShape(
    topStart = 14.dp, topEnd = 14.dp, bottomStart = 32.dp, bottomEnd = 32.dp
)

/**
 * Círculo de emergencia: aros concéntricos con degradados radiales para dar
 * profundidad, sombra alrededor, un reflejo sutil arriba y la sirena blanca
 * al centro. [pulsing] agrega un halo que late muy despacio.
 */
@Composable
private fun EmergencyBeacon(
    size: Dp,
    pulsing: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "beacon")
    val pulse by if (pulsing) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        // Halo pulsante (detrás de todo)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 1f + 0.16f * pulse
                    scaleX = s
                    scaleY = s
                    alpha = 0.22f * pulse
                }
                .background(EmergencyRedBright, CircleShape)
        )

        // Aro externo: sombra + rosa muy claro
        Box(
            Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = EmergencyRedDeep,
                    spotColor = EmergencyRedDeep
                )
                .background(
                    Brush.radialGradient(listOf(EmergencyRingOuter, EmergencyRingMid)),
                    CircleShape
                )
        )

        // Aro intermedio
        Box(
            Modifier
                .fillMaxSize(0.82f)
                .background(
                    Brush.radialGradient(
                        listOf(EmergencyRingMid, EmergencyRedBright)
                    ),
                    CircleShape
                )
        )

        // Núcleo rojo con degradado (luz arriba, sombra abajo) + reflejo
        Box(
            Modifier
                .fillMaxSize(0.60f)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(EmergencyRedBright, ErrorRedLight, EmergencyRedDeep)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Reflejo/highlight sutil en la mitad superior
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.30f),
                            0.5f to Color.Transparent
                        ),
                        CircleShape
                    )
            )
            SirenGlyph(
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.52f)
            )
        }
    }
}

/**
 * Sirena de emergencia dibujada con [Canvas]: perilla + cúpula + base +
 * dos rayos de luz. Silueta blanca sólida (no un signo de exclamación).
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

/**
 * Botón rojo grande. No es un `Button` de Material: es un [Box] con
 * degradado vertical, glow rojo ([shadow] con `spotColor`), micro-encogido
 * al presionar ([pressScale]) y ripple blanco.
 */
@Composable
private fun EmergencySendButton(
    onClick: () -> Unit,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val height = if (compact) 110.dp else 122.dp
    val labelSize = if (compact) 25.sp else 28.sp
    val planeSize = if (compact) 54.dp else 62.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(interactionSource)
            .shadow(
                elevation = if (enabled) 14.dp else 0.dp,
                shape = EmergencyButtonShape,
                clip = false,
                ambientColor = EmergencyRedDeep,
                spotColor = EmergencyRedDeep
            )
            .clip(EmergencyButtonShape)
            .background(
                Brush.verticalGradient(
                    if (enabled) {
                        listOf(EmergencyRedBright, ErrorRedLight, EmergencyRedDeep)
                    } else {
                        listOf(
                            EmergencyRedDeep.copy(alpha = 0.55f),
                            EmergencyRedDeep.copy(alpha = 0.55f)
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
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center
    ) {
        // Highlight interno arriba
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.18f),
                        0.45f to Color.Transparent
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaperPlaneGlyph(tint = Color.White, modifier = Modifier.size(planeSize))
            Spacer(Modifier.width(18.dp))
            Text(
                text = "ENVIAR ALERTA",
                color = Color.White,
                fontSize = labelSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
    }
}

/** Avión de papel / "enviar" — geometría del ícono `send` de Material, dibujada con [Canvas]. */
@Composable
private fun PaperPlaneGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        val path = Path().apply {
            moveTo(ox + s * 0.083f, oy + s * 0.875f)
            lineTo(ox + s * 0.958f, oy + s * 0.500f)
            lineTo(ox + s * 0.083f, oy + s * 0.125f)
            lineTo(ox + s * 0.083f, oy + s * 0.417f)
            lineTo(ox + s * 0.708f, oy + s * 0.500f)
            lineTo(ox + s * 0.083f, oy + s * 0.583f)
            close()
        }
        drawPath(path, tint)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5FBL, heightDp = 620)
@Composable
private fun EmergencyCardPreview() {
    GuardianAppTheme {
        Box(Modifier.padding(24.dp)) {
            EmergencyCard(onEmergencyClick = {})
        }
    }
}
