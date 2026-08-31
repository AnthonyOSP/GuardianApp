package com.example.guardianapp.navigation

/**
 * FASE 9: las 3 secciones del menú inferior, compartidas por ambos roles
 * (cada rol les da contenido propio — ver `screens/usuario/` y
 * `screens/apoderado/`). Un enum + `when` alcanza: es un intercambio entre
 * 3 hermanos sin pila de navegación, argumentos ni deep links, mismo
 * criterio que ya usó la Fase 2 para no agregar una librería de navegación
 * para elegir el rol (ver `MainActivity.GuardianAppRoot`).
 */
enum class AppTab(val emoji: String, val label: String) {
    INICIO("🏠", "Inicio"),
    HISTORIAL("🕘", "Historial"),
    AJUSTES("⚙️", "Ajustes")
}
