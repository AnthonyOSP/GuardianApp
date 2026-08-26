package com.example.guardianapp

/**
 * FASE 2: rol local y temporal seleccionado en la pantalla de inicio.
 * No hay cuentas ni autenticación real todavía; esto solo decide qué
 * pantalla se muestra dentro de esta misma sesión de la app.
 */
enum class Role(val displayName: String) {
    USUARIO("Usuario"),
    APODERADO("Apoderado")
}
