package com.example.guardianapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = VioletPrimaryLight,
    onPrimary = VioletOnPrimaryLight,
    primaryContainer = VioletPrimaryContainerLight,
    onPrimaryContainer = VioletOnPrimaryContainerLight,
    secondary = NeutralSecondaryLight,
    onSecondary = NeutralOnSecondaryLight,
    secondaryContainer = NeutralSecondaryContainerLight,
    onSecondaryContainer = NeutralOnSecondaryContainerLight,
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
    primary = VioletPrimaryDark,
    onPrimary = VioletOnPrimaryDark,
    primaryContainer = VioletPrimaryContainerDark,
    onPrimaryContainer = VioletOnPrimaryContainerDark,
    secondary = NeutralSecondaryDark,
    onSecondary = NeutralOnSecondaryDark,
    secondaryContainer = NeutralSecondaryContainerDark,
    onSecondaryContainer = NeutralOnSecondaryContainerDark,
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

// Esquinas redondeadas, más suaves que el default de Material3 — look
// "tarjeta" de `design/guardian-navigation.png` (no píldora: esa era la
// referencia visual anterior, de un shot distinto de Dribbble).
private val GuardianShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Forma de botón: rectángulo bien redondeado (16dp), no píldora — coincide con el resto de las tarjetas. */
val ButtonShape = RoundedCornerShape(16.dp)

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
