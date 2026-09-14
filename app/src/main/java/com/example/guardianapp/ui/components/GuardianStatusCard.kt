package com.example.guardianapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.guardianapp.ui.theme.GuardianBlueGradient
import com.example.guardianapp.ui.theme.HeroSoftBackground
import com.example.guardianapp.ui.theme.HeroSoftOnBackground

/** Estilo visual de [GuardianStatusCard]. */
enum class GuardianHeroStyle {
    /** Fondo azul muy claro + texto oscuro (código del Usuario). */
    SOFT,

    /** Degradado azul + texto/íconos blancos (estado del Apoderado). */
    GRADIENT
}

/** Color de texto/ícono recomendado sobre una [GuardianStatusCard] de este estilo. */
fun GuardianHeroStyle.contentColor(): Color = when (this) {
    GuardianHeroStyle.SOFT -> HeroSoftOnBackground
    GuardianHeroStyle.GRADIENT -> Color.White
}

/**
 * Tarjeta "hero" destacada de cada pantalla de Inicio: el código de Usuario
 * (estilo [GuardianHeroStyle.SOFT]) o el estado de registro del Apoderado
 * (estilo [GuardianHeroStyle.GRADIENT]) — ambas de
 * `design/guardianapp-ui-reference.png`. Fijas en su color independientemente
 * del tema claro/oscuro del sistema, igual que el resto de los acentos de
 * marca (ver `ui/theme/Color.kt`).
 */
@Composable
fun GuardianStatusCard(
    style: GuardianHeroStyle,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (style == GuardianHeroStyle.GRADIENT) 10.dp else 4.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .then(
                when (style) {
                    GuardianHeroStyle.SOFT -> Modifier.background(HeroSoftBackground)
                    GuardianHeroStyle.GRADIENT -> Modifier.background(GuardianBlueGradient)
                }
            )
            .padding(20.dp),
        content = content
    )
}
