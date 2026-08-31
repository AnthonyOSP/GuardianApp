package com.example.guardianapp.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * FASE 9: barra de navegación inferior fija, compartida por ambos roles.
 * `NavigationBar`/`NavigationBarItem` son de Material3 (ya en el proyecto)
 * e incluyen de fábrica el indicador visual de la pestaña activa (la "pill"
 * detrás del ícono) con su propia animación — cumple "indicador de sección
 * activa" + "animaciones" sin código extra.
 *
 * Los íconos son Material Icons (`Home`/`Settings` de `material-icons-core`,
 * `History` como `ImageVector` local — ver [AppTab.icon]); cada uno lleva
 * `contentDescription` para lectores de pantalla. Los colores salen del tema
 * (`MaterialTheme.colorScheme`), sin valores hardcodeados.
 */
@Composable
fun BottomNavBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    NavigationBar {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(text = tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
