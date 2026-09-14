package com.example.guardianapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.guardianapp.backend.BackendEventRepository
import com.example.guardianapp.ble.BleManager
import com.example.guardianapp.firebase.FirebaseRepository
import com.example.guardianapp.identity.LocalIdentity
import com.example.guardianapp.navigation.AppTab
import com.example.guardianapp.navigation.GuardianBottomBar
import com.example.guardianapp.screens.usuario.UsuarioAjustesScreen
import com.example.guardianapp.screens.usuario.UsuarioConectarScreen
import com.example.guardianapp.screens.usuario.UsuarioHistorialScreen
import com.example.guardianapp.screens.usuario.UsuarioInicioScreen
import com.example.guardianapp.ui.components.AnimatedStatus
import com.example.guardianapp.ui.theme.GuardianAppTheme

/**
 * FASE 3, pantalla del rol Usuario. Desde la Fase 9 es solo el "host" de la
 * navegación inferior (rediseño sobre `design/guardianapp-ui-reference.png`:
 * Conectar/Inicio/Ajustes, ver `navigation/AppTab.kt`) — el contenido de
 * cada pestaña vive en `screens/usuario/`. Este archivo sigue siendo dueño
 * de todo lo que **no** debe reiniciarse al cambiar de pestaña: [BleManager],
 * [FirebaseRepository], [BackendEventRepository] y el `LaunchedEffect` que
 * reacciona a un evento BLE nuevo — exactamente igual que antes de este
 * rediseño, sin cambios de lógica. Como viven arriba del `when(selectedTab)`,
 * cambiar de pestaña nunca los recrea ni pierde su estado (conexión BLE,
 * resultado del último envío, etc.).
 *
 * Historial ya no es una pestaña de la barra inferior (la referencia visual
 * solo pide 3 botones) — [historialVisible] la muestra por encima, como una
 * pantalla "apilada" sobre Inicio, alcanzable desde su enlace "Ver todos".
 */
@Composable
fun UsuarioBleScreen(onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    val bleManager = remember { BleManager(context) }
    val firebaseRepository = remember { FirebaseRepository(context) }
    val backendEventRepository = remember { BackendEventRepository(context) }
    // FASE 6: identidad anónima persistida de este Usuario (ver LocalIdentity).
    val usuarioId = remember { LocalIdentity.getOrCreateUsuarioId(context) }

    DisposableEffect(Unit) {
        bleManager.register()
        onDispose {
            bleManager.unregister()
            firebaseRepository.dispose()
            backendEventRepository.dispose()
        }
    }

    // FASE 4 + FASE 5: cada vez que llega un evento BLE nuevo, se dispara en
    // paralelo (a) el registro en Firestore y (b) el aviso HTTP al backend
    // propio, que es quien decide a qué Apoderado(s) notificar por FCM. Son
    // dos llamadas de red independientes: una puede fallar sin afectar a la
    // otra. BleManager no sabe nada de ninguna de las dos, solo expone
    // `lastEvent`.
    LaunchedEffect(bleManager.lastEvent) {
        bleManager.lastEvent?.let { event ->
            firebaseRepository.logEvent(event.message)
            backendEventRepository.notifyEvent(
                type = EventConstants.EVENT_TYPE,
                message = event.message,
                deviceId = EventConstants.DEFAULT_DEVICE_ID,
                usuarioId = usuarioId
            )
        }
    }

    var selectedTab by remember { mutableStateOf(AppTab.INICIO) }
    var historialVisible by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            GuardianBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    historialVisible = false
                    selectedTab = tab
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (historialVisible) {
                UsuarioHistorialScreen(
                    backendEventRepository = backendEventRepository,
                    onBack = { historialVisible = false }
                )
            } else {
                AnimatedStatus(targetState = selectedTab) { tab ->
                    when (tab) {
                        AppTab.CONECTAR -> UsuarioConectarScreen(
                            context = context,
                            bleManager = bleManager,
                            firebaseRepository = firebaseRepository,
                            backendEventRepository = backendEventRepository
                        )
                        AppTab.INICIO -> UsuarioInicioScreen(
                            context = context,
                            bleManager = bleManager,
                            firebaseRepository = firebaseRepository,
                            backendEventRepository = backendEventRepository,
                            usuarioId = usuarioId,
                            onVerHistorial = { historialVisible = true },
                            onAbrirConexion = { selectedTab = AppTab.CONECTAR },
                            onOpenAjustes = { selectedTab = AppTab.AJUSTES }
                        )
                        AppTab.AJUSTES -> UsuarioAjustesScreen(usuarioId = usuarioId, onCerrarSesion = onCerrarSesion)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UsuarioBleScreenPreview() {
    GuardianAppTheme {
        UsuarioBleScreen(onCerrarSesion = {})
    }
}
