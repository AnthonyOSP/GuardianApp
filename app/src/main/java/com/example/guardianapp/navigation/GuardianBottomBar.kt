package com.example.guardianapp.navigation

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.guardianapp.ui.components.pressScale
import com.example.guardianapp.ui.theme.GuardianBlueGradient
import com.example.guardianapp.ui.theme.HeroGradientEnd

private val BarHeight = 74.dp
private val HomeButtonSize = 64.dp

/**
 * Barra de navegación inferior flotante de `design/guardianapp-ui-reference.png`:
 * una barra blanca con esquinas redondeadas y sombra suave, con el botón de
 * Inicio como un círculo azul elevado que sobresale por encima. Reemplaza el
 * `NavigationBar`/`NavigationBarItem` de Material3 que usaba la Fase 9 — la
 * referencia pide explícitamente que NO sea una barra de navegación
 * tradicional.
 *
 * Solo controla qué [AppTab] está seleccionada; no sabe nada de qué pantalla
 * renderiza cada una (eso lo decide `UsuarioBleScreen`/`ApoderadoScreen`).
 */
@Composable
fun GuardianBottomBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    // La altura total reserva la mitad del botón Home por encima del borde
    // superior de la barra, para que quede "insertado" en ella pero
    // sobresaliendo — mitad adentro, mitad afuera.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(BarHeight + HomeButtonSize / 2)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SideNavItem(
                    tab = AppTab.CONECTAR,
                    selected = selectedTab == AppTab.CONECTAR,
                    onClick = { onTabSelected(AppTab.CONECTAR) },
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.width(HomeButtonSize))
                SideNavItem(
                    tab = AppTab.AJUSTES,
                    selected = selectedTab == AppTab.AJUSTES,
                    onClick = { onTabSelected(AppTab.AJUSTES) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        GuardianHomeButton(
            onClick = { onTabSelected(AppTab.INICIO) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun SideNavItem(tab: AppTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = tint
        )
    }
}

/**
 * Botón circular elevado de "Inicio": ~64dp, degradado azul, ícono blanco,
 * borde blanco (para que se vea "insertado" en la barra) y un glow azul
 * suave — el elemento visual dominante de la barra, como pide la imagen de
 * referencia. Siempre se dibuja destacado (no cambia de estilo si Inicio no
 * está seleccionado): es el atajo permanente a Inicio, mientras que
 * Conectar/Ajustes sí cambian de color según la pestaña activa.
 */
@Composable
fun GuardianHomeButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = HomeButtonSize) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interactionSource)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = HeroGradientEnd,
                spotColor = HeroGradientEnd
            )
            .clip(CircleShape)
            .background(brush = GuardianBlueGradient)
            .border(width = 3.dp, color = MaterialTheme.colorScheme.surface, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClickLabel = "Inicio",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Inicio",
            tint = Color.White,
            modifier = Modifier.size(size * 0.42f)
        )
    }
}
