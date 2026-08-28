# Firebase — Fases 4, 5 y 6

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

Desde la Fase 6, esa notificación llega **únicamente** al Apoderado
vinculado al Usuario que generó el evento (antes, Fase 5, era broadcast a
todos los Apoderados registrados). Ver § 7f para el modelo de vinculación.

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

## 7f. Fase 6 — vinculación Usuario↔Apoderado (reemplaza el broadcast)

Desde la Fase 6, el backend **ya no** notifica a todos los Apoderados
registrados: solo al que está vinculado con el Usuario que generó el
evento. Piezas nuevas:

- **`vinculaciones/{usuarioId}`** — un documento por Usuario vinculado, doc
  ID = el código de 6 caracteres que muestra `UsuarioBleScreen`. Campo
  `apoderadoId` (a qué Apoderado apunta) y `createdAt` (timestamp del
  servidor).
- **`apoderadoTokens/{token}`** gana un campo `apoderadoId` (el ID local
  persistido de ese Apoderado, ver `LocalIdentity.kt`) — antes solo tenía
  `fcmToken`/`updatedAt`.
- **`usuarioId`** (Usuario) y **`apoderadoId`** (Apoderado) son IDs
  anónimos generados una sola vez por instalación y persistidos con
  `SharedPreferences` (`LocalIdentity.kt`) — **no** hay Firebase
  Authentication todavía. Es una excepción puntual a "el rol no se
  persiste" de la Fase 2: el rol elegido sigue sin persistirse, pero esta
  identidad anónima sí, porque sin eso la vinculación no sobreviviría a
  cerrar sesión.
- **Cómo se vinculan**: el Usuario ve su código de 6 caracteres en pantalla
  (siempre visible, no depende de estar conectado por BLE) y se lo
  comparte al Apoderado. El Apoderado lo escribe en el campo "Código de tu
  Usuario" de su pantalla y toca "Vincular" (`VinculacionRepository.kt`).

  **El código de 6 caracteres NO es una medida de seguridad** — es
  solamente un mecanismo de emparejamiento sencillo para esta fase.
  Cualquiera que conozca el código de un Usuario podría vincularse a él.
  La mitigación real llega con **Firebase Authentication, todavía no
  implementada** (fase futura): ese día, `usuarioId`/`apoderadoId` dejan de
  generarse localmente y pasan a ser el `uid` real de cada cuenta — el
  esquema de Firestore de arriba no cambia, solo cambia de dónde sale el
  valor.

- **Tokens viejos de la Fase 5**: si ya habías registrado el rol Apoderado
  antes de esta fase, esos documentos en `apoderadoTokens` no tienen
  `apoderadoId` y quedan inservibles para el nuevo flujo (no rompen nada,
  simplemente ninguna consulta los va a encontrar). **Bórralos** en
  Firestore Console → `apoderadoTokens` antes de probar la Fase 6, para no
  confundirte con tokens huérfanos.
- **Reglas**: `firestore.rules` tiene el bloque nuevo `match /vinculaciones/{usuarioId}`
  y el de `apoderadoTokens` actualizado para exigir `apoderadoId`. Hay que
  **republicarlas** (paso 6) — el archivo también corrige unos backticks
  sobrantes que habían quedado mal pegados al final del archivo en una
  edición anterior y que impedían publicarlo.

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

## 9. Prueba física obligatoria (Fase 5/6) — dos teléfonos

Requiere el backend ya desplegado en Render (`backend/README.md`) y
`local.properties` configurado (sección 7e). Necesitas **dos** teléfonos.

**Teléfono 1 (Apoderado)**, antes de generar el evento:
1. Abre GuardianApp → rol **Apoderado**.
2. Concede el permiso de notificaciones si lo pide (solo Android 13+).
3. Debe mostrar `✓ Firebase conectado` / `Dispositivo registrado`. Si dice
   "Registrando dispositivo..." por mucho tiempo o "No se pudo registrar el
   dispositivo", revisa la conexión a Internet de ese teléfono antes de
   seguir — sin un token registrado no hay a quién notificar.
4. **(Fase 6, nuevo)** Anota el código de 6 caracteres que muestra el
   Teléfono 2 (ver abajo) en el campo "Código de tu Usuario" → "Vincular" →
   espera `✓ Vinculado correctamente`. Sin este paso, el backend responde
   `notified: 0` y no llega ninguna notificación — es el comportamiento
   correcto (ya no hay broadcast), no un error.

**Teléfono 2 (Usuario)**, con el ESP32 encendido: repite los pasos 1-7 de la
sección 8. Su código de 6 caracteres aparece arriba de todo, siempre
visible. Además de `Firebase: ✓ Evento enviado correctamente`, debe
aparecer una segunda sección:

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

## 10. Probar la vinculación (Fase 6) sin ESP32, con dos emuladores

Los emuladores Android no tienen hardware BLE (ver sección 8), así que la
parte de "generar el evento" se prueba con `curl` directo contra Render en
vez del ESP32 real — aísla exactamente la lógica de ruteo nueva.

1. **Borra la colección `apoderadoTokens`** en Firestore Console si tiene
   documentos de antes de la Fase 6 (ver § 7f — quedan sin `apoderadoId`,
   inservibles pero no rompen nada).
2. Emulador **Apoderado** (p. ej. `Pixel_10`) → rol Apoderado → conceder
   permiso de notificaciones → esperar `✓ Firebase conectado / Dispositivo
   registrado`.
3. Emulador **Usuario** (p. ej. `GuardianApp_Test`) → rol Usuario → anotar
   el código de 6 caracteres que aparece arriba de todo (no depende de BLE).
4. En el emulador Apoderado, escribir ese código en "Código de tu Usuario"
   → "Vincular" → esperar `✓ Vinculado correctamente`.
5. Verificar en Firebase Console: `vinculaciones/<código>` existe con el
   `apoderadoId` correcto; `apoderadoTokens` tiene ese `apoderadoId` en el
   token del emulador Apoderado.
6. Simular el evento directamente contra Render (reemplaza `<código>` y la
   API key):
   ```bash
   curl -X POST https://guardianapp-backend.onrender.com/api/events \
     -H "Content-Type: application/json" -H "X-API-Key: <tu key>" \
     -d '{"type":"ESP32_EVENT","message":"EVENTO_TEST","deviceId":"ESP32_GUARDIAN","usuarioId":"<código>"}'
   ```
   Esperado: `{"ok":true,"notified":1,...}` y la notificación llega al
   emulador Apoderado.
7. **Probar el aislamiento** (sin broadcast): repetir el mismo `curl` con un
   código inventado que no exista en `vinculaciones` (p. ej. `"usuarioId":"ZZZZZZ"`)
   → esperado `{"ok":true,"notified":0,"warning":"..."}`, **sin** que llegue
   ninguna notificación a ningún teléfono.
8. **Agregar un segundo par sin tocar código**: un tercer emulador/teléfono
   como Usuario 2 genera su propio código (automático, distinto al de
   Usuario 1); un cuarto como Apoderado 2 se vincula con ese código nuevo.
   Queda un segundo par `vinculaciones` totalmente independiente del
   primero — un evento con el `usuarioId` de Usuario 1 nunca notifica a
   Apoderado 2, y viceversa.
