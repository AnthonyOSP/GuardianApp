package com.example.guardianapp

/**
 * Metadatos fijos del ESP32 de pruebas, compartidos por todo lo que envía
 * un evento hacia afuera: el registro en Firestore
 * ([com.example.guardianapp.firebase.FirebaseRepository], Fase 4) y el
 * aviso al backend propio para notificar al Apoderado
 * ([com.example.guardianapp.backend.BackendEventRepository], Fase 5).
 *
 * `DEFAULT_DEVICE_ID` no es una MAC ni un identificador real de
 * dispositivo — es un valor fijo de prueba mientras no existe una relación
 * real Usuario↔Apoderado. Ver `firebase/README.md` y `backend/README.md`.
 */
object EventConstants {
    const val EVENT_TYPE = "ESP32_EVENT"
    const val DEFAULT_DEVICE_ID = "ESP32_GUARDIAN"
}
