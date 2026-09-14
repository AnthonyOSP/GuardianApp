package com.example.guardianapp.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import com.example.guardianapp.ui.theme.HeroGradientEnd
import com.example.guardianapp.ui.theme.HeroGradientStart

/**
 * Íconos "de marca" dibujados a mano con [Canvas] (mismo criterio que
 * `SirenGlyph` en `ui/components/EmergencyTile.kt`, ya presente antes de
 * este rediseño): formas simples y limpias en vez de transcribir paths
 * complejos de Material Symbols a mano — más bajo riesgo de un path mal
 * formado y un lenguaje visual propio y consistente en toda la app. Todos
 * reciben `tint`/`modifier` igual que un [androidx.compose.material3.Icon].
 */

/** Tenedor + cuchillo — ícono de "Comida". */
@Composable
fun RestaurantGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        fun x(f: Float) = ox + s * f
        fun y(f: Float) = oy + s * f
        val strokeWidth = s * 0.09f

        // Tenedor: tres púas cortas que confluyen en un mango vertical.
        val forkX = x(0.32f)
        listOf(0.20f, 0.32f, 0.44f).forEach { tineX ->
            drawLine(
                color = tint,
                start = Offset(x(tineX), y(0.08f)),
                end = Offset(x(tineX), y(0.34f)),
                strokeWidth = strokeWidth * 0.85f,
                cap = StrokeCap.Round
            )
        }
        drawLine(color = tint, start = Offset(x(0.20f), y(0.34f)), end = Offset(x(0.44f), y(0.34f)), strokeWidth = strokeWidth * 0.7f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(forkX, y(0.34f)), end = Offset(forkX, y(0.92f)), strokeWidth = strokeWidth, cap = StrokeCap.Round)

        // Cuchillo: hoja en forma de gota que se afina hacia un mango vertical.
        val knifeX = x(0.70f)
        val bladePath = Path().apply {
            moveTo(x(0.60f), y(0.08f))
            cubicTo(x(0.86f), y(0.10f), x(0.86f), y(0.30f), x(0.70f), y(0.42f))
            lineTo(knifeX, y(0.50f))
            cubicTo(x(0.60f), y(0.34f), x(0.58f), y(0.16f), x(0.60f), y(0.08f))
            close()
        }
        drawPath(path = bladePath, color = tint)
        drawLine(color = tint, start = Offset(knifeX, y(0.50f)), end = Offset(knifeX, y(0.92f)), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

/** Silueta simple de inodoro — ícono de "Baño". */
@Composable
fun BathroomGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        fun x(f: Float) = ox + s * f
        fun y(f: Float) = oy + s * f

        // Tanque (arriba, rectángulo redondeado angosto).
        drawRoundRect(
            color = tint,
            topLeft = Offset(x(0.30f), y(0.06f)),
            size = Size(s * 0.40f, s * 0.20f),
            cornerRadius = CornerRadius(s * 0.05f, s * 0.05f)
        )
        // Base/asiento (óvalo inferior, más ancho).
        drawOval(
            color = tint,
            topLeft = Offset(x(0.14f), y(0.32f)),
            size = Size(s * 0.72f, s * 0.56f)
        )
        // "Agujero" del asiento, en el color de fondo detrás del ícono —
        // se resuelve con un óvalo más chico usando blend simple: en vez de
        // recortar, se dibuja con alpha bajo para sugerir la abertura sin
        // depender del color de fondo real.
        drawOval(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(x(0.30f), y(0.42f)),
            size = Size(s * 0.40f, s * 0.30f)
        )
        // Base al piso.
        drawRoundRect(
            color = tint,
            topLeft = Offset(x(0.22f), y(0.84f)),
            size = Size(s * 0.56f, s * 0.10f),
            cornerRadius = CornerRadius(s * 0.04f, s * 0.04f)
        )
    }
}

/** Cruz redondeada (estilo "primeros auxilios") — ícono de "Ayuda". */
@Composable
fun HelpGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        val armThickness = s * 0.32f
        val corner = CornerRadius(s * 0.10f, s * 0.10f)

        drawRoundRect(
            color = tint,
            topLeft = Offset(ox + (s - armThickness) / 2f, oy + s * 0.08f),
            size = Size(armThickness, s * 0.84f),
            cornerRadius = corner
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(ox + s * 0.08f, oy + (s - armThickness) / 2f),
            size = Size(s * 0.84f, armThickness),
            cornerRadius = corner
        )
    }
}

/**
 * Sirena de emergencia: perilla + cúpula + base + dos rayos de luz —
 * ícono de "Emergencia" (antes vivía como `SirenGlyph`, privado dentro de
 * `ui/components/EmergencyTile.kt`; ese componente se reemplazó por
 * [com.example.guardianapp.ui.components.GuardianActionCard] y este ícono
 * pasó a ser público acá para poder reutilizarse también en el Historial).
 */
@Composable
fun EmergencyGlyph(tint: Color, modifier: Modifier = Modifier) {
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
 * Logo de marca: escudo con degradado azul + corazón blanco al centro
 * (mismo espíritu que la portada de `design/guardianapp-ui-reference.png`).
 * Se usa en miniatura en [com.example.guardianapp.ui.components.GuardianTopBar]
 * y más grande en `RoleSelectionScreen`.
 */
@Composable
fun GuardianLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val shield = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.92f, h * 0.16f)
            lineTo(w * 0.92f, h * 0.52f)
            cubicTo(w * 0.92f, h * 0.80f, w * 0.72f, h * 0.95f, w * 0.5f, h)
            cubicTo(w * 0.28f, h * 0.95f, w * 0.08f, h * 0.80f, w * 0.08f, h * 0.52f)
            lineTo(w * 0.08f, h * 0.16f)
            close()
        }
        drawPath(path = shield, brush = Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))

        // Corazón: dos círculos + un triángulo, superpuestos.
        val cx = w * 0.5f
        val cy = h * 0.46f
        val r = w * 0.155f
        drawCircle(color = Color.White, radius = r, center = Offset(cx - r * 0.95f, cy - r * 0.35f))
        drawCircle(color = Color.White, radius = r, center = Offset(cx + r * 0.95f, cy - r * 0.35f))
        val heartTip = Path().apply {
            moveTo(cx - r * 1.9f, cy - r * 0.15f)
            lineTo(cx + r * 1.9f, cy - r * 0.15f)
            lineTo(cx, cy + r * 1.9f)
            close()
        }
        drawPath(path = heartTip, color = Color.White)
    }
}
