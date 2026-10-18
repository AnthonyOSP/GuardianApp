# ESP32 — Firmware de prueba BLE (Fase 3)

Este directorio no forma parte del build de Android (Gradle no lo toca). Es el
firmware de prueba que corre en el ESP32 para poder probar físicamente el
flujo `ESP32 → BLE → GuardianApp` de la Fase 3.

## Hardware

Cualquier placa **ESP32 DevKit genérico** (ESP32-WROOM-32 o similar) sirve.
No se usa ningún sensor real todavía; el botón "BOOT" integrado en la placa
(GPIO 0) se reutiliza opcionalmente para disparar un evento manualmente.

## Cargar el sketch con Arduino IDE

1. **Archivo > Preferencias > "URLs Adicionales de Gestor de Tarjetas"**, agrega:
   `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
2. **Herramientas > Placa > Gestor de tarjetas...** busca `esp32` (de Espressif Systems) e instálalo.
3. **Herramientas > Placa** → selecciona **"ESP32 Dev Module"**.
4. **Herramientas > Puerto** → selecciona el puerto USB del ESP32.
5. Abre `GuardianAppBleTest/GuardianAppBleTest.ino` y súbelo (▶).

No hace falta instalar ninguna librería aparte: `BLEDevice.h`, `BLEServer.h`,
`BLEUtils.h` y `BLE2902.h` vienen incluidas con el paquete de placas `esp32`.

(También puede compilarse con PlatformIO usando `framework = arduino` y
`board = esp32dev` en `platformio.ini`, sin cambios en el código.)

## Comprobar que se está anunciando

- Abre el **Monitor Serie** a **115200 baudios**: debe imprimir
  `BLE listo. Anunciándose como 'ESP32 Guardian'...`.
- O usa una app como **nRF Connect** (Android/iOS) desde otro teléfono: debe
  aparecer un dispositivo llamado **"ESP32 Guardian"** anunciando el Service
  UUID de abajo.

## UUIDs del servicio BLE

Deben coincidir exactamente con
`app/src/main/java/com/example/guardianapp/ble/BleConstants.kt` en el
proyecto Android.

| Elemento | UUID | Propiedades |
|---|---|---|
| Service | `a07498ca-ad5b-474e-940d-16f1fbe7e8cd` | — |
| Characteristic | `51ff12bb-3ed8-46e5-b4f9-d64e2fec021b` | `READ`, `NOTIFY` (no `WRITE`) |
| Descriptor CCCD | `00002902-0000-1000-8000-00805f9b34fb` (estándar BLE) | habilita las notificaciones |

## Comportamiento

- Al arrancar, crea el servicio/characteristic y empieza a anunciarse como
  `ESP32 Guardian`.
- Cuando GuardianApp se conecta y se suscribe a notificaciones, el ESP32
  envía el texto `EVENTO_TEST`:
  - automáticamente cada 10 segundos mientras el teléfono siga conectado.
  - o al presionar el botón **BOOT** de la placa (envío manual, útil para
    demostraciones en vivo sin esperar los 10 segundos).
- Si el teléfono se desconecta, el ESP32 vuelve a anunciarse automáticamente
  para permitir una nueva conexión.

## Qué texto mandar (códigos de evento)

GuardianApp reconoce estos 4 textos en la characteristic (sin distinguir
mayúsculas). Con cualquiera de ellos, el Apoderado recibe la misma
notificación que con el botón correspondiente de la app:

| Texto enviado por BLE | Notificación al Apoderado |
|---|---|
| `EMERGENCY` | 🚨 Emergencia — "El Usuario ha enviado una alerta de emergencia." |
| `FOOD` | 🍽️ Comida — "El Usuario necesita comida." |
| `BATHROOM` | 🚻 Baño — "El Usuario necesita ayuda para ir al baño." |
| `HELP` | 🆘 Ayuda — "El Usuario necesita ayuda." |

Cualquier otro texto (por ejemplo `EVENTO_TEST`, el de este sketch de
prueba) llega como notificación genérica: **Nuevo evento** — `Evento
"EVENTO_TEST" desde ESP32_GUARDIAN`.

Ejemplo en el sketch: `enviarEvento("EMERGENCY");` en vez de
`enviarEvento("EVENTO_TEST");`. Máximo 20 bytes por mensaje (Android no
negocia un MTU mayor); los 4 códigos caben de sobra.
