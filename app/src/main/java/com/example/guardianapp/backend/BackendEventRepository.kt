package com.example.guardianapp.backend

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
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

    /**
     * Avisa al backend de un evento nuevo. No lanza excepciones: cualquier
     * fallo (backend no configurado, sin Internet, timeout, error HTTP)
     * termina en [notifyState] como [BackendNotifyState.Error].
     */
    fun notifyEvent(type: String, message: String, deviceId: String) {
        if (message.isBlank()) {
            notifyState = BackendNotifyState.Error("Evento vacío: no se avisó al backend.")
            return
        }
        val baseUrl = BuildConfig.BACKEND_BASE_URL
        if (baseUrl.isBlank()) {
            notifyState = BackendNotifyState.Error(
                "El backend no está configurado (falta BACKEND_BASE_URL en local.properties)."
            )
            return
        }
        if (!isInternetAvailable(context)) {
            notifyState = BackendNotifyState.Error("Sin conexión a Internet. No se pudo avisar al backend.")
            return
        }

        val thisRequest = ++requestSeq
        notifyState = BackendNotifyState.Sending

        executor.execute {
            val result = runCatching { postEvent(baseUrl, type, message, deviceId) }
            mainHandler.post {
                if (thisRequest != requestSeq) return@post // respuesta de un envío ya obsoleto
                notifyState = result.fold(
                    onSuccess = { BackendNotifyState.Success },
                    onFailure = { e -> BackendNotifyState.Error(describeError(e)) }
                )
            }
        }
    }

    /** Corre en el hilo de [executor]; nunca en el hilo principal. */
    private fun postEvent(baseUrl: String, type: String, message: String, deviceId: String) {
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
                .toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }

            val code = connection.responseCode
            if (code !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IOException("El backend respondió $code${errorBody?.let { b -> ": $b" }.orEmpty()}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun describeError(e: Throwable): String = when (e) {
        is SocketTimeoutException ->
            "El backend no respondió a tiempo (¿estará despertando en Render? intenta de nuevo)."
        is IOException -> e.message ?: "No se pudo contactar al backend."
        else -> e.message ?: "Error desconocido al avisar al backend."
    }

    /** Debe llamarse cuando la pantalla que usa este repositorio sale de composición. */
    fun dispose() {
        executor.shutdownNow()
    }
}
