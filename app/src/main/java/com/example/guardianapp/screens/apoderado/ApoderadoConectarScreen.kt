package com.example.guardianapp.screens.apoderado

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.guardianapp.fcm.DeviceRegistrationState
import com.example.guardianapp.fcm.hasNotificationsPermission
import com.example.guardianapp.fcm.needsNotificationsPermission
import com.example.guardianapp.ui.components.GuardianCard
import com.example.guardianapp.ui.components.GuardianIconBadge
import com.example.guardianapp.ui.components.GuardianTopBar
import com.example.guardianapp.ui.theme.ButtonShape
import com.example.guardianapp.ui.theme.successColor

/**
 * Pestaña "Conectar" del Apoderado, sobre `design/guardianapp-ui-reference.png`.
 * El Apoderado no tiene BLE/ESP32 (eso es solo del Usuario) — acá "conectar"
 * es el estado real de su conexión con el sistema de alertas: el permiso de
 * notificaciones y el registro del token FCM en Firestore
 * ([DeviceRegistrationState], ya calculado por `FcmTokenRepository` — este
 * archivo solo lo muestra, no agrega lógica nueva).
 */
@Composable
fun ApoderadoConectarScreen(registrationState: DeviceRegistrationState) {
    val context = LocalContext.current
    var notificationsPermissionGranted by remember { mutableStateOf(hasNotificationsPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsPermissionGranted = granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GuardianTopBar(
            title = "Conectar",
            subtitle = "Estado de tu conexión con GuardianApp",
            leading = { GuardianIconBadge(icon = Icons.Default.Notifications) }
        )
        Spacer(modifier = Modifier.height(24.dp))

        GuardianCard {
            EstadoRow(
                label = "Notificaciones",
                ok = notificationsPermissionGranted,
                okText = "Activadas",
                notOkText = "Debes conceder el permiso"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            EstadoRow(
                label = "Registro de alertas",
                ok = registrationState is DeviceRegistrationState.Registered,
                okText = "Registrado",
                notOkText = when (registrationState) {
                    is DeviceRegistrationState.Error -> registrationState.message
                    else -> "Registrando..."
                }
            )
        }

        if (needsNotificationsPermission() && !notificationsPermissionGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Conceder permiso de notificaciones")
            }
        }
    }
}

@Composable
private fun EstadoRow(label: String, ok: Boolean, okText: String, notOkText: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (ok) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (ok) successColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(16.dp)
            )
            Text(
                text = if (ok) okText else notOkText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (ok) successColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
