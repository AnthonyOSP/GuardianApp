package com.example.guardianapp.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Encapsula toda la lógica BLE (escaneo, conexión GATT, suscripción a
 * notificaciones) para la pantalla de Usuario. La UI solo lee el estado
 * expuesto (propiedades `by mutableStateOf`, observables por Compose) y
 * llama a [startScan], [connect] y [disconnect].
 *
 * No usa corrutinas ni Flow: los callbacks de BLE (que llegan en hilos de
 * Binder) escriben directamente sobre el estado de Compose, que es seguro
 * de mutar desde cualquier hilo.
 *
 * Nota de compatibilidad: este código usa deliberadamente la API BLE
 * "clásica" (characteristic.value, descriptor.value) en vez de los
 * overloads con ByteArray agregados en Android 13 (API 33), para tener un
 * solo camino de código válido en todo el rango minSdk 26 - targetSdk 37.
 * Genera warnings de @Deprecated (suprimidos), no errores.
 */
class BleManager(private val context: Context) {

    var bluetoothEnabled by mutableStateOf(isBluetoothCurrentlyEnabled())
        private set

    var isScanning by mutableStateOf(false)
        private set

    var connectionState by mutableStateOf(BleConnectionState.DISCONNECTED)
        private set

    var connectedDeviceName by mutableStateOf<String?>(null)
        private set

    var lastEvent by mutableStateOf<ReceivedEvent?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val _foundDevices = mutableStateListOf<DiscoveredDevice>()
    val foundDevices: List<DiscoveredDevice> get() = _foundDevices

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val stopScanRunnable = Runnable { stopScan() }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshBluetoothState()
        }
    }

    /** Debe llamarse cuando la pantalla de Usuario entra en composición. */
    fun register() {
        ContextCompat.registerReceiver(
            context,
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        refreshBluetoothState()
    }

    /** Debe llamarse cuando la pantalla de Usuario sale de composición. */
    fun unregister() {
        stopScan()
        disconnect()
        runCatching { context.unregisterReceiver(bluetoothStateReceiver) }
    }

    fun refreshBluetoothState() {
        bluetoothEnabled = isBluetoothCurrentlyEnabled()
    }

    private fun isBluetoothCurrentlyEnabled(): Boolean =
        context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning) return
        errorMessage = null

        if (!BlePermissions.hasRequiredPermissions(context)) {
            errorMessage = "No se concedieron los permisos de Bluetooth necesarios."
            return
        }
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            errorMessage = "Este dispositivo no tiene Bluetooth."
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            errorMessage = "Bluetooth está desactivado. Actívalo para buscar el ESP32."
            return
        }
        val leScanner = bluetoothAdapter.bluetoothLeScanner
        if (leScanner == null) {
            errorMessage = "No se pudo iniciar el escaneo BLE."
            return
        }

        _foundDevices.clear()
        scanner = leScanner
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.GUARDIAN_SERVICE_UUID))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        isScanning = true
        leScanner.startScan(filters, settings, scanCallback)
        mainHandler.postDelayed(stopScanRunnable, BleConstants.SCAN_TIMEOUT_MS)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        mainHandler.removeCallbacks(stopScanRunnable)
        if (isScanning) {
            runCatching { scanner?.stopScan(scanCallback) }
        }
        isScanning = false
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = result.scanRecord?.deviceName
                ?: device.name
                ?: BleConstants.EXPECTED_DEVICE_NAME
            if (_foundDevices.none { it.address == device.address }) {
                _foundDevices.add(DiscoveredDevice(name = name, address = device.address, device = device))
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            errorMessage = "No se pudo buscar dispositivos BLE (código $errorCode)."
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (connectionState != BleConnectionState.DISCONNECTED) return
        if (!BlePermissions.hasRequiredPermissions(context)) {
            errorMessage = "No se concedieron los permisos de Bluetooth necesarios."
            return
        }
        stopScan()
        errorMessage = null
        lastEvent = null
        connectionState = BleConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                errorMessage = if (connectionState == BleConnectionState.CONNECTING) {
                    "No se pudo conectar con el ESP32. Verifica que esté encendido y cerca del teléfono."
                } else {
                    "Se perdió la conexión con el ESP32."
                }
                cleanupAfterDisconnect(g)
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDeviceName = g.device.name ?: BleConstants.EXPECTED_DEVICE_NAME
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    cleanupAfterDisconnect(g)
                }
            }
        }

        @Suppress("DEPRECATION")
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                errorMessage = "Error al leer los servicios BLE del ESP32."
                g.disconnect()
                return
            }
            val characteristic = g
                .getService(BleConstants.GUARDIAN_SERVICE_UUID)
                ?.getCharacteristic(BleConstants.GUARDIAN_EVENT_CHARACTERISTIC_UUID)
            if (characteristic == null) {
                errorMessage = "El dispositivo conectado no es un ESP32 Guardian válido."
                g.disconnect()
                return
            }

            val subscribed = g.setCharacteristicNotification(characteristic, true)
            val cccd = characteristic.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (!subscribed || cccd == null) {
                errorMessage = "Error al suscribirse a notificaciones BLE."
                g.disconnect()
                return
            }
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(cccd)
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectionState = BleConnectionState.CONNECTED
                errorMessage = null
            } else {
                errorMessage = "Error al suscribirse a notificaciones BLE."
                g.disconnect()
            }
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid != BleConstants.GUARDIAN_EVENT_CHARACTERISTIC_UUID) return
            val message = characteristic.value?.toString(Charsets.UTF_8)?.trim().orEmpty()
            if (message.isEmpty()) return
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            lastEvent = ReceivedEvent(message = message, receivedAt = timestamp)
        }

        @SuppressLint("MissingPermission")
        private fun cleanupAfterDisconnect(g: BluetoothGatt) {
            connectionState = BleConnectionState.DISCONNECTED
            connectedDeviceName = null
            g.close()
            if (gatt === g) gatt = null
        }
    }
}
