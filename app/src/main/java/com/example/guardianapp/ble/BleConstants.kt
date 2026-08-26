package com.example.guardianapp.ble

import java.util.UUID

/**
 * UUIDs del servicio BLE personalizado "GuardianApp" (FASE 3).
 *
 * IMPORTANTE: deben coincidir exactamente con los definidos en el firmware
 * de prueba del ESP32 (esp32/GuardianAppBleTest/GuardianAppBleTest.ino).
 *
 * Service UUID:        [GUARDIAN_SERVICE_UUID]
 * Characteristic UUID: [GUARDIAN_EVENT_CHARACTERISTIC_UUID]
 *   - Lectura (READ): sí, permite leer el último valor enviado.
 *   - Escritura (WRITE): no. En esta fase Android solo recibe eventos.
 *   - Notificaciones (NOTIFY): sí, es el mecanismo principal: el ESP32
 *     llama a notify() cuando ocurre un evento y Android lo recibe en
 *     BluetoothGattCallback.onCharacteristicChanged.
 */
object BleConstants {
    val GUARDIAN_SERVICE_UUID: UUID = UUID.fromString("a07498ca-ad5b-474e-940d-16f1fbe7e8cd")
    val GUARDIAN_EVENT_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("51ff12bb-3ed8-46e5-b4f9-d64e2fec021b")

    /** Descriptor estándar de Bluetooth (CCCD) para activar notificaciones. */
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Nombre BLE anunciado por el firmware de prueba del ESP32. */
    const val EXPECTED_DEVICE_NAME = "ESP32 Guardian"

    /** Tiempo máximo de escaneo antes de detenerlo automáticamente. */
    const val SCAN_TIMEOUT_MS = 15_000L
}
