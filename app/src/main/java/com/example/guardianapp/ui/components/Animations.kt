package com.example.guardianapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Micro-interacción táctil: encoge levemente cualquier composable mientras
 * está presionado (botones, tarjetas tocables). Requiere que el mismo
 * [interactionSource] se le pase también al `clickable`/`Button` que genera
 * los eventos de presión.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, label = "pressScale")
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Transición reutilizable para bloques de "estado" (Idle/Sending/Success/
 * Error de cualquiera de los sealed class del proyecto): funde y desliza
 * levemente el contenido en vez de reemplazarlo de golpe. Un solo lugar
 * para esta animación en vez de repetirla en cada pantalla.
 */
@Composable
fun <T> AnimatedStatus(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(tween(220)) { height -> height / 3 })
                .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(150)) { height -> -height / 3 })
        },
        label = "statusTransition"
    ) { state -> content(state) }
}
