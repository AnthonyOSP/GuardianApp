package com.example.guardianapp.fcm

import androidx.compose.runtime.getValue
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
 */
object GuardianNotificationCenter {
    var lastEvent by mutableStateOf<NotifiedEvent?>(null)
        private set

    fun onEventReceived(event: NotifiedEvent) {
        lastEvent = event
    }
}
