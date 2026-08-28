package com.example.guardianapp.firebase

/**
 * Estado de la vinculación Usuario↔Apoderado (Fase 6), tal como lo ve
 * [com.example.guardianapp.ApoderadoScreen]. Ver [VinculacionRepository.vincular].
 */
sealed class VinculacionState {
    /** Todavía no se intentó vincular. */
    data object Idle : VinculacionState()

    /** Escritura en curso contra Firestore. */
    data object Sending : VinculacionState()

    /** La vinculación quedó guardada en "vinculaciones". */
    data object Success : VinculacionState()

    /** No se pudo vincular; [message] es apto para mostrar en pantalla. */
    data class Error(val message: String) : VinculacionState()
}
