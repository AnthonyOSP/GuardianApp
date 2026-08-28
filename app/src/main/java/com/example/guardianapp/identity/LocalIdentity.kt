package com.example.guardianapp.identity

import android.content.Context
import androidx.core.content.edit
import java.util.UUID
import kotlin.random.Random

/**
 * FASE 6: identificadores anónimos por instalación, persistidos con
 * SharedPreferences, usados para vincular Usuario↔Apoderado sin Firebase
 * Authentication todavía (ver `firebase/README.md` § Fase 6 y el comentario
 * sobre `vinculaciones` en `firestore.rules`).
 *
 * Excepción puntual a la decisión de la Fase 2 de no persistir nada: el rol
 * elegido sigue sin persistirse (se resetea con "Cerrar sesión", ver
 * `MainActivity.GuardianAppRoot`); esto es distinto — es la identidad
 * anónima del teléfono, y hace falta persistirla para que la vinculación
 * sobreviva a cerrar sesión/reabrir la app.
 *
 * Cuando se implemente Firebase Authentication (fase futura, NO en esta),
 * estos valores dejan de generarse localmente y pasan a ser el `uid` real
 * de Firebase Auth — el esquema de Firestore
 * (`vinculaciones/{usuarioId}`, `apoderadoTokens.apoderadoId`) no cambia,
 * solo cambia de dónde sale el valor que se guarda en esos campos.
 */
object LocalIdentity {
    private const val PREFS_NAME = "guardian_identity"
    private const val KEY_USUARIO_ID = "usuario_id"
    private const val KEY_APODERADO_ID = "apoderado_id"

    // Sin caracteres ambiguos (0/O, 1/I) para que sea fácil de leer y
    // tipear a mano al vincular.
    private const val USUARIO_ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val USUARIO_ID_LENGTH = 6

    /**
     * Código corto (6 caracteres) que identifica a este Usuario. Se genera
     * una sola vez por instalación y se muestra en pantalla
     * ([com.example.guardianapp.UsuarioBleScreen]) para que el Apoderado lo
     * escriba al vincularse.
     *
     * NO es una medida de seguridad: es solo un mecanismo de emparejamiento
     * sencillo para esta fase — cualquiera que conozca el código puede
     * vincularse a este Usuario. Ver el comentario sobre `vinculaciones` en
     * `firestore.rules`.
     */
    fun getOrCreateUsuarioId(context: Context): String =
        getOrCreate(context, KEY_USUARIO_ID) { generateUsuarioCode() }

    /** UUID interno del Apoderado; nunca se muestra ni se tipea a mano. */
    fun getOrCreateApoderadoId(context: Context): String =
        getOrCreate(context, KEY_APODERADO_ID) { UUID.randomUUID().toString() }

    private fun getOrCreate(context: Context, key: String, generate: () -> String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(key, null)?.let { return it }
        val value = generate()
        prefs.edit { putString(key, value) }
        return value
    }

    private fun generateUsuarioCode(): String {
        val random = Random.Default
        return (1..USUARIO_ID_LENGTH)
            .map { USUARIO_ID_ALPHABET[random.nextInt(USUARIO_ID_ALPHABET.length)] }
            .joinToString("")
    }
}
