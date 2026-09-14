package com.example.guardianapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.guardianapp.ui.icons.GuardianLogo
import com.example.guardianapp.ui.theme.GuardianBlueGradient

/**
 * Encabezado reutilizado por las 6 pestañas de Inicio/Historial/Ajustes (los
 * dos roles) — insignia circular a la izquierda (el logo de marca o un
 * ícono, ver [GuardianLogoBadge]/[GuardianIconBadge]) + título/subtítulo, y
 * un espacio opcional a la derecha para una acción ([trailing], ej. el botón
 * de perfil/campana/volver).
 */
@Composable
fun GuardianTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

/** Insignia del logo de marca (escudo + corazón) — encabezado de las pantallas "Inicio". */
@Composable
fun GuardianLogoBadge(size: androidx.compose.ui.unit.Dp = 40.dp) {
    GuardianLogo(modifier = Modifier.size(size))
}

/** Insignia circular con degradado azul + ícono blanco — encabezado de Historial/Ajustes. */
@Composable
fun GuardianIconBadge(icon: ImageVector, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(brush = GuardianBlueGradient, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/** Botón circular de acción del encabezado (perfil, campana, volver) — fondo azul muy claro. */
@Composable
fun GuardianIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: (() -> Unit)? = null,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}
