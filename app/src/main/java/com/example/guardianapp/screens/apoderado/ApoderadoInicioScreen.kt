package com.example.guardianapp.screens.apoderado

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.guardianapp.fcm.DeviceRegistrationState
import com.example.guardianapp.fcm.FcmTokenRepository
import com.example.guardianapp.fcm.GuardianNotificationCenter
import com.example.guardianapp.firebase.VinculacionRepository
import com.example.guardianapp.firebase.VinculacionState
import com.example.guardianapp.identity.LocalIdentity
import com.example.guardianapp.ui.components.AnimatedStatus
import com.example.guardianapp.ui.components.HistorialEntryCard
import com.example.guardianapp.ui.components.SectionCard
import com.example.guardianapp.ui.components.categoryFor
import com.example.guardianapp.ui.components.formatTime
import com.example.guardianapp.ui.theme.ButtonShape
import com.example.guardianapp.ui.theme.successColor

/**
 * FASE 9, pestaña "Inicio" del Apoderado: estado de registro FCM,
 * vinculación con el Usuario, y una vista compacta de la última actividad
 * (con acceso directo a la pestaña Historial completa). Contenido que
 * antes vivía directo en `ApoderadoScreen.kt`; `ApoderadoScreen` sigue
 * siendo quien crea `FcmTokenRepository`/`VinculacionRepository` y dispara
 * el `LaunchedEffect` de registro (sin cambios ahí).
 *
 * La tarjeta "hero" ("Todo listo"/"Registrando...") está inspirada en
 * `design/guardian-navigation.png`, pero adaptada a lo que esta app puede
 * verificar de verdad: no afirma que "el Usuario está conectado" (el
 * Apoderado no tiene forma de comprobar eso hoy), solo el estado real de
 * *su propio* registro para recibir notificaciones.
 */
@Composable
fun ApoderadoInicioScreen(
    fcmTokenRepository: FcmTokenRepository,
    vinculacionRepository: VinculacionRepository,
    apoderadoId: String,
    onVerHistorialCompleto: () -> Unit
) {
    val context = LocalContext.current
    var notificationsPermissionGranted by remember {
        mutableStateOf(hasNotificationsPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsPermissionGranted = granted }

    // El registro del token no depende del permiso de notificaciones: FCM
    // puede entregar mensajes igual (los usa GuardianNotificationCenter),
    // el permiso solo decide si Android puede *mostrar* la notificación.
    LaunchedEffect(Unit) { fcmTokenRepository.registerCurrentToken(apoderadoId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "GuardianApp", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Modo Apoderado",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (needsNotificationsPermission() && !notificationsPermissionGranted) {
            SectionCard {
                Text(
                    text = "GuardianApp necesita permiso para mostrar notificaciones.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    shape = ButtonShape
                ) {
                    Text("Conceder permiso de notificaciones")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        EstadoHeroCard(fcmTokenRepository.registrationState)
        Spacer(modifier = Modifier.height(16.dp))

        SectionCard {
            VinculacionSection(
                vinculacionState = vinculacionRepository.vinculacionState,
                onVincular = { codigo -> vinculacionRepository.vincular(codigo, apoderadoId) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        UltimaActividadCard(onVerTodo = onVerHistorialCompleto)
    }
}

/** Tarjeta "hero" (siempre oscura) con el estado real de registro de este dispositivo. */
@Composable
private fun EstadoHeroCard(state: DeviceRegistrationState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            AnimatedStatus(targetState = state) { s ->
                when (s) {
                    is DeviceRegistrationState.Idle,
                    is DeviceRegistrationState.Registering -> Column {
                        Text(
                            text = "Registrando dispositivo...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Un momento, estamos preparando tu dispositivo para recibir alertas.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    is DeviceRegistrationState.Registered -> Column {
                        Text(
                            text = "Todo listo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tu dispositivo está registrado para recibir alertas.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color = successColor(), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dispositivo conectado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    is DeviceRegistrationState.Error -> Column {
                        Text(
                            text = "No se pudo registrar el dispositivo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(text = s.message, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

/**
 * FASE 6: vinculación con el Usuario. El código de 6 caracteres lo muestra
 * `UsuarioInicioScreen` en el otro teléfono — NO es una medida de
 * seguridad, es solo el mecanismo de emparejamiento de esta fase (ver
 * `VinculacionRepository` y `firestore.rules`).
 */
@Composable
private fun VinculacionSection(
    vinculacionState: VinculacionState,
    onVincular: (String) -> Unit
) {
    var codigo by remember { mutableStateOf("") }

    Text(text = "Vincular con tu Usuario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Pídele a tu Usuario el código de 6 caracteres que aparece en su pantalla.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = codigo,
        onValueChange = { codigo = it },
        label = { Text("Código de tu Usuario") },
        singleLine = true,
        modifier = Modifier.widthIn(max = 320.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = { onVincular(codigo) }, shape = ButtonShape) {
        Text("Vincular")
    }
    Spacer(modifier = Modifier.height(8.dp))
    AnimatedStatus(targetState = vinculacionState) { state ->
        when (state) {
            is VinculacionState.Idle -> Unit
            is VinculacionState.Sending -> Text(text = "Vinculando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            is VinculacionState.Success -> Text(text = "✓ Vinculado correctamente", color = successColor())
            is VinculacionState.Error -> Text(
                text = "✗ ${state.message}",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Vista compacta de los últimos eventos, con acceso directo a la pestaña Historial completa. */
@Composable
private fun UltimaActividadCard(onVerTodo: () -> Unit) {
    val historial = GuardianNotificationCenter.history
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Última actividad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onVerTodo) {
                Text("Ver todo")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (historial.isEmpty()) {
            Text(
                text = "Esperando eventos...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            historial.take(2).forEach { event ->
                HistorialEntryCard(
                    category = categoryFor(event.type),
                    message = event.message,
                    timeLabel = formatTime(event.receivedAtMillis)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
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
