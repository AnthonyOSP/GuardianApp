package com.example.guardianapp.backend

/**
 * Estado del aviso al backend propio (`backend/`, Render) para que dispare
 * la notificación FCM al Apoderado. Es independiente del estado de
 * [com.example.guardianapp.firebase.EventUploadState] (que es sobre guardar
 * el evento en Firestore): son dos llamadas separadas que pueden fallar por
 * separado, ver `UsuarioBleScreen.kt`.
 */
sealed class BackendNotifyState {
    /** Todavía no se ha recibido ningún evento para avisar al backend. */
    data object Idle : BackendNotifyState()

    /** Petición HTTP en curso. Puede tardar: el plan free de Render duerme
     *  el servicio tras inactividad y tarda ~30-50s en despertar. */
    data object Sending : BackendNotifyState()

    /** El backend respondió 2xx. */
    data object Success : BackendNotifyState()

    /** No se pudo avisar al backend; [message] es apto para mostrar en pantalla. */
    data class Error(val message: String) : BackendNotifyState()
}
