package com.example.guardianapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.guardianapp.ui.icons.GuardianIcons
import com.example.guardianapp.ui.theme.ThemeMode
import com.example.guardianapp.ui.theme.ThemeSettings

/**
 * Fila "Modo oscuro" de Ajustes (compartida por Usuario y Apoderado): muestra
 * el tema actual y, al tocarla, un diálogo para elegir Según el sistema /
 * Claro / Oscuro. El cambio se aplica al instante en toda la app y se guarda
 * (ver [ThemeSettings]).
 */
@Composable
fun ThemeSettingRow() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    SettingsRow(
        icon = GuardianIcons.DarkMode,
        title = "Modo oscuro",
        subtitle = ThemeSettings.mode.label,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Modo oscuro") },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = ThemeSettings.mode == mode,
                                    role = Role.RadioButton,
                                    onClick = {
                                        ThemeSettings.setMode(context, mode)
                                        showDialog = false
                                    }
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = ThemeSettings.mode == mode, onClick = null)
                            Text(text = mode.label, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cerrar") }
            }
        )
    }
}
