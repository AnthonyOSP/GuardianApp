package com.example.guardianapp.screens.apoderado

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.guardianapp.fcm.DeviceRegistrationState
import com.example.guardianapp.fcm.FcmTokenRepository
import com.example.guardianapp.fcm.GuardianNotificationCenter
import com.example.guardianapp.fcm.hasNotificationsPermission
import com.example.guardianapp.fcm.needsNotificationsPermission
import com.example.guardianapp.firebase.VinculacionRepository
import com.example.guardianapp.firebase.VinculacionState
import com.example.guardianapp.ui.components.AnimatedStatus
import com.example.guardianapp.ui.components.GuardianCard
import com.example.guardianapp.ui.components.GuardianHeroStyle
import com.example.guardianapp.ui.components.GuardianIconButton
import com.example.guardianapp.ui.components.GuardianLogoBadge
import com.example.guardianapp.ui.components.GuardianSectionTitle
import com.example.guardianapp.ui.components.GuardianStatusCard
import com.example.guardianapp.ui.components.GuardianTopBar
import com.example.guardianapp.ui.components.HistorialEntryCard
import com.example.guardianapp.ui.components.categoryFor
import com.example.guardianapp.ui.components.contentColor
import com.example.guardianapp.ui.components.formatTime
import com.example.guardianapp.ui.icons.GuardianIcons
import com.example.guardianapp.ui.theme.ButtonShape
import com.example.guardianapp.ui.theme.successColor

/**
 * Pestaña "Inicio" del Apoderado, rediseñada sobre
 * `design/guardianapp-ui-reference.png`: estado de registro FCM,
 * vinculación con el Usuario, y una vista compacta de la última actividad
 * (con acceso directo a Historial). Contenido que antes vivía directo en
 * `ApoderadoScreen.kt`; `ApoderadoScreen` sigue siendo quien crea
 * `FcmTokenRepository`/`VinculacionRepository` y dispara el `LaunchedEffect`
 * de registro (sin cambios ahí).
 *
 * La tarjeta "hero" ("Todo listo"/"Registrando...") sigue sin afirmar que
 * "el Usuario está conectado" (el Apoderado no tiene forma de comprobar eso
 * hoy) — solo el estado real de *su propio* registro para recibir
 * notificaciones, igual que antes del rediseño.
 */
@Composable
fun ApoderadoInicioScreen(
    fcmTokenRepository: FcmTokenRepository,
    vinculacionRepository: VinculacionRepository,
    apoderadoId: String,
    onVerHistorialCompleto: () -> Unit,
    onOpenAjustes: () -> Unit
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
        GuardianTopBar(
            title = "GuardianApp",
            subtitle = "Modo Apoderado",
            leading = { GuardianLogoBadge() },
            trailing = {
                GuardianIconButton(icon = Icons.Default.Notifications, contentDescription = "Notificaciones", onClick = onOpenAjustes)
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (needsNotificationsPermission() && !notificationsPermissionGranted) {
            GuardianCard {
                Text(
                    text = "GuardianApp necesita permiso para mostrar notificaciones.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conceder permiso de notificaciones")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        EstadoHeroCard(fcmTokenRepository.registrationState)
        Spacer(modifier = Modifier.height(16.dp))

        GuardianCard {
            VinculacionSection(
                vinculacionState = vinculacionRepository.vinculacionState,
                onVincular = { codigo -> vinculacionRepository.vincular(codigo, apoderadoId) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        UltimaActividadCard(onVerTodo = onVerHistorialCompleto)
    }
}

/** Tarjeta "hero degradado" con el estado real de registro de este dispositivo. */
@Composable
private fun EstadoHeroCard(state: DeviceRegistrationState) {
    GuardianStatusCard(style = GuardianHeroStyle.GRADIENT) {
        val textColor = GuardianHeroStyle.GRADIENT.contentColor()
        AnimatedStatus(targetState = state) { s ->
            when (s) {
                is DeviceRegistrationState.Idle,
                is DeviceRegistrationState.Registering -> Column {
                    Text(
                        text = "Registrando dispositivo...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Un momento, estamos preparando tu dispositivo para recibir alertas.",
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
                is DeviceRegistrationState.Registered -> Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color = textColor.copy(alpha = 0.22f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Todo listo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tu dispositivo está registrado para recibir alertas.",
                        color = textColor.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = textColor, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dispositivo conectado",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                    }
                }
                is DeviceRegistrationState.Error -> Column {
                    Text(
                        text = "No se pudo registrar el dispositivo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(text = s.message, color = textColor.copy(alpha = 0.85f))
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

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GuardianIcons.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "Vincular con tu Usuario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Pídele a tu Usuario el código de 6 caracteres que aparece en su pantalla.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = codigo,
        onValueChange = { codigo = it },
        label = { Text("Código de tu Usuario") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = { onVincular(codigo) }, shape = ButtonShape, modifier = Modifier.fillMaxWidth()) {
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

/** Vista compacta de los últimos eventos, con acceso directo al Historial completo. */
@Composable
private fun UltimaActividadCard(onVerTodo: () -> Unit) {
    val historial = GuardianNotificationCenter.history
    GuardianCard {
        GuardianSectionTitle(title = "Última actividad", trailingText = "Ver todo", onTrailingClick = onVerTodo)
        Spacer(modifier = Modifier.height(12.dp))
        if (historial.isEmpty()) {
            Text(
                text = "Esperando eventos...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                historial.take(2).forEach { event ->
                    HistorialEntryCard(
                        category = categoryFor(event.type),
                        message = event.message,
                        timeLabel = formatTime(event.receivedAtMillis)
                    )
                }
            }
        }
    }
}
