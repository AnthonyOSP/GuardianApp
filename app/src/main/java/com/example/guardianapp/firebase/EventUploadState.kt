package com.example.guardianapp.firebase

/**
 * Estado del envío de un evento a Cloud Firestore, tal como lo ve la UI.
 * Ver [FirebaseRepository.logEvent].
 */
sealed class EventUploadState {
    /** Todavía no se ha recibido ningún evento para enviar. */
    data object Idle : EventUploadState()

    /** Escritura en curso contra Firestore. */
    data object Sending : EventUploadState()

    /** El documento se guardó correctamente en la colección "events". */
    data object Success : EventUploadState()

    /** No se pudo guardar el evento; [message] es apto para mostrar en pantalla. */
    data class Error(val message: String) : EventUploadState()
}
