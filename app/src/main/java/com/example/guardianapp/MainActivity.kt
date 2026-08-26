package com.example.guardianapp

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

/**
 * FASE 2: selección de rol (Usuario / Apoderado) y pantallas locales
 * correspondientes. El rol es solo un estado en memoria (sin persistencia,
 * sin cuentas, sin autenticación) — se pierde al cerrar la app o al
 * presionar "Cerrar sesión".
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GuardianAppRoot()
            }
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
            Role.APODERADO -> RoleHomeScreen(role = role, onCerrarSesion = { selectedRole = null })
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
