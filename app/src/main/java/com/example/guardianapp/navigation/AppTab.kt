package com.example.guardianapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.guardianapp.ui.icons.GuardianIcons

/**
 * FASE 9: las 3 secciones del menú inferior, compartidas por ambos roles
 * (cada rol les da contenido propio — ver `screens/usuario/` y
 * `screens/apoderado/`). Un enum + `when` alcanza: es un intercambio entre
 * 3 hermanos sin pila de navegación, argumentos ni deep links, mismo
 * criterio que ya usó la Fase 2 para no agregar una librería de navegación
 * para elegir el rol (ver `MainActivity.GuardianAppRoot`).
 *
 * El [icon] usa Material Icons en vez de un emoji, para un aspecto
 * consistente con Material 3: `Home` y `Settings` salen de
 * `material-icons-core` (el set curado, liviano); `History` no está en ese
 * set, así que se define como [ImageVector] local en
 * `ui/icons/GuardianIcons.kt` para no arrastrar `material-icons-extended`
 * (~+8 MB de APK debug). El [label] se muestra bajo el ícono y también se
 * reutiliza como `contentDescription` (ver [BottomNavBar]).
 */
enum class AppTab(val icon: ImageVector, val label: String) {
    INICIO(Icons.Default.Home, "Inicio"),
    HISTORIAL(GuardianIcons.History, "Historial"),
    AJUSTES(Icons.Default.Settings, "Ajustes")
}
