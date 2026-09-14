package com.example.guardianapp.screens.usuario

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.guardianapp.backend.BackendEventRepository
import com.example.guardianapp.backend.BackendNotifyState
import com.example.guardianapp.ble.BleConnectionState
import com.example.guardianapp.ble.BleManager
import com.example.guardianapp.ble.BlePermissions
import com.example.guardianapp.ble.DiscoveredDevice
import com.example.guardianapp.firebase.EventUploadState
import com.example.guardianapp.firebase.FirebaseRepository
import com.example.guardianapp.ui.components.AnimatedStatus
import com.example.guardianapp.ui.components.GuardianCard
import com.example.guardianapp.ui.components.GuardianIconBadge
import com.example.guardianapp.ui.components.GuardianTopBar
import com.example.guardianapp.ui.icons.GuardianIcons
import com.example.guardianapp.ui.theme.ButtonShape
import com.example.guardianapp.ui.theme.successColor

/**
 * Pestaña "Conectar" del Usuario, sobre `design/guardianapp-ui-reference.png`:
 * la pantalla de conexión Bluetooth/ESP32 que antes vivía embebida en Inicio
 * (Fase 3, sin cambios de lógica — el escaneo/conexión BLE sigue siendo el
 * mismo [BleManager], creado y poseído por `UsuarioBleScreen`). Solo se
 * movió la UI acá y se le agregó el resumen "Bluetooth ✓ Activado / ESP32
 * ✓ Conectado" que pide la referencia; `UsuarioInicioScreen` ahora solo
 * muestra una tarjeta compacta de estado con un enlace a esta pantalla.
 */
@Composable
fun UsuarioConectarScreen(
    context: Context,
    bleManager: BleManager,
    firebaseRepository: FirebaseRepository,
    backendEventRepository: BackendEventRepository
) {
    var permissionsGranted by remember {
        mutableStateOf(BlePermissions.hasRequiredPermissions(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> permissionsGranted = results.values.all { it } }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { bleManager.refreshBluetoothState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GuardianTopBar(
            title = "Conectar dispositivo",
            subtitle = "Bluetooth y ESP32",
            leading = { GuardianIconBadge(icon = GuardianIcons.Bluetooth) }
        )
        Spacer(modifier = Modifier.height(24.dp))

        GuardianCard {
            ResumenEstadoRow(
                label = "Bluetooth",
                ok = bleManager.bluetoothEnabled,
                okText = "Activado",
                notOkText = "Desactivado"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            ResumenEstadoRow(
                label = "ESP32",
                ok = bleManager.connectionState == BleConnectionState.CONNECTED,
                okText = "Conectado",
                notOkText = if (bleManager.connectionState == BleConnectionState.CONNECTING) "Conectando..." else "Desconectado"
            )
            bleManager.connectedDeviceName?.let { name ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dispositivo: $name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        GuardianCard {
            when {
                !BlePermissions.isBluetoothSupported(context) -> {
                    Text(
                        text = "Este dispositivo no tiene Bluetooth Low Energy.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                !permissionsGranted -> {
                    Text(
                        text = "GuardianApp necesita permiso de Bluetooth para buscar el ESP32.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(BlePermissions.requiredRuntimePermissions()) },
                        shape = ButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Conceder permisos")
                    }
                }

                !bleManager.bluetoothEnabled -> {
                    Text(text = "Bluetooth está desactivado.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                        shape = ButtonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Activar Bluetooth")
                    }
                }

                else -> {
                    bleManager.errorMessage?.let { message ->
                        Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    when (bleManager.connectionState) {
                        BleConnectionState.CONNECTED -> ConnectedSection(bleManager, firebaseRepository, backendEventRepository)
                        BleConnectionState.CONNECTING -> Text("Conectando...", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        BleConnectionState.DISCONNECTED -> ScanSection(bleManager = bleManager, enabled = bleManager.bluetoothEnabled)
                    }
                }
            }
        }
    }
}

/** Fila "Bluetooth ✓ Activado" / "ESP32 ✓ Conectado" del resumen de arriba. */
@Composable
private fun ResumenEstadoRow(label: String, ok: Boolean, okText: String, notOkText: String) {
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

@Composable
private fun ScanSection(bleManager: BleManager, enabled: Boolean) {
    Text(text = "Dispositivos encontrados:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))

    if (bleManager.foundDevices.isEmpty()) {
        Text(
            text = if (bleManager.isScanning) "Buscando dispositivos..." else "Ningún dispositivo encontrado todavía.",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        bleManager.foundDevices.forEach { discovered ->
            DiscoveredDeviceRow(discovered = discovered, onConectar = { bleManager.connect(discovered.device) })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { bleManager.startScan() },
        enabled = enabled && !bleManager.isScanning,
        shape = ButtonShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (bleManager.isScanning) "Buscando..." else "Buscar dispositivo")
    }
}

@Composable
private fun DiscoveredDeviceRow(discovered: DiscoveredDevice, onConectar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = discovered.name)
        Button(onClick = onConectar, shape = ButtonShape) {
            Text("Conectar")
        }
    }
}

@Composable
private fun ConnectedSection(
    bleManager: BleManager,
    firebaseRepository: FirebaseRepository,
    backendEventRepository: BackendEventRepository
) {
    Text(
        text = bleManager.connectedDeviceName ?: "ESP32 Guardian",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Estado:", modifier = Modifier.fillMaxWidth())
    Text(text = "Conectado", style = MaterialTheme.typography.bodyLarge, color = successColor(), modifier = Modifier.fillMaxWidth())

    Spacer(modifier = Modifier.height(24.dp))
    val event = bleManager.lastEvent
    if (event != null) {
        Text(text = "Evento recibido:", modifier = Modifier.fillMaxWidth())
        Text(text = event.message, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth())
        Text(text = event.receivedAt, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        FirebaseStatusSection(uploadState = firebaseRepository.uploadState)
        Spacer(modifier = Modifier.height(16.dp))
        BackendStatusSection(notifyState = backendEventRepository.notifyState)
    } else {
        Text(text = "Esperando evento del ESP32...", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = { bleManager.disconnect() }, shape = ButtonShape, modifier = Modifier.fillMaxWidth()) {
        Text("Desconectar")
    }
}

/** FASE 4: muestra el resultado del envío del último evento a Firestore. */
@Composable
private fun FirebaseStatusSection(uploadState: EventUploadState) {
    Text(text = "Firebase:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
    AnimatedStatus(targetState = uploadState) { state ->
        when (state) {
            is EventUploadState.Idle -> Text(text = "En espera.")
            is EventUploadState.Sending -> Text(text = "Enviando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            is EventUploadState.Success -> Text(text = "✓ Evento enviado correctamente", color = successColor())
            is EventUploadState.Error -> Text(
                text = "✗ ${state.message}",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** FASE 5: muestra el resultado del aviso HTTP al backend (el que dispara FCM al Apoderado). */
@Composable
private fun BackendStatusSection(notifyState: BackendNotifyState) {
    Text(text = "Notificación al Apoderado:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
    AnimatedStatus(targetState = notifyState) { state ->
        when (state) {
            is BackendNotifyState.Idle -> Text(text = "En espera.")
            is BackendNotifyState.Sending -> Text(text = "Enviando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            is BackendNotifyState.Success -> Text(text = "✓ Backend notificado correctamente", color = successColor())
            is BackendNotifyState.Error -> Text(
                text = "✗ ${state.message}",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}
