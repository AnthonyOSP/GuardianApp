package com.example.guardianapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de GuardianApp v4: rediseño visual sobre
 * `design/guardianapp-ui-reference.png` — azul como color principal
 * (reemplaza el violeta/índigo de la v3, `design/guardian-navigation.png`),
 * con fondos blancos/azul muy claro, tarjetas blancas y sombras suaves.
 * Solo cambia la piel visual: ningún nombre de repositorio/estado que la UI
 * consume cambió, así que este archivo no toca lógica de negocio.
 */

// Azul — el acento de marca de esta paleta (antes violeta).
val BluePrimaryLight = Color(0xFF4F6FFF)
val BlueOnPrimaryLight = Color(0xFFFFFFFF)
val BluePrimaryContainerLight = Color(0xFFEAF1FF)
val BlueOnPrimaryContainerLight = Color(0xFF172554)

val BluePrimaryDark = Color(0xFF9DB4FF)
val BlueOnPrimaryDark = Color(0xFF102055)
val BluePrimaryContainerDark = Color(0xFF1E2A4A)
val BlueOnPrimaryContainerDark = Color(0xFFDCE7FF)

// Secundario — azul más claro, usado en degradados y acentos suaves.
val BlueSecondaryLight = Color(0xFF6EA8FF)
val BlueOnSecondaryLight = Color(0xFFFFFFFF)
val BlueSecondaryContainerLight = Color(0xFFEAF1FF)
val BlueOnSecondaryContainerLight = Color(0xFF172554)

val BlueSecondaryDark = Color(0xFF89B8FF)
val BlueOnSecondaryDark = Color(0xFF0B1220)
val BlueSecondaryContainerDark = Color(0xFF223255)
val BlueOnSecondaryContainerDark = Color(0xFFDCE7FF)

// Fondo/superficie — blanco y azul muy claro, mucho espacio en blanco.
val BackgroundLight = Color(0xFFF8FAFF)
val OnBackgroundLight = Color(0xFF172554)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF172554)
val SurfaceVariantLight = Color(0xFFEAF1FF)
val OnSurfaceVariantLight = Color(0xFF64748B)
val OutlineLight = Color(0xFFDCE6F7)

val BackgroundDark = Color(0xFF0B1220)
val OnBackgroundDark = Color(0xFFE7EEFF)
val SurfaceDark = Color(0xFF121A2E)
val OnSurfaceDark = Color(0xFFE7EEFF)
val SurfaceVariantDark = Color(0xFF1B2540)
val OnSurfaceVariantDark = Color(0xFF94A3C4)
val OutlineDark = Color(0xFF2B3655)

// Error/Emergencia — el único color de acento cálido fuera de la marca.
val ErrorRedLight = Color(0xFFEF4444)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val ErrorRedDark = Color(0xFFFF8686)
val OnErrorDark = Color(0xFF3B0A0A)
val ErrorContainerDark = Color(0xFF5C1414)
val OnErrorContainerDark = Color(0xFFFFD9D9)

// Éxito — no existe como rol semántico en Material3, se resuelve aparte
// (ver GuardianAppTheme.successColor()).
val SuccessGreenLight = Color(0xFF22C55E)
val SuccessGreenDark = Color(0xFF4ADE80)

// Colores de categoría, solo para las insignias de la pantalla Historial y
// las tarjetas de "Accesos rápidos" (ver ui/components/EventCategory.kt).
// Emergencia reutiliza `error` (rojo), no hace falta un color nuevo.
val CategoryOrange = Color(0xFFF59E0B) // Comida
val CategoryBlue = Color(0xFF3B82F6)   // Baño
val CategoryGreen = Color(0xFF10B981)  // Ayuda

/**
 * Constantes de marca "hero", deliberadamente fijas en ambos modos claro y
 * oscuro (mismo criterio que usaba `HeroContainer`/`HeroOnContainer` en la
 * paleta v2/monocromo): el degradado azul de la imagen de referencia debe
 * verse igual sin importar el tema del sistema.
 */
// Tarjeta "hero clara" (código de Usuario): fondo azul muy claro, texto oscuro.
val HeroSoftBackground = Color(0xFFEAF1FF)
val HeroSoftOnBackground = Color(0xFF172554)

// Tarjeta/botón "hero degradado" (estado del Apoderado, botón Home central,
// logo): degradado azul, siempre con texto/ícono blanco encima.
val HeroGradientStart = Color(0xFF6EA8FF)
val HeroGradientEnd = Color(0xFF4F6FFF)

// Tile de Emergencia en "Accesos rápidos": mismo rojo que `error`, con un
// tinte de fondo pastel un poco más fuerte que las otras 3 categorías para
// que destaque levemente (ver GuardianActionCard).
val EmergencyRedBright = Color(0xFFFF6A61)
val EmergencyRedDeep = Color(0xFFB81D14)
