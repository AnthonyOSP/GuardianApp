package com.example.guardianapp.firebase

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
                .addOnSuccessListener { vinculacionState = VinculacionState.Success }
                .addOnFailureListener { e -> vinculacionState = VinculacionState.Error(describeFirebaseError(e)) }
        } catch (e: Exception) {
            vinculacionState = VinculacionState.Error(describeFirebaseError(e))
        }
    }

    companion object {
        const val VINCULACIONES_COLLECTION = "vinculaciones"
    }
}
