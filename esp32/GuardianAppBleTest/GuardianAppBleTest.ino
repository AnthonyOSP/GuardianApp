/*
 * GuardianApp — Firmware de prueba BLE para la Fase 3.
 *
 * El ESP32 actúa como periférico BLE ("GATT server"): se anuncia con el
 * nombre "ESP32 Guardian", expone un Service con una Characteristic, y
 * envía notificaciones con el texto "EVENTO_TEST":
 *   - automáticamente cada 10 segundos mientras haya un teléfono conectado.
 *   - opcionalmente al presionar el botón BOOT de la placa (GPIO 0).
 *
 * Placa objetivo: cualquier ESP32 DevKit genérico (ESP32-WROOM-32).
 *   En Arduino IDE: Herramientas > Placa > "ESP32 Dev Module".
 *
 * Librerías necesarias: ninguna externa. Las clases BLEDevice / BLEServer /
 * BLEUtils / BLE2902 forman parte de "ESP32 BLE Arduino", incluida junto
 * con el paquete de placas ESP32 (no hay que instalarla aparte).
 *
 * Cómo cargar (Arduino IDE):
 *   1. Archivo > Preferencias > "URLs Adicionales de Gestor de Tarjetas":
 *      https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
 *   2. Herramientas > Placa > Gestor de tarjetas... instalar "esp32" (Espressif Systems).
 *   3. Herramientas > Placa > "ESP32 Dev Module".
 *   4. Herramientas > Puerto > selecciona el puerto USB del ESP32.
 *   5. Sketch > Subir (o el botón de flecha ▶).
 *
 * Cómo comprobar que se está anunciando:
 *   - Abre el Monitor Serie (115200 baudios): debe imprimir
 *     "BLE listo. Anunciándose como 'ESP32 Guardian'...".
 *   - O usa una app como "nRF Connect" en otro teléfono: debe verse
 *     "ESP32 Guardian" en la lista de dispositivos BLE cercanos, con el
 *     Service UUID indicado abajo.
 *
 * IMPORTANTE: estos UUIDs deben coincidir exactamente con
 * app/src/main/java/com/example/guardianapp/ble/BleConstants.kt
 */

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// Debe coincidir con BleConstants.GUARDIAN_SERVICE_UUID
#define SERVICE_UUID        "a07498ca-ad5b-474e-940d-16f1fbe7e8cd"
// Debe coincidir con BleConstants.GUARDIAN_EVENT_CHARACTERISTIC_UUID
#define CHARACTERISTIC_UUID "51ff12bb-3ed8-46e5-b4f9-d64e2fec021b"

#define DEVICE_NAME "ESP32 Guardian"

// Botón BOOT integrado en la mayoría de las placas ESP32 DevKit (activo en LOW).
#define BOTON_EVENTO_PIN 0

const unsigned long INTERVALO_AUTOMATICO_MS = 10000; // 10 segundos

BLEServer* pServer = nullptr;
BLECharacteristic* pCharacteristic = nullptr;
bool deviceConnected = false;
unsigned long ultimoEnvio = 0;
bool botonAnteriorPresionado = false;

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    deviceConnected = true;
    Serial.println("Teléfono conectado.");
  }

  void onDisconnect(BLEServer* server) override {
    deviceConnected = false;
    Serial.println("Teléfono desconectado. Reanudando anuncio BLE...");
    // Sin esto, el ESP32 deja de anunciarse tras la primera desconexión.
    server->getAdvertising()->start();
  }
};

void enviarEvento(const char* mensaje) {
  if (!deviceConnected) return;
  pCharacteristic->setValue(mensaje);
  pCharacteristic->notify();
  Serial.print("Evento enviado: ");
  Serial.println(mensaje);
}

void setup() {
  Serial.begin(115200);

  pinMode(BOTON_EVENTO_PIN, INPUT_PULLUP);

  BLEDevice::init(DEVICE_NAME);

  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());

  BLEService* pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
  );
  // Descriptor CCCD: permite que el cliente (Android) se suscriba a notificaciones.
  pCharacteristic->addDescriptor(new BLE2902());
  pCharacteristic->setValue("SIN_EVENTO");

  pService->start();

  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  BLEDevice::startAdvertising();

  Serial.println("BLE listo. Anunciándose como 'ESP32 Guardian'...");
  Serial.print("Service UUID: ");
  Serial.println(SERVICE_UUID);
}

void loop() {
  unsigned long ahora = millis();

  // Envío automático cada 10 segundos mientras haya conexión.
  if (deviceConnected && (ahora - ultimoEnvio >= INTERVALO_AUTOMATICO_MS)) {
    ultimoEnvio = ahora;
    enviarEvento("EVENTO_TEST");
  }

  // Envío manual con el botón BOOT (opcional, útil para la demo en vivo).
  bool botonPresionado = (digitalRead(BOTON_EVENTO_PIN) == LOW);
  if (botonPresionado && !botonAnteriorPresionado) {
    enviarEvento("EVENTO_TEST");
    ultimoEnvio = ahora; // evita que el envío automático se dispare justo después
  }
  botonAnteriorPresionado = botonPresionado;

  delay(20); // pequeño respiro para el debounce del botón
}
