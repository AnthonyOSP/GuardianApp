package com.example.guardianapp.ble

import android.bluetooth.BluetoothDevice

/** Estado de conexión con el ESP32, tal como lo ve la UI. */
enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/** Un dispositivo BLE encontrado durante el escaneo. */
data class DiscoveredDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice
)

/** Un evento recibido desde el ESP32 vía notificación BLE. */
data class ReceivedEvent(
    val message: String,
    val receivedAt: String
)
