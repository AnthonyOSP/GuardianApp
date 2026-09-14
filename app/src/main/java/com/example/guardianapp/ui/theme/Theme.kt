package com.example.guardianapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = BlueOnPrimaryContainerLight,
    secondary = BlueSecondaryLight,
    onSecondary = BlueOnSecondaryLight,
    secondaryContainer = BlueSecondaryContainerLight,
    onSecondaryContainer = BlueOnSecondaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorRedLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    secondary = BlueSecondaryDark,
    onSecondary = BlueOnSecondaryDark,
    secondaryContainer = BlueSecondaryContainerDark,
    onSecondaryContainer = BlueOnSecondaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorRedDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

// Esquinas redondeadas, look "tarjeta premium" de
// `design/guardianapp-ui-reference.png` — un poco más suaves que la v3.
private val GuardianShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Forma de botón: píldora completa (bordes 100% redondeados) — coincide con
 * "Comenzar"/"Vincular"/"Buscar dispositivo" en la imagen de referencia.
 * (La v3 anterior usaba un rectángulo de 16dp; la referencia actual tiene
 * prioridad sobre ese diseño para los aspectos visuales.)
 */
val ButtonShape = RoundedCornerShape(percent = 50)

/** Degradado azul de marca (botón Home central, tarjetas "hero", logo). */
val GuardianBlueGradient: Brush
    get() = Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))

/**
 * Theme de GuardianApp: `MaterialTheme` de Compose con un `ColorScheme` y
 * `Shapes` propios (no reemplaza el sistema de theming, solo lo alimenta).
 */
@Composable
fun GuardianAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = GuardianShapes,
        content = content
    )
}

/**
 * Verde de éxito para los estados "✓ ... correctamente". No es un rol
 * semántico de Material3 (solo existe `error`, no `success`), así que se
 * resuelve aparte según el modo claro/oscuro activo.
 */
@Composable
fun successColor(): Color = if (isSystemInDarkTheme()) SuccessGreenDark else SuccessGreenLight
