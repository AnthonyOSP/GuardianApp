package com.example.guardianapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.guardianapp.fcm.DeviceRegistrationState
import com.example.guardianapp.fcm.FcmTokenRepository
import com.example.guardianapp.fcm.GuardianNotificationCenter
import com.example.guardianapp.fcm.NotifiedEvent

/**
 * FASE 5, pantalla del rol Apoderado: reemplaza a [RoleHomeScreen] (que
 * quedó sin uso) igual que [UsuarioBleScreen] reemplazó a [RoleHomeScreen]
 * para Usuario en la Fase 3. Registra el token FCM de este teléfono y
 * muestra el último evento notificado.
 */
@Composable
fun ApoderadoScreen(onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    val fcmTokenRepository = remember { FcmTokenRepository(context) }

    var notificationsPermissionGranted by remember {
        mutableStateOf(hasNotificationsPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsPermissionGranted = granted }

    // El registro del token no depende del permiso de notificaciones: FCM
    // puede entregar mensajes igual (los usa GuardianNotificationCenter),
    // el permiso solo decide si Android puede *mostrar* la notificación.
    LaunchedEffect(Unit) { fcmTokenRepository.registerCurrentToken() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "GuardianApp", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Modo Apoderado", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (needsNotificationsPermission() && !notificationsPermissionGranted) {
            Text(
                text = "GuardianApp necesita permiso para mostrar notificaciones.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text("Conceder permiso de notificaciones")
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(text = "Estado de notificaciones:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        RegistrationStatusSection(fcmTokenRepository.registrationState)

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        EventSection(event = GuardianNotificationCenter.lastEvent)

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCerrarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun RegistrationStatusSection(state: DeviceRegistrationState) {
    when (state) {
        is DeviceRegistrationState.Idle,
        is DeviceRegistrationState.Registering -> Text(text = "Registrando dispositivo...")
        is DeviceRegistrationState.Registered -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "✓ Firebase conectado")
            Text(text = "Dispositivo registrado")
        }
        is DeviceRegistrationState.Error -> Text(
            text = "No se pudo registrar el dispositivo: ${state.message}",
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EventSection(event: NotifiedEvent?) {
    if (event == null) {
        Text(text = "Esperando eventos...", textAlign = TextAlign.Center)
        return
    }
    Text(text = "Último evento", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Tipo: ${event.type}")
    Text(text = "Mensaje: ${event.message}", style = MaterialTheme.typography.headlineSmall)
    Text(text = "Dispositivo: ${event.deviceId}")
}

/** Antes de Android 13 (API 33), las notificaciones no requieren permiso en tiempo de ejecución. */
private fun needsNotificationsPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private fun hasNotificationsPermission(context: android.content.Context): Boolean {
    if (!needsNotificationsPermission()) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

@Preview(showBackground = true)
@Composable
private fun ApoderadoScreenPreview() {
    MaterialTheme {
        ApoderadoScreen(onCerrarSesion = {})
    }
}
