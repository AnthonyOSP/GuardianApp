package com.example.guardianapp.backend

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.guardianapp.BuildConfig
import com.example.guardianapp.firebase.isInternetAvailable
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

/**
 * FASE 5 (backend en Render): avisa a `backend/` (ver su README) cuando
 * llega un evento BLE, para que sea el backend quien decida a qué
 * Apoderado(s) notificar y dispare FCM con credenciales de Admin SDK —
 * Android nunca las tiene. Mismo patrón "clase expone `mutableStateOf`, la
 * UI solo lee" que [com.example.guardianapp.firebase.FirebaseRepository] /
 * [com.example.guardianapp.fcm.FcmTokenRepository].
 *
 * Es independiente de `FirebaseRepository`: este aviso y el guardado en
 * Firestore son dos llamadas de red separadas, disparadas juntas desde
 * `UsuarioBleScreen.kt`, que pueden tener éxito o fallar sin depender una
 * de la otra.
 *
 * Usa deliberadamente `HttpURLConnection` en un hilo de fondo propio en vez
 * de agregar OkHttp/Retrofit: es una sola petición POST/JSON pequeña, y
 * este proyecto prefiere evitar dependencias nuevas cuando no hacen falta
 * (mismo criterio que usar Handler en vez de coroutines en el resto de la
 * capa de red del proyecto).
 */
class BackendEventRepository(private val context: Context) {

    var notifyState by mutableStateOf<BackendNotifyState>(BackendNotifyState.Idle)
        private set

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Se incrementa en cada llamada para poder ignorar respuestas de envíos ya obsoletos. */
    private var requestSeq = 0

    private val _sentHistory = mutableStateListOf<SentEvent>()

    /**
     * FASE 9 — historial del Usuario ("alertas que envié"): un registro por
     * cada intento que terminó en éxito o error (no por los estados
     * intermedios `Idle`/`Sending`), más reciente primero. Extensión mínima
     * de lo que ya existía (`notifyState`) — no hay una colección de
     * Firestore nueva ni se toca el backend; se arma enteramente con la
     * información que este repositorio ya maneja en cada llamada a
     * [notifyEvent]. Misma limitación que
     * [com.example.guardianapp.fcm.GuardianNotificationCenter.history]: vive
     * en memoria, se pierde si el proceso muere.
     */
    val sentHistory: List<SentEvent> get() = _sentHistory

    /**
     * Avisa al backend de un evento nuevo, identificando a este Usuario con
     * [usuarioId] (ver [com.example.guardianapp.identity.LocalIdentity]) —
     * es lo que el backend usa para resolver la vinculación con el
     * Apoderado correcto (Fase 6) en vez de hacer broadcast. No lanza
     * excepciones: cualquier fallo (backend no configurado, sin Internet,
     * timeout, error HTTP, Usuario sin vincular) termina en [notifyState]
     * como [BackendNotifyState.Error] — solo `notified > 0` en la respuesta
     * del backend cuenta como [BackendNotifyState.Success] (Fase 7: antes
     * de esto, un `{"ok":true,"notified":0}` por falta de vinculación se
     * mostraba igual que un envío exitoso, lo cual era engañoso).
     */
    fun notifyEvent(type: String, message: String, deviceId: String, usuarioId: String) {
        if (message.isBlank()) {
            setTerminalState(type, message, BackendNotifyState.Error("Evento vacío: no se avisó al backend."))
            return
        }
        val baseUrl = BuildConfig.BACKEND_BASE_URL
        if (baseUrl.isBlank()) {
            setTerminalState(
                type, message,
                BackendNotifyState.Error("El backend no está configurado (falta BACKEND_BASE_URL en local.properties).")
            )
            return
        }
        if (!isInternetAvailable(context)) {
            setTerminalState(type, message, BackendNotifyState.Error("Sin conexión a Internet. No se pudo avisar al backend."))
            return
        }

        val thisRequest = ++requestSeq
        notifyState = BackendNotifyState.Sending

        executor.execute {
            val result = runCatching { postEvent(baseUrl, type, message, deviceId, usuarioId) }
            mainHandler.post {
                if (thisRequest != requestSeq) return@post // respuesta de un envío ya obsoleto
                val state = result.fold(
                    onSuccess = { notified ->
                        if (notified > 0) {
                            BackendNotifyState.Success
                        } else {
                            BackendNotifyState.Error("El Usuario todavía no está vinculado a un Apoderado.")
                        }
                    },
                    onFailure = { e -> BackendNotifyState.Error(describeError(e)) }
                )
                setTerminalState(type, message, state)
            }
        }
    }

    /** Aplica un estado final (Success/Error, nunca Idle/Sending) y lo registra en [sentHistory]. */
    private fun setTerminalState(type: String, message: String, state: BackendNotifyState) {
        notifyState = state
        _sentHistory.add(0, SentEvent(type = type, message = message, sentAtMillis = System.currentTimeMillis(), success = state is BackendNotifyState.Success))
    }

    /**
     * Corre en el hilo de [executor]; nunca en el hilo principal. Devuelve
     * el campo `notified` de la respuesta del backend (0 si el Usuario
     * todavía no está vinculado a ningún Apoderado — ver
     * `backend/src/routes/events.js`).
     */
    private fun postEvent(baseUrl: String, type: String, message: String, deviceId: String, usuarioId: String): Int {
        val url = URL(baseUrl.trimEnd('/') + "/api/events")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("X-API-Key", BuildConfig.BACKEND_API_KEY)
            // Timeouts generosos: el plan free de Render duerme el servicio
            // tras inactividad y puede tardar ~30-50s en despertar con la
            // primera petición. Ver backend/README.md.
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000

            val body = JSONObject()
                .put("type", type)
                .put("message", message)
                .put("deviceId", deviceId)
                .put("usuarioId", usuarioId)
                .toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }

            val code = connection.responseCode
            if (code !in 200..299) {
                // Mensaje genérico a propósito: no repetimos el cuerpo crudo
                // de la respuesta al usuario (podría no ser apto para
                // mostrar), y nunca exponemos la API key en ningún mensaje.
                val userMessage = if (code == 401) {
                    "API key inválida o ausente."
                } else {
                    "No se pudo enviar la alerta. Comprueba tu conexión e inténtalo nuevamente."
                }
                throw IOException(userMessage)
            }

            val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return JSONObject(responseBody).optInt("notified", 0)
        } finally {
            connection.disconnect()
        }
    }

    private fun describeError(e: Throwable): String = when (e) {
        is SocketTimeoutException ->
            "El backend no respondió a tiempo (¿estará despertando en Render? intenta de nuevo)."
        is IOException -> e.message ?: "No se pudo enviar la alerta. Comprueba tu conexión e inténtalo nuevamente."
        else -> "No se pudo enviar la alerta. Comprueba tu conexión e inténtalo nuevamente."
    }

    /** Debe llamarse cuando la pantalla que usa este repositorio sale de composición. */
    fun dispose() {
        executor.shutdownNow()
    }
}

/** Un intento de envío ya resuelto (éxito o error) — ver [BackendEventRepository.sentHistory]. */
data class SentEvent(
    val type: String,
    val message: String,
    val sentAtMillis: Long,
    val success: Boolean
)
