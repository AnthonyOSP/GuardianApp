package com.example.guardianapp.screens.apoderado

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.guardianapp.BuildConfig
import com.example.guardianapp.fcm.DeviceRegistrationState
import com.example.guardianapp.ui.components.SectionCard
import com.example.guardianapp.ui.components.SettingsRow
import com.example.guardianapp.ui.theme.ButtonShape

/**
 * FASE 9, pestaña "Ajustes" del Apoderado. Mismo criterio que
 * `UsuarioAjustesScreen`: solo las filas con datos reales muestran algo
 * específico (Notificaciones = estado real de registro FCM; Dispositivos
 * vinculados = el ID propio de este Apoderado; Acerca de = versión real vía
 * `BuildConfig`); el resto se marca "Próximamente" en vez de simular una
 * función que no existe.
 */
@Composable
fun ApoderadoAjustesScreen(
    registrationState: DeviceRegistrationState,
    apoderadoId: String,
    onCerrarSesion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        SectionCard {
            SettingsRow(emoji = "👤", title = "Perfil", subtitle = "Próximamente")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsRow(
                emoji = "🔔",
                title = "Notificaciones",
                subtitle = when (registrationState) {
                    is DeviceRegistrationState.Registered -> "Activas — dispositivo registrado"
                    is DeviceRegistrationState.Error -> "No se pudo activar: ${registrationState.message}"
                    else -> "Registrando..."
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsRow(
                emoji = "🔗",
                title = "Dispositivos vinculados",
                subtitle = "ID de este dispositivo: $apoderadoId"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsRow(
                emoji = "ℹ️",
                title = "Acerca de",
                subtitle = "GuardianApp ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = onCerrarSesion,
            shape = ButtonShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
        ) {
            Text("Cerrar sesión")
        }
    }
}
