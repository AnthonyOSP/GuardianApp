package com.example.guardianapp.screens.usuario

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guardianapp.EventConstants
import com.example.guardianapp.SimulatedEvent
import com.example.guardianapp.backend.BackendEventRepository
import com.example.guardianapp.backend.BackendNotifyState
import com.example.guardianapp.ble.BleConnectionState
import com.example.guardianapp.ble.BleManager
import com.example.guardianapp.firebase.FirebaseRepository
import com.example.guardianapp.ui.components.AnimatedStatus
import com.example.guardianapp.ui.components.CategoryIcon
import com.example.guardianapp.ui.components.GuardianActionCard
import com.example.guardianapp.ui.components.GuardianCard
import com.example.guardianapp.ui.components.GuardianHeroStyle
import com.example.guardianapp.ui.components.GuardianIconButton
import com.example.guardianapp.ui.components.GuardianLogoBadge
import com.example.guardianapp.ui.components.GuardianSectionTitle
import com.example.guardianapp.ui.components.GuardianStatusCard
import com.example.guardianapp.ui.components.GuardianTopBar
import com.example.guardianapp.ui.components.categoryFor
import com.example.guardianapp.ui.components.contentColor
import com.example.guardianapp.ui.components.pressScale
import com.example.guardianapp.ui.icons.GuardianIcons
import com.example.guardianapp.ui.theme.successColor

/**
 * Pestaña "Inicio" del Usuario, rediseñada sobre
 * `design/guardianapp-ui-reference.png`: tarjeta del código de vinculación,
 * las 4 acciones rápidas (Fase 7) y un resumen (no el detalle completo,
 * movido a [UsuarioConectarScreen]) de la conexión BLE con el ESP32
 * (Fase 3). Es el mismo contenido/estado que antes vivía directo en
 * `UsuarioBleScreen.kt`, ahora como una pestaña de la navegación inferior —
 * `UsuarioBleScreen` sigue siendo quien crea `BleManager`/
 * `FirebaseRepository`/`BackendEventRepository` y dispara el
 * `LaunchedEffect` del evento BLE (sin cambios ahí), y los pasa acá ya
 * listos.
 */
@Composable
fun UsuarioInicioScreen(
    context: Context,
    bleManager: BleManager,
    firebaseRepository: FirebaseRepository,
    backendEventRepository: BackendEventRepository,
    usuarioId: String,
    onVerHistorial: () -> Unit,
    onAbrirConexion: () -> Unit,
    onOpenAjustes: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GuardianTopBar(
            title = "GuardianApp",
            subtitle = "Modo Usuario",
            leading = { GuardianLogoBadge() },
            trailing = {
                GuardianIconButton(icon = Icons.Default.Person, contentDescription = "Perfil", onClick = onOpenAjustes)
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        CodigoUsuarioCard(usuarioId = usuarioId)
        Spacer(modifier = Modifier.height(24.dp))

        GuardianSectionTitle(
            title = "Accesos rápidos",
            trailingText = "Ver todos",
            onTrailingClick = onVerHistorial
        )
        Spacer(modifier = Modifier.height(12.dp))
        AlertaSection(
            notifyState = backendEventRepository.notifyState,
            onEventoClick = { evento ->
                backendEventRepository.notifyEvent(
                    type = evento.type,
                    message = evento.message,
                    deviceId = EventConstants.DEFAULT_DEVICE_ID,
                    usuarioId = usuarioId
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        ConexionEsp32ResumenCard(bleManager = bleManager, onClick = onAbrirConexion)
    }
}

/**
 * FASE 6 + rediseño: tarjeta "hero clara" (fondo azul muy claro) con el
 * código de 6 caracteres y un botón para copiarlo al portapapeles. No afirma
 * "conectado a tu apoderado": el Usuario no tiene forma de comprobar en
 * tiempo real si ya lo vinculó un Apoderado (las reglas de Firestore de la
 * Fase 6 bloquean esa lectura desde el cliente, ver `firebase/firestore.rules`
 * y el comentario equivalente del lado Apoderado en `ApoderadoInicioScreen`)
 * — mostrar eso sería inventar un dato, así que en su lugar se explica qué
 * hacer con el código, que sí es información real.
 */
@Composable
private fun CodigoUsuarioCard(usuarioId: String) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    GuardianStatusCard(style = GuardianHeroStyle.SOFT) {
        val textColor = GuardianHeroStyle.SOFT.contentColor()
        Text(text = "Hola 👋", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tu código de usuario",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = usuarioId,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color = Color.White.copy(alpha = 0.6f), shape = CircleShape)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(usuarioId))
                        Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GuardianIcons.ContentCopy,
                    contentDescription = "Copiar código",
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color = textColor.copy(alpha = 0.5f), shape = CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Comparte tu código con tu Apoderado para vincularlo.",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * FASE 3 + rediseño: resumen compacto y tocable (abre [UsuarioConectarScreen])
 * del estado real de la conexión BLE — no simula ningún dato, solo lee
 * [BleManager].
 */
@Composable
private fun ConexionEsp32ResumenCard(bleManager: BleManager, onClick: () -> Unit) {
    val conectado = bleManager.connectionState == BleConnectionState.CONNECTED
    val interactionSource = remember { MutableInteractionSource() }
    GuardianCard(modifier = Modifier.pressScale(interactionSource).clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = GuardianIcons.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Conexión con el ESP32", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "GuardianApp se conecta por Bluetooth para buscar el ESP32.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (conectado) successColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (conectado) "Conectado" else "Desconectado",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (conectado) successColor() else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * FASE 7 + rediseño: 4 tarjetas de "Accesos rápidos" en una grilla 2x2, con
 * [GuardianActionCard] (Emergencia incluida — ya no tiene un componente
 * aparte, solo un tinte más fuerte, ver esa función). Cada una dispara el
 * mismo camino que en el futuro disparará BLE:
 * [BackendEventRepository.notifyEvent]. Se deshabilitan mientras hay un
 * envío en curso para evitar toques duplicados accidentales.
 */
@Composable
private fun AlertaSection(
    notifyState: BackendNotifyState,
    onEventoClick: (SimulatedEvent) -> Unit
) {
    val enviando = notifyState is BackendNotifyState.Sending

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SimulatedEvent.entries.chunked(2).forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                fila.forEach { evento ->
                    val category = categoryFor(evento.type)
                    GuardianActionCard(
                        title = evento.label,
                        subtitle = evento.actionLabel,
                        color = category.color,
                        enabled = !enviando,
                        emphasized = evento.isCritical,
                        onClick = { onEventoClick(evento) },
                        modifier = Modifier.weight(1f),
                        icon = { tint -> CategoryIcon(kind = category.kind, tint = tint, modifier = Modifier.size(22.dp)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        AnimatedStatus(targetState = notifyState) { state -> AlertaStatusText(state) }
    }
}

@Composable
private fun AlertaStatusText(notifyState: BackendNotifyState) {
    when (notifyState) {
        is BackendNotifyState.Idle -> Unit
        is BackendNotifyState.Sending -> Text(text = "Enviando alerta...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        is BackendNotifyState.Success -> Text(text = "✓ Alerta enviada correctamente", color = successColor())
        is BackendNotifyState.Error -> Text(
            text = "✕ ${notifyState.message}",
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}
