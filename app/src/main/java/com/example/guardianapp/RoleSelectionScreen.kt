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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.guardianapp.ui.components.pressScale
import com.example.guardianapp.ui.icons.GuardianLogo
import com.example.guardianapp.ui.theme.GuardianAppTheme
import com.example.guardianapp.ui.theme.GuardianBlueGradient

/**
 * Pantalla inicial: elegir el rol de este teléfono. La selección es local y
 * temporal (no se persiste, no hay cuentas) — ver [Role] y `MainActivity`.
 * Sin cambios de lógica en el rediseño sobre
 * `design/guardianapp-ui-reference.png`: mismas dos tarjetas, mismo
 * [onRoleSelected]; solo cambió la piel visual (logo de marca + paleta azul).
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
        GuardianLogo(modifier = Modifier.size(80.dp))
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
            icon = Icons.Default.Person,
            title = "Usuario",
            description = "Enviar una alerta a tu apoderado.",
            onClick = { onRoleSelected(Role.USUARIO) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        RoleCard(
            icon = Icons.Default.Notifications,
            title = "Apoderado",
            description = "Recibes una alerta de tu usuario.",
            onClick = { onRoleSelected(Role.APODERADO) }
        )
    }
}

/** Tarjeta grande y tocable para elegir un rol: insignia con degradado azul + título + descripción de una línea. */
@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    description: String,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(brush = GuardianBlueGradient, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoleSelectionScreenPreview() {
    GuardianAppTheme {
        RoleSelectionScreen(onRoleSelected = {})
    }
}
