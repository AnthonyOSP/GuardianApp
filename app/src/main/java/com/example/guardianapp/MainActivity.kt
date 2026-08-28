package com.example.guardianapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.guardianapp.fcm.GuardianNotificationCenter
import com.example.guardianapp.fcm.GuardianNotifications
import com.example.guardianapp.fcm.NotifiedEvent

/**
 * FASE 2: selección de rol (Usuario / Apoderado) y pantallas locales
 * correspondientes. El rol es solo un estado en memoria (sin persistencia,
 * sin cuentas, sin autenticación) — se pierde al cerrar la app o al
 * presionar "Cerrar sesión".
 *
 * FASE 5: además de su ciclo de vida normal, esta actividad puede abrirse
 * al tocar una notificación FCM. Con la app en segundo plano/cerrada,
 * Android la lanza automáticamente con los datos del evento como extras del
 * Intent (ver [GuardianFirebaseMessagingService][
 * com.example.guardianapp.fcm.GuardianFirebaseMessagingService] para el
 * caso "app en primer plano", que no pasa por aquí). `launchMode=singleTop`
 * en el manifest hace que, si la actividad ya está en la pantalla, se
 * reutilice vía [onNewIntent] en vez de crear una instancia nueva (lo que
 * perdería el rol ya seleccionado, que no se persiste).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        setContent {
            MaterialTheme {
                GuardianAppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val type = intent?.getStringExtra(GuardianNotifications.EXTRA_EVENT_TYPE)
        val message = intent?.getStringExtra(GuardianNotifications.EXTRA_EVENT_MESSAGE)
        val deviceId = intent?.getStringExtra(GuardianNotifications.EXTRA_EVENT_DEVICE_ID)
        if (!type.isNullOrBlank() && !message.isNullOrBlank() && !deviceId.isNullOrBlank()) {
            GuardianNotificationCenter.onEventReceived(NotifiedEvent(type, message, deviceId))
        }
    }
}

@Composable
fun GuardianAppRoot() {
    var selectedRole by remember { mutableStateOf<Role?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val role = selectedRole
        when (role) {
            null -> RoleSelectionScreen(onRoleSelected = { selectedRole = it })
            Role.USUARIO -> UsuarioBleScreen(onCerrarSesion = { selectedRole = null })
            Role.APODERADO -> ApoderadoScreen(onCerrarSesion = { selectedRole = null })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GuardianAppRootPreview() {
    MaterialTheme {
        GuardianAppRoot()
    }
}
