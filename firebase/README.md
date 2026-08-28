# Firebase — Fases 4 y 5

Este directorio no forma parte del build de Gradle (igual que `esp32/`): es
documentación y la fuente de verdad de las reglas de Firestore, que se
publican a mano en Firebase Console porque este repo no tiene configurado el
Firebase CLI/emulador.

GuardianApp usa Cloud Firestore para registrar los eventos que el rol Usuario
recibe del ESP32 por BLE (Fase 4). Todo el código que habla con Firestore
vive en `app/src/main/java/com/example/guardianapp/firebase/`
(`FirebaseRepository`, `EventUploadState`).

Desde la Fase 5, el rol Apoderado recibe una notificación push (FCM) cuando
eso pasa — ese código vive en `app/src/main/java/com/example/guardianapp/fcm/`
(quién la recibe) y en `backend/` (quién decide enviarla; ver
`backend/README.md`, es un proyecto Node.js separado desplegado en Render,
no forma parte del build de Gradle). Ver § 7d más abajo para el contrato
entre Android y el backend, y § 7e para configurar Android contra tu backend
desplegado.

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
| `deviceId` | string | `"ESP32_GUARDIAN"` (fijo por ahora, ver `EventConstants.DEFAULT_DEVICE_ID`) |
| `source` | string | `"esp32"` |
| `timestamp` | timestamp | generado por el servidor (`FieldValue.serverTimestamp()`) |

## 7b. Fase 5 — republicar las reglas

Las reglas de `firestore.rules` cambiaron en la Fase 5 (se agregó la
colección `apoderadoTokens`). Repite el paso 6: pega el contenido completo
actualizado de [`firestore.rules`](./firestore.rules) en Firestore Database →
Reglas → Publicar. No hace falta ningún otro paso manual en Firebase Console
para habilitar Cloud Messaging — está disponible automáticamente para
cualquier app registrada, a diferencia de Firestore que sí hubo que crear
explícitamente.

## 7c. Fase 5 — estructura de datos adicional

Colección `apoderadoTokens`, un documento por token FCM activo (el ID del
documento **es** el token):

| Campo | Tipo | Valor |
|---|---|---|
| `fcmToken` | string | igual al ID del documento |
| `updatedAt` | timestamp | generado por el servidor |

Ver el comentario en `FcmTokenRepository.kt` sobre por qué el ID del
documento es el propio token (asociación temporal sin cuentas todavía).

## 7d. Fase 5 — contrato del mensaje FCM (Android ↔ backend)

Quien envíe la notificación —el backend Node/Express en `backend/`, ver su
README— debe enviar un mensaje FCM con **ambos** payloads, `notification` y
`data`, con estas claves exactas — es lo que espera
`GuardianFirebaseMessagingService.kt` / `MainActivity.kt`:

```text
notification:
  title: "Nuevo evento"
  body:  "Se recibió un evento desde <deviceId>"

data:
  type:      <el campo "type" del documento de events>
  message:   <el campo "message" del documento de events>
  deviceId:  <el campo "deviceId" del documento de events>
```

Enviar ambos payloads (no solo `data`) es intencional: así Android muestra
la notificación automáticamente cuando la app está en segundo plano o
cerrada, sin código adicional (ver el comentario en
`GuardianFirebaseMessagingService.onMessageReceived`).

## 7e. Fase 5 — conectar Android con el backend de Render

El Usuario avisa al backend por HTTPS (`BackendEventRepository.kt`) además
de registrar el evento en Firestore (ambas cosas ocurren en paralelo, ver
`UsuarioBleScreen.kt`). La URL del backend y la API key **no están
hardcodeadas en el código fuente** — se leen de `local.properties` (raíz del
proyecto, ya gitignoreado) en tiempo de build (`app/build.gradle.kts` las
inyecta como `BuildConfig.BACKEND_BASE_URL` / `BuildConfig.BACKEND_API_KEY`).

Una vez desplegado el backend en Render (ver `backend/README.md`), agrega a
`local.properties`:

```properties
BACKEND_BASE_URL=https://<tu-servicio>.onrender.com
BACKEND_API_KEY=<la misma EVENTS_API_KEY que configuraste en Render>
```

Sin estas dos líneas, la app compila igual (por defecto son strings vacíos)
pero `UsuarioBleScreen` mostrará "Notificación al Apoderado: ✗ El backend no
está configurado" — es el comportamiento esperado hasta que despliegues el
backend, no un error de código.

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

## 9. Prueba física obligatoria (Fase 5) — dos teléfonos

Requiere el backend ya desplegado en Render (`backend/README.md`) y
`local.properties` configurado (sección 7e). Necesitas **dos** teléfonos.

**Teléfono 1 (Apoderado)**, antes de generar el evento:
1. Abre GuardianApp → rol **Apoderado**.
2. Concede el permiso de notificaciones si lo pide (solo Android 13+).
3. Debe mostrar `✓ Firebase conectado` / `Dispositivo registrado`. Si dice
   "Registrando dispositivo..." por mucho tiempo o "No se pudo registrar el
   dispositivo", revisa la conexión a Internet de ese teléfono antes de
   seguir — sin un token registrado no hay a quién notificar.

**Teléfono 2 (Usuario)**, con el ESP32 encendido: repite los pasos 1-7 de la
sección 8. Ahora, además de `Firebase: ✓ Evento enviado correctamente`,
debe aparecer una segunda sección:

- `Notificación al Apoderado:` `Enviando...` → `✓ Backend notificado correctamente`

Si el backend en Render llevaba un rato dormido, este paso puede tardar
30-50s — no es un error, es el "despertar" del plan free (ver
`backend/README.md`).

**De vuelta en el Teléfono 1 (Apoderado)**: debe aparecer la notificación
del sistema:

```text
GuardianApp
Nuevo evento
Se recibió un evento desde ESP32_GUARDIAN
```

Probar los tres estados de la app en el Teléfono 2 (Apoderado) como pide el
enunciado: con la app abierta (se actualiza sola, sin notificación del
sistema — ver `GuardianFirebaseMessagingService`), en segundo plano, y
completamente cerrada. En los tres casos debe llegar la notificación; al
tocarla, debe abrir GuardianApp y mostrar "Último evento" con Tipo/Mensaje/
Dispositivo.

Antes de esto, `./gradlew assembleDebug` debe compilar sin errores (con
`google-services.json` ya colocado en `app/`).
