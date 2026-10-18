package com.example.guardianapp.firebase

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * FASE 6: escribe la vinculación Usuario↔Apoderado en Firestore, desde el
 * teléfono del Apoderado (que es quien conoce ambos IDs en el momento de
 * vincularse: el suyo propio y el código de 6 caracteres que le compartió
 * el Usuario). Mismo patrón "clase expone `mutableStateOf`, la UI solo lee"
 * que el resto de los repositorios del proyecto.
 *
 * El documento resultante, `vinculaciones/{usuarioId}`, es lo que el
 * backend (`backend/src/routes/events.js`) usa para saber a qué
 * `apoderadoId` —y por lo tanto a qué token(s) FCM— notificar, en vez de
 * hacer broadcast a todos los tokens registrados.
 *
 * IMPORTANTE: el código de 6 caracteres NO es una medida de seguridad, es
 * solo un mecanismo de emparejamiento sencillo para esta fase — ver el
 * comentario sobre `vinculaciones` en `firebase/firestore.rules` y
 * `firebase/README.md` § Fase 6. Se reemplazará cuando exista Firebase
 * Authentication (fase futura, no implementada todavía).
 */
class VinculacionRepository(private val context: Context) {

    var vinculacionState by mutableStateOf<VinculacionState>(VinculacionState.Idle)
        private set

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Código del Usuario con el que este Apoderado se vinculó por última vez
     * con éxito, o `null` si nunca se vinculó. Se persiste localmente para
     * que la pantalla muestre "Vinculado con …" en vez del formulario cada
     * vez que se abre la app (`vinculaciones` no es legible desde el
     * cliente, ver `firestore.rules`, así que no se puede consultar).
     *
     * Es solo lo que este teléfono recuerda: si otro Apoderado se vincula
     * después al mismo código, sobreescribe `vinculaciones/{código}` y este
     * valor queda desactualizado sin que el teléfono se entere (mismo
     * caveat "sin Auth todavía" que el resto de la vinculación).
     */
    var linkedUsuarioId by mutableStateOf(prefs.getString(KEY_LINKED_USUARIO_ID, null))
        private set

    /**
     * Vincula el código de Usuario [usuarioId] con este Apoderado
     * ([apoderadoId]). Re-vincular el mismo código sobreescribe la
     * vinculación anterior (por diseño: es una relación 1 a 1, ver
     * `firestore.rules`). No lanza excepciones.
     */
    fun vincular(usuarioId: String, apoderadoId: String) {
        val code = usuarioId.trim().uppercase()
        if (code.isEmpty()) {
            vinculacionState = VinculacionState.Error("Ingresa el código de tu Usuario.")
            return
        }
        if (!isInternetAvailable(context)) {
            vinculacionState = VinculacionState.Error("Sin conexión a Internet. No se pudo vincular.")
            return
        }

        vinculacionState = VinculacionState.Sending

        try {
            FirebaseFirestore.getInstance()
                .collection(VINCULACIONES_COLLECTION)
                .document(code)
                .set(
                    hashMapOf(
                        "apoderadoId" to apoderadoId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .addOnSuccessListener {
                    prefs.edit { putString(KEY_LINKED_USUARIO_ID, code) }
                    linkedUsuarioId = code
                    vinculacionState = VinculacionState.Success
                }
                .addOnFailureListener { e -> vinculacionState = VinculacionState.Error(describeFirebaseError(e)) }
        } catch (e: Exception) {
            vinculacionState = VinculacionState.Error(describeFirebaseError(e))
        }
    }

    companion object {
        const val VINCULACIONES_COLLECTION = "vinculaciones"
        private const val PREFS_NAME = "guardian_vinculacion"
        private const val KEY_LINKED_USUARIO_ID = "linked_usuario_id"
    }
}
