package com.example.guardianapp.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Íconos propios definidos como [ImageVector] en código (sin PNG/SVG, sin
 * dependencia extra).
 *
 * Sólo existe [History]: `material-icons-core` (el set curado que sí usa el
 * proyecto) trae `Home` y `Settings` pero **no** `History`, y traer
 * `material-icons-extended` sólo por ese ícono sumaba ~8 MB al APK debug
 * (R8 está apagado, `optimization.enable = false`). Este es el mismo dibujo
 * 24x24 que `Icons.Filled.History` de Material, transcrito con el DSL de
 * `ImageVector.Builder`. El color base es negro porque `Icon(...)` lo
 * re-tinta con `LocalContentColor` al pintarlo (igual que los íconos de
 * `material-icons-core`).
 */
object GuardianIcons {

    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "GuardianIcons.History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero
            ) {
                // d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87
                //    3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06
                //    l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9z
                //    m-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"
                moveTo(13f, 3f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                horizontalLineTo(1f)
                lineToRelative(3.89f, 3.89f)
                lineToRelative(0.07f, 0.14f)
                lineTo(9f, 12f)
                horizontalLineTo(6f)
                curveToRelative(0f, -3.87f, 3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                curveToRelative(-1.93f, 0f, -3.68f, -0.79f, -4.94f, -2.06f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveToRelative(4.97f, 0f, 9f, -4.03f, 9f, -9f)
                reflectiveCurveToRelative(-4.03f, -9f, -9f, -9f)
                close()
                moveToRelative(-1f, 5f)
                verticalLineToRelative(5f)
                lineToRelative(4.28f, 2.54f)
                lineToRelative(0.72f, -1.21f)
                lineToRelative(-3.5f, -2.08f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                close()
            }
        }.build()
    }
}
