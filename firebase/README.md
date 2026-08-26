# Firebase — Fase 4

Este directorio no forma parte del build de Gradle (igual que `esp32/`): es
documentación y la fuente de verdad de las reglas de Firestore, que se
publican a mano en Firebase Console porque este repo no tiene configurado el
Firebase CLI/emulador.

GuardianApp usa Cloud Firestore para registrar los eventos que el rol Usuario
recibe del ESP32 por BLE. Todo el código que habla con Firebase vive en
`app/src/main/java/com/example/guardianapp/firebase/` (`FirebaseRepository`,
`EventUploadState`).

## 1. Crear el proyecto en Firebase Console

1. Entra a <https://console.firebase.google.com/> con la cuenta que vayas a
   usar para este proyecto.
2. "Agregar proyecto" (o "Add project").
3. Ponle un nombre (p. ej. `GuardianApp`). El Analytics de Google es
   opcional para esta fase; puedes desactivarlo.
4. Espera a que Firebase termine de aprovisionar el proyecto.

## 2. Registrar la app Android

1. Dentro del proyecto, pantalla principal → ícono de Android para
   "Agregar app".
2. **Nombre del paquete de Android** — debe ser exactamente:

   ```
   com.example.guardianapp
   ```

   (es el `applicationId`/`namespace` real del proyecto, en
   `app/build.gradle.kts`). Si no coincide exactamente, la app no podrá
   inicializar Firebase.
3. El apodo de la app y el SHA-1 son opcionales para esta fase (el SHA-1 solo
   hace falta para Google Sign-In / Dynamic Links, que no usamos todavía).
4. Pulsa "Registrar app".

## 3. Descargar y colocar `google-services.json`

1. Firebase Console te ofrece descargar `google-services.json` en ese mismo
   paso (también puedes volver a descargarlo luego desde
   **Configuración del proyecto → tus apps**).
2. Colócalo en:

   ```
   app/google-services.json
   ```

   (al mismo nivel que `app/build.gradle.kts`, **no** en la raíz del repo).
3. Este archivo está en `.gitignore` — no se sube a git. Cada persona que
   clone el repo debe descargar su propia copia desde Firebase Console.
4. Sin este archivo, `./gradlew assembleDebug` / `build` fallará al procesar
   los recursos de Google Services (`processDebugGoogleServices` o
   equivalente). Es el comportamiento esperado hasta que coloques el archivo.

## 4. Configuración de Gradle (ya aplicada en el código)

No necesitas tocar nada aquí — ya está hecho en esta fase:

- `gradle/libs.versions.toml`: plugin `com.google.gms.google-services`
  (4.5.0) y `firebase-bom` (34.18.0).
- `build.gradle.kts` (raíz): `alias(libs.plugins.google.services) apply false`.
- `app/build.gradle.kts`: aplica el plugin y agrega
  `implementation(platform(libs.firebase.bom))` +
  `implementation(libs.firebase.firestore)`.

## 5. Habilitar Cloud Firestore

1. En Firebase Console, menú lateral → **Compilación → Firestore Database**.
2. "Crear base de datos".
3. Elige una ubicación (region) — no se puede cambiar después. Cualquier
   región cercana a ti sirve para pruebas.
4. Modo de inicio: elige **modo de producción** (no "modo de prueba"), porque
   vamos a publicar reglas propias en el siguiente paso en vez de usar
   `allow read, write: if true;`.

## 6. Publicar las reglas de seguridad

Las reglas de esta fase están en [`firestore.rules`](./firestore.rules) de
este mismo directorio (es la fuente de verdad versionada en git). Para
aplicarlas:

1. Firestore Database → pestaña **Reglas** (Rules).
2. Borra el contenido del editor y pega el contenido completo de
   [`firestore.rules`](./firestore.rules).
3. "Publicar".

**Importante:** estas reglas son temporales porque todavía no hay Firebase
Authentication (fase futura). Bloquean toda lectura y solo aceptan crear
documentos con la forma exacta que usa `FirebaseRepository`. Cuando se
implemente Authentication, estas reglas deben reemplazarse por unas que
exijan `request.auth != null`. Ver el comentario al inicio de
`firestore.rules` para más detalle.

## 7. Estructura de datos

Colección `events`, un documento por evento:

| Campo | Tipo | Valor en esta fase |
|---|---|---|
| `type` | string | `"ESP32_EVENT"` |
| `message` | string | el mensaje recibido por BLE (p. ej. `"EVENTO_TEST"`) |
| `deviceId` | string | `"ESP32_GUARDIAN"` (fijo por ahora, ver `FirebaseRepository.DEFAULT_DEVICE_ID`) |
| `source` | string | `"esp32"` |
| `timestamp` | timestamp | generado por el servidor (`FieldValue.serverTimestamp()`) |

## 8. Prueba física obligatoria (Fase 4)

El BLE real necesita un **teléfono Android físico** — el emulador no tiene
hardware Bluetooth. El AVD existente no se toca ni se elimina; sigue
sirviendo para el resto del desarrollo.

1. **Enciende el ESP32** con el firmware de
   `esp32/GuardianAppBleTest/GuardianAppBleTest.ino` ya cargado (el mismo de
   la Fase 3, no cambió).
2. Instala la app en el teléfono físico:

   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew installDebug
   ```

   (o desde Android Studio, seleccionando el dispositivo físico como
   destino de ejecución).
3. Abre GuardianApp → rol **Usuario**.
4. Concede los permisos de Bluetooth si los pide, activa Bluetooth si está
   apagado.
5. **Buscar dispositivos** → conectar al `ESP32 Guardian`.
6. Haz que el ESP32 envíe `EVENTO_TEST` (según el firmware, normalmente al
   activar el disparador que llama a `notify()` — revisa `esp32/README.md`
   si no recuerdas cuál es exactamente).
7. En la pantalla del teléfono deberías ver, en este orden:
   - `Evento recibido:` `EVENTO_TEST`
   - `Firebase:` `Enviando...`
   - `Firebase:` `✓ Evento enviado correctamente`

   Si algo falla, debe mostrarse `Firebase: ✗ <mensaje>` (sin Internet,
   error de Firestore, timeout, etc.) — la app no debe cerrarse.
8. Abre [Firebase Console](https://console.firebase.google.com/) → el
   proyecto de este repo → **Firestore Database → Datos** → colección
   `events`. Debe haber un documento nuevo con los campos de la tabla de la
   sección 7 (`type: ESP32_EVENT`, `message: EVENTO_TEST`,
   `deviceId: ESP32_GUARDIAN`, `source: esp32`, `timestamp` con la hora del
   servidor).

Antes de esto, `./gradlew assembleDebug` debe compilar sin errores (con
`google-services.json` ya colocado en `app/`).
