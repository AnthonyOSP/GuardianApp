package com.example.guardianapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.guardianapp.fcm.FcmTokenRepository
import com.example.guardianapp.firebase.VinculacionRepository
import com.example.guardianapp.identity.LocalIdentity
import com.example.guardianapp.navigation.AppTab
import com.example.guardianapp.navigation.GuardianBottomBar
import com.example.guardianapp.screens.apoderado.ApoderadoAjustesScreen
import com.example.guardianapp.screens.apoderado.ApoderadoConectarScreen
import com.example.guardianapp.screens.apoderado.ApoderadoHistorialScreen
import com.example.guardianapp.screens.apoderado.ApoderadoInicioScreen
import com.example.guardianapp.ui.components.AnimatedStatus
import com.example.guardianapp.ui.theme.GuardianAppTheme

/**
 * FASE 5, pantalla del rol Apoderado. Desde la Fase 9 es solo el "host" de
 * la navegación inferior (rediseño sobre
 * `design/guardianapp-ui-reference.png`: Conectar/Inicio/Ajustes, ver
 * `navigation/AppTab.kt`) — el contenido de cada pestaña vive en
 * `screens/apoderado/`. Este archivo sigue siendo dueño de
 * [FcmTokenRepository]/[VinculacionRepository] y del `apoderadoId`:
 * viven arriba del `when(selectedTab)`, así que cambiar de pestaña nunca
 * pierde el registro FCM ni el resultado de la última vinculación.
 *
 * Historial ya no es una pestaña de la barra inferior — [historialVisible]
 * la muestra por encima, como una pantalla "apilada" sobre Inicio, igual que
 * en `UsuarioBleScreen`.
 */
@Composable
fun ApoderadoScreen(onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    val fcmTokenRepository = remember { FcmTokenRepository(context) }
    val vinculacionRepository = remember { VinculacionRepository(context) }
    // FASE 6: identidad anónima persistida de este Apoderado (ver LocalIdentity).
    val apoderadoId = remember { LocalIdentity.getOrCreateApoderadoId(context) }

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
                ApoderadoHistorialScreen(onBack = { historialVisible = false })
            } else {
                AnimatedStatus(targetState = selectedTab) { tab ->
                    when (tab) {
                        AppTab.CONECTAR -> ApoderadoConectarScreen(registrationState = fcmTokenRepository.registrationState)
                        AppTab.INICIO -> ApoderadoInicioScreen(
                            fcmTokenRepository = fcmTokenRepository,
                            vinculacionRepository = vinculacionRepository,
                            apoderadoId = apoderadoId,
                            onVerHistorialCompleto = { historialVisible = true },
                            onOpenAjustes = { selectedTab = AppTab.AJUSTES }
                        )
                        AppTab.AJUSTES -> ApoderadoAjustesScreen(
                            registrationState = fcmTokenRepository.registrationState,
                            apoderadoId = apoderadoId,
                            onCerrarSesion = onCerrarSesion
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ApoderadoScreenPreview() {
    GuardianAppTheme {
        ApoderadoScreen(onCerrarSesion = {})
    }
}
