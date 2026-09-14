package com.example.guardianapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.guardianapp.ui.icons.GuardianIcons

/**
 * FASE 9 + rediseño sobre `design/guardianapp-ui-reference.png`: las 3
 * secciones de la barra inferior flotante, compartidas por ambos roles (cada
 * uno les da contenido propio — ver `screens/usuario/` y
 * `screens/apoderado/`). El orden importa: [GuardianBottomBar] los dibuja en
 * este orden (izquierda→derecha), con INICIO como el botón circular elevado
 * del centro.
 *
 * Historial ya no es una pestaña de esta barra (la referencia solo pide
 * Conectar/Inicio/Ajustes) — sigue existiendo como pantalla, alcanzable
 * desde un enlace "Ver historial" en Inicio (ver `UsuarioBleScreen`/
 * `ApoderadoScreen`). CONECTAR reemplaza ese hueco: para el Usuario abre la
 * pantalla de conexión BLE/ESP32 que ya existía dentro de Inicio; para el
 * Apoderado, el estado real de su registro de notificaciones (no hay BLE de
 * ese lado) — ver `screens/usuario/UsuarioConectarScreen.kt` y
 * `screens/apoderado/ApoderadoConectarScreen.kt`.
 */
enum class AppTab(val icon: ImageVector, val label: String) {
    CONECTAR(GuardianIcons.Bluetooth, "Conectar"),
    INICIO(Icons.Default.Home, "Inicio"),
    AJUSTES(Icons.Default.Settings, "Ajustes")
}
