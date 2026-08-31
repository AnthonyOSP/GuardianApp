package com.example.guardianapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de GuardianApp v3: clara, con acento violeta/índigo — la de
 * `design/guardian-navigation.png` (Fase 9), no la negra de la iteración
 * anterior ("botones en negro" fue una referencia visual distinta, un shot
 * de Dribbble de wallet cripto; esta imagen reemplaza esa dirección).
 *
 * `primary` es violeta en ambos modos (más claro en oscuro, para
 * contraste) — como `Button`/`FilterChip`/`NavigationBarItem` de
 * Material3 usan `primary`/`primaryContainer` por defecto, la mayoría de
 * los acentos de la app salen violeta sin overrides por sitio. El rojo
 * (`error`) sigue reservado para Emergencia y los estados de error.
 */

// Violeta/índigo — el acento de marca de esta paleta.
val VioletPrimaryLight = Color(0xFF6C5CE7)
val VioletOnPrimaryLight = Color(0xFFFFFFFF)
val VioletPrimaryContainerLight = Color(0xFFE4E1FB)
val VioletOnPrimaryContainerLight = Color(0xFF1A1A2E)

val VioletPrimaryDark = Color(0xFFB7ACFF)
val VioletOnPrimaryDark = Color(0xFF20154D)
val VioletPrimaryContainerDark = Color(0xFF3B2F7A)
val VioletOnPrimaryContainerDark = Color(0xFFE4E1FB)

// Secundario — gris/lavanda neutro para contenedores tonales suaves.
val NeutralSecondaryLight = Color(0xFF5C5C70)
val NeutralOnSecondaryLight = Color(0xFFFFFFFF)
val NeutralSecondaryContainerLight = Color(0xFFEDEDF5)
val NeutralOnSecondaryContainerLight = Color(0xFF1A1A2E)

val NeutralSecondaryDark = Color(0xFFC7C7D6)
val NeutralOnSecondaryDark = Color(0xFF1A1A2E)
val NeutralSecondaryContainerDark = Color(0xFF2E2E42)
val NeutralOnSecondaryContainerDark = Color(0xFFEDEDF5)

// Fondo/superficie — claro y suave (lavanda muy tenue), no blanco puro.
val BackgroundLight = Color(0xFFF5F5FB)
val OnBackgroundLight = Color(0xFF1A1A2E)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1A1A2E)
val SurfaceVariantLight = Color(0xFFEDEDF5)
val OnSurfaceVariantLight = Color(0xFF63636F)
val OutlineLight = Color(0xFFD8D8E4)

val BackgroundDark = Color(0xFF14141F)
val OnBackgroundDark = Color(0xFFE8E8F0)
val SurfaceDark = Color(0xFF1E1E2E)
val OnSurfaceDark = Color(0xFFE8E8F0)
val SurfaceVariantDark = Color(0xFF2E2E42)
val OnSurfaceVariantDark = Color(0xFFA8A8BC)
val OutlineDark = Color(0xFF3A3A4E)

// Error/Emergencia — el único color de acento fuera de la marca.
val ErrorRedLight = Color(0xFFE53E3E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD4)
val OnErrorContainerLight = Color(0xFF410001)

val ErrorRedDark = Color(0xFFFF8A80)
val OnErrorDark = Color(0xFF690002)
val ErrorContainerDark = Color(0xFF8C1D12)
val OnErrorContainerDark = Color(0xFFFFDAD4)

// Éxito — no existe como rol semántico en Material3, se resuelve aparte
// (ver GuardianAppTheme.successColor()).
val SuccessGreenLight = Color(0xFF16A34A)
val SuccessGreenDark = Color(0xFF4ADE80)

// Colores de categoría, solo para las insignias de la pantalla Historial y
// las tarjetas de "Accesos rápidos" (ver ui/components/EventCategory.kt).
// Emergencia reutiliza `error` (rojo), no hace falta un color nuevo.
val CategoryOrange = Color(0xFFEA8C1E)
val CategoryBlue = Color(0xFF2563EB)
val CategoryGreen = Color(0xFF16A34A)

// EmergencyCard (ui/components/EmergencyCard.kt): la acción crítica del
// Usuario con un tratamiento visual premium. Es un bloque de aspecto FIJO
// —no se invierte con el modo claro/oscuro—, mismo criterio que la tarjeta
// "hero" del código de Usuario: una alerta de emergencia debe verse
// idéntica e inequívoca siempre. El tono medio del degradado rojo es
// `ErrorRedLight` (#E53E3E), el mismo rojo de `error` del tema.
val EmergencyCardSurface = Color(0xFFFFFFFF) // tarjeta blanca
val EmergencyNavy = Color(0xFF071735)        // título "Emergencia"
val EmergencySlate = Color(0xFF68738A)       // subtítulo "Enviar alerta inmediata"
val EmergencyRingOuter = Color(0xFFFFE7E4)   // aro externo del círculo (rosa muy claro)
val EmergencyRingMid = Color(0xFFF7B7B1)     // aro intermedio (profundidad)
val EmergencyRedBright = Color(0xFFFF6A61)   // luz del degradado rojo (arriba)
val EmergencyRedDeep = Color(0xFFB81D14)     // sombra del degradado rojo (abajo) + glow
