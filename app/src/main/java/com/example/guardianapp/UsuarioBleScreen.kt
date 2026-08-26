package com.example.guardianapp

import android.bluetooth.BluetoothAdapter
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.guardianapp.ble.BleConnectionState
import com.example.guardianapp.ble.BleManager
import com.example.guardianapp.ble.BlePermissions
import com.example.guardianapp.ble.DiscoveredDevice
import com.example.guardianapp.firebase.EventUploadState
import com.example.guardianapp.firebase.FirebaseRepository

/**
 * FASE 3, pantalla del rol Usuario: buscar el ESP32 por BLE, conectarse y
 * mostrar el último evento recibido. Toda la lógica de Bluetooth vive en
 * [BleManager]; esta pantalla solo lee su estado y reacciona a acciones
 * del usuario (botones).
 */
@Composable
fun UsuarioBleScreen(onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    val bleManager = remember { BleManager(context) }
    val firebaseRepository = remember { FirebaseRepository(context) }

    DisposableEffect(Unit) {
        bleManager.register()
        onDispose {
            bleManager.unregister()
            firebaseRepository.dispose()
        }
    }

    // FASE 4: cada vez que llega un evento BLE nuevo, se reenvía a Firestore.
    // BleManager no sabe nada de Firebase; solo expone `lastEvent`.
    LaunchedEffect(bleManager.lastEvent) {
        bleManager.lastEvent?.let { event -> firebaseRepository.logEvent(event.message) }
    }

    var permissionsGranted by remember {
        mutableStateOf(BlePermissions.hasRequiredPermissions(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }
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
        Text(text = "GuardianApp", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Modo Usuario", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))

        when {
            !BlePermissions.isBluetoothSupported(context) -> {
                Text(
                    text = "Este dispositivo no tiene Bluetooth Low Energy.",
                    textAlign = TextAlign.Center
                )
            }

            !permissionsGranted -> {
                Text(
                    text = "GuardianApp necesita permiso de Bluetooth para buscar el ESP32.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(BlePermissions.requiredRuntimePermissions()) }) {
                    Text("Conceder permisos")
                }
            }

            else -> {
                BluetoothStatusSection(
                    enabled = bleManager.bluetoothEnabled,
                    onActivar = { enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                bleManager.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (bleManager.connectionState) {
                    BleConnectionState.CONNECTED -> ConnectedSection(bleManager, firebaseRepository)
                    BleConnectionState.CONNECTING -> Text("Conectando...")
                    BleConnectionState.DISCONNECTED -> ScanSection(
                        bleManager = bleManager,
                        enabled = bleManager.bluetoothEnabled
                    )
                }
            }
        }

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
private fun BluetoothStatusSection(enabled: Boolean, onActivar: () -> Unit) {
    Text(text = "Estado Bluetooth:", style = MaterialTheme.typography.labelLarge)
    Text(text = if (enabled) "Activado" else "Desactivado")
    if (!enabled) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onActivar) {
            Text("Activar Bluetooth")
        }
    }
}

@Composable
private fun ScanSection(bleManager: BleManager, enabled: Boolean) {
    Text(text = "Dispositivos encontrados:", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(8.dp))

    if (bleManager.foundDevices.isEmpty()) {
        Text(
            text = if (bleManager.isScanning) {
                "Buscando dispositivos..."
            } else {
                "Ningún dispositivo encontrado todavía."
            },
            textAlign = TextAlign.Center
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
        enabled = enabled && !bleManager.isScanning
    ) {
        Text(if (bleManager.isScanning) "Buscando..." else "Buscar dispositivos")
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
        Button(onClick = onConectar) {
            Text("Conectar")
        }
    }
}

@Composable
private fun ConnectedSection(bleManager: BleManager, firebaseRepository: FirebaseRepository) {
    Text(
        text = bleManager.connectedDeviceName ?: "ESP32 Guardian",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Estado:")
    Text(text = "Conectado", style = MaterialTheme.typography.bodyLarge)

    Spacer(modifier = Modifier.height(24.dp))
    val event = bleManager.lastEvent
    if (event != null) {
        Text(text = "Evento recibido:")
        Text(text = event.message, style = MaterialTheme.typography.headlineSmall)
        Text(text = event.receivedAt, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        FirebaseStatusSection(uploadState = firebaseRepository.uploadState)
    } else {
        Text(text = "Esperando evento del ESP32...", textAlign = TextAlign.Center)
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = { bleManager.disconnect() }) {
        Text("Desconectar")
    }
}

/** FASE 4: muestra el resultado del envío del último evento a Firestore. */
@Composable
private fun FirebaseStatusSection(uploadState: EventUploadState) {
    Text(text = "Firebase:", style = MaterialTheme.typography.labelLarge)
    when (uploadState) {
        is EventUploadState.Idle -> Text(text = "En espera.")
        is EventUploadState.Sending -> Text(text = "Enviando...")
        is EventUploadState.Success -> Text(text = "✓ Evento enviado correctamente")
        is EventUploadState.Error -> Text(
            text = "✗ ${uploadState.message}",
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsuarioBleScreenPreview() {
    MaterialTheme {
        UsuarioBleScreen(onCerrarSesion = {})
    }
}
