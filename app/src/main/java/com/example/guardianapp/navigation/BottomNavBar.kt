package com.example.guardianapp.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * FASE 9: barra de navegación inferior fija, compartida por ambos roles.
 * `NavigationBar`/`NavigationBarItem` son de Material3 (ya en el proyecto,
 * sin dependencia nueva) e incluyen de fábrica el indicador visual de la
 * pestaña activa (la "pill" detrás del ícono) con su propia animación —
 * cumple "indicador de sección activa" + "animaciones" sin código extra.
 */
@Composable
fun BottomNavBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    NavigationBar {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Text(text = tab.emoji) },
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
