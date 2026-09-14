package com.example.guardianapp.screens.usuario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.guardianapp.BuildConfig
import com.example.guardianapp.ui.components.GuardianIconBadge
import com.example.guardianapp.ui.components.GuardianTopBar
import com.example.guardianapp.ui.components.SettingsRow
import com.example.guardianapp.ui.icons.GuardianIcons
import com.example.guardianapp.ui.theme.ButtonShape

/**
 * Pestaña "Ajustes" del Usuario, rediseñada sobre
 * `design/guardianapp-ui-reference.png`: cada opción es su propia tarjeta
 * blanca redondeada (ver [SettingsRow]), no un bloque gris grande con
 * divisores. Solo dos filas tienen datos reales hoy (Dispositivos vinculados
 * = el código propio; Acerca de = versión de la app vía `BuildConfig`, ya
 * existía); el resto se muestra como "Próximamente" en vez de simular una
 * función que no existe. "Cerrar sesión" es la misma función que ya existía.
 */
@Composable
fun UsuarioAjustesScreen(usuarioId: String, onCerrarSesion: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GuardianTopBar(
            title = "Ajustes",
            subtitle = "Configura tu cuenta y preferencias",
            leading = { GuardianIconBadge(icon = Icons.Default.Settings) }
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsRow(icon = Icons.Default.Person, title = "Perfil", subtitle = "Próximamente")
            SettingsRow(icon = Icons.Default.Notifications, title = "Notificaciones", subtitle = "No aplica a tu rol todavía")
            SettingsRow(icon = GuardianIcons.Link, title = "Dispositivos vinculados", subtitle = "Tu código: $usuarioId")
            SettingsRow(
                icon = Icons.Default.Info,
                title = "Acerca de",
                subtitle = "GuardianApp ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onCerrarSesion,
            shape = ButtonShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.height(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar sesión", fontWeight = FontWeight.Medium)
        }
    }
}
