package com.example.guardianapp

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guardianapp.ui.components.pressScale
import com.example.guardianapp.ui.theme.GuardianAppTheme

/**
 * Pantalla inicial: elegir el rol de este teléfono. La selección es local y
 * temporal (no se persiste, no hay cuentas) — ver [Role] y `MainActivity`.
 *
 * Rediseño visual (skill mobile-app-ui-design, paleta monocromo + rojo):
 * dos tarjetas grandes y tocables, con una insignia "hero" oscura por
 * arriba (mismo tratamiento que la tarjeta de código en `UsuarioBleScreen`)
 * y una leve animación de presión al tocarlas.
 */
@Composable
fun RoleSelectionScreen(onRoleSelected: (Role) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmojiBadge(emoji = "🛡️", background = MaterialTheme.colorScheme.primaryContainer, size = 72.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "GuardianApp",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "¿Con qué rol vas a usar la app?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        RoleCard(
            emoji = "🧑",
            title = "Usuario",
            description = "Te conectás al ESP32 y podés enviar una alerta a tu Apoderado.",
            badgeColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = { onRoleSelected(Role.USUARIO) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        RoleCard(
            emoji = "🔔",
            title = "Apoderado",
            description = "Recibís una notificación apenas tu Usuario genera una alerta.",
            badgeColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = { onRoleSelected(Role.APODERADO) }
        )
    }
}

/** Tarjeta grande y tocable para elegir un rol: insignia oscura + título + descripción de una línea. */
@Composable
private fun RoleCard(
    emoji: String,
    title: String,
    description: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .pressScale(interactionSource),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmojiBadge(emoji = emoji, background = badgeColor, size = 56.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Insignia circular oscura con un emoji grande centrado — el "ícono" de esta pantalla, sin agregar una librería de íconos. */
@Composable
private fun EmojiBadge(emoji: String, background: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value / 2).sp)
    }
}

@Preview(showBackground = true)
@Composable
fun RoleSelectionScreenPreview() {
    GuardianAppTheme {
        RoleSelectionScreen(onRoleSelected = {})
    }
}
