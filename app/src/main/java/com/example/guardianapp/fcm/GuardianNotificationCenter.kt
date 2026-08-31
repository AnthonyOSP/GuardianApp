package com.example.guardianapp.fcm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Puente entre componentes que Android instancia por su cuenta
 * ([GuardianFirebaseMessagingService], `MainActivity.onNewIntent`) y la UI
 * de Compose ([ApoderadoScreen][com.example.guardianapp.ApoderadoScreen]).
 *
 * Es la única excepción en el proyecto al patrón "cada pantalla crea su
 * propio manager con `remember`" (usado por `BleManager` y
 * `FirebaseRepository`): un `FirebaseMessagingService` lo instancia el
 * sistema operativo, no la UI, así que no hay forma de inyectarle una
 * instancia por-pantalla. Un objeto singleton con estado de Compose es la
 * forma más simple de conectar ambos mundos sin agregar un
 * ViewModel/EventBus genérico.
 *
 * FASE 9 — historial del Apoderado: [history] es la fuente de datos de la
 * pantalla Historial. Antes de esta fase, este objeto solo guardaba
 * [lastEvent] (se pisaba con cada evento nuevo); ahora también acumula
 * cada evento recibido en esta ejecución del proceso, más reciente
 * primero. Deliberadamente **no** se agregó ninguna colección nueva de
 * Firestore para esto: `events` ya existe pero tiene `allow read: if false`
 * (nadie puede leerla desde el cliente, ver `firebase/firestore.rules`) y
 * ni siquiera guarda a qué Apoderado pertenece cada evento — reconstruir el
 * historial desde ahí exigiría cambiar reglas de seguridad y el esquema de
 * datos. En cambio, cada evento que el Apoderado recibió por FCM ya pasa
 * por acá (ver los dos call-sites de [onEventReceived]), así que acumularlo
 * en memoria es la extensión mínima de algo que ya existía.
 *
 * Limitación conocida y aceptada: vive en memoria mientras el proceso de
 * la app sigue vivo — se pierde si el proceso muere (no solo con la app en
 * segundo plano, que sí lo conserva). Si se necesita que sobreviva a
 * reinicios, el siguiente paso natural sería persistirlo en
 * `SharedPreferences` (mismo mecanismo que ya usa `LocalIdentity.kt`), no
 * implementado todavía para mantener este cambio acotado.
 */
object GuardianNotificationCenter {
    var lastEvent by mutableStateOf<NotifiedEvent?>(null)
        private set

    private val _history = mutableStateListOf<NotifiedEvent>()

    /** Más reciente primero. */
    val history: List<NotifiedEvent> get() = _history

    fun onEventReceived(event: NotifiedEvent) {
        lastEvent = event
        _history.add(0, event)
    }
}
