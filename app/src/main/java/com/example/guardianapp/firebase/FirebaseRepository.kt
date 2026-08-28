package com.example.guardianapp.firebase

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.guardianapp.EventConstants
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * FASE 4: registra en Cloud Firestore los eventos que [BleManager][
 * com.example.guardianapp.ble.BleManager] recibe del ESP32. Es el único
 * punto del proyecto que conoce el SDK de Firebase; la UI (Compose) y la
 * capa BLE no dependen de él directamente, solo llaman a [logEvent] y leen
 * [uploadState] (mismo patrón que [BleManager][
 * com.example.guardianapp.ble.BleManager]: la clase expone estado
 * `mutableStateOf`, la UI lo observa).
 *
 * Estructura de cada documento en la colección "events":
 *   type: "ESP32_EVENT", message, deviceId, source: "esp32",
 *   timestamp: server timestamp (FieldValue.serverTimestamp()).
 */
class FirebaseRepository(private val context: Context) {

    var uploadState by mutableStateOf<EventUploadState>(EventUploadState.Idle)
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    /** Se incrementa en cada llamada para poder ignorar respuestas/timeouts de envíos ya obsoletos. */
    private var requestSeq = 0

    /**
     * Envía [message] a la colección "events". No lanza excepciones: cualquier
     * fallo (Firebase no configurado, sin Internet, error de Firestore,
     * timeout, evento vacío) se refleja en [uploadState] como [EventUploadState.Error].
     */
    fun logEvent(message: String, deviceId: String = EventConstants.DEFAULT_DEVICE_ID) {
        if (message.isBlank()) {
            uploadState = EventUploadState.Error("Evento vacío: no se envió a Firebase.")
            return
        }
        if (!isInternetAvailable(context)) {
            uploadState = EventUploadState.Error("Sin conexión a Internet. No se pudo enviar el evento.")
            return
        }

        val firestore = try {
            FirebaseFirestore.getInstance()
        } catch (e: IllegalStateException) {
            // Ocurre si FirebaseApp no se inicializó (p. ej. falta google-services.json).
            uploadState = EventUploadState.Error("Firebase no está configurado en la app.")
            return
        }

        val thisRequest = ++requestSeq
        uploadState = EventUploadState.Sending
        armTimeout(thisRequest)

        val data = hashMapOf(
            "type" to EventConstants.EVENT_TYPE,
            "message" to message,
            "deviceId" to deviceId,
            "source" to SOURCE,
            "timestamp" to FieldValue.serverTimestamp()
        )

        try {
            firestore.collection(EVENTS_COLLECTION)
                .add(data)
                .addOnSuccessListener {
                    if (thisRequest == requestSeq) {
                        cancelPendingTimeout()
                        uploadState = EventUploadState.Success
                    }
                }
                .addOnFailureListener { exception ->
                    if (thisRequest == requestSeq) {
                        cancelPendingTimeout()
                        uploadState = EventUploadState.Error(describeFirebaseError(exception))
                    }
                }
        } catch (e: Exception) {
            cancelPendingTimeout()
            uploadState = EventUploadState.Error(describeFirebaseError(e))
        }
    }

    /** Debe llamarse cuando la pantalla que usa este repositorio sale de composición. */
    fun dispose() {
        cancelPendingTimeout()
    }

    private fun armTimeout(request: Int) {
        cancelPendingTimeout()
        val runnable = Runnable {
            if (request == requestSeq) {
                uploadState = EventUploadState.Error("Tiempo de espera agotado al enviar el evento.")
            }
        }
        timeoutRunnable = runnable
        mainHandler.postDelayed(runnable, TIMEOUT_MS)
    }

    private fun cancelPendingTimeout() {
        timeoutRunnable?.let(mainHandler::removeCallbacks)
        timeoutRunnable = null
    }

    companion object {
        private const val EVENTS_COLLECTION = "events"
        private const val SOURCE = "esp32"
        private const val TIMEOUT_MS = 10_000L
    }
}
