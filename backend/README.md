# GuardianApp — backend (Fases 5, 6 y 8)

Backend HTTP pequeño (Express) que:

1. Recibe `POST /api/events` desde el Android del Usuario cuando llega un evento del ESP32, identificado por su `usuarioId`.
2. Busca en Firestore a qué Apoderado está vinculado ese Usuario (`vinculaciones/{usuarioId}`, Fase 6 — ver `firebase/README.md` § 7f) y lee **solo** los tokens FCM de ese Apoderado (`apoderadoTokens` filtrado por `apoderadoId`). Ya no hace broadcast a todos los Apoderados registrados (esto era el comportamiento de la Fase 5).
3. Arma el título/cuerpo de la notificación según `type` (tabla `EVENT_TITLES` en `src/routes/events.js`, Fase 8 — debe coincidir con `SimulatedEvent.kt` del lado Android; ver `firebase/README.md` § 7d) y la envía con Firebase Admin SDK.
4. Borra los tokens que FCM reporte inválidos.

**No escucha Firestore de forma permanente.** Es solo request/response, a propósito: así funciona bien en el plan gratuito de Render, que duerme el servicio tras inactividad y lo despierta con la siguiente petición HTTP — un proceso que necesitara estar "siempre escuchando" no sobreviviría a eso.

No forma parte del build de Gradle (como `esp32/` y `firebase/`): es un proyecto Node.js independiente, en su propia carpeta.

## Seguridad — leer antes de desplegar

- **`EVENTS_API_KEY` es una protección TEMPORAL**, no autenticación real. Es un secreto compartido: Android lo manda en el header `X-API-Key` y el backend lo compara con un valor fijo (`src/middleware/apiKey.js`). Cualquiera que consiga esa clave —incluido alguien que decompile el APK, que en este proyecto no está ofuscado (`optimization.enable = false`)— podría llamar al endpoint. Es suficiente para evitar abuso casual mientras no hay cuentas de usuario; **se reemplazará por Firebase Authentication** (Android manda un ID Token, el backend lo verifica con `admin.auth().verifyIdToken(...)`) en una fase futura.
- **Las credenciales de Firebase Admin SDK (el JSON de la cuenta de servicio) nunca deben:**
  - viajar a Android,
  - subirse a este repositorio (`backend/.gitignore` ya excluye cualquier `serviceAccountKey.json` por si acaso),
  - ni pegarse en un chat/PR/issue.

  En Render se suben como **Secret File** (ver más abajo), no como variable de entorno de texto — así se evita tener que escapar los saltos de línea de la clave privada RSA dentro de un valor de una sola línea.

## Correr localmente

Necesitas Node.js 20+ y un archivo de credenciales de Firebase Admin SDK **solo para tu máquina** (no se sube a git):

1. Firebase Console → ⚙️ **Configuración del proyecto** → pestaña **Cuentas de servicio** → botón **Generar nueva clave privada**. Descarga el JSON y guárdalo en `backend/serviceAccountKey.json` (ese nombre exacto ya está en `.gitignore`).
2. Genera una API key de prueba: `openssl rand -hex 32`.
3. Copia `.env.example` a `.env` y completa `EVENTS_API_KEY` con la clave del paso 2 (deja `GOOGLE_APPLICATION_CREDENTIALS=./serviceAccountKey.json` como está).
4. Instala dependencias y arranca:

   ```bash
   cd backend
   npm install
   node --env-file=.env index.js
   ```

   (`--env-file` es una flag nativa de Node 20.6+; no hace falta el paquete `dotenv`.)
5. Probar:

   ```bash
   curl http://localhost:8080/health

   curl -X POST http://localhost:8080/api/events \
     -H "Content-Type: application/json" \
     -H "X-API-Key: <tu EVENTS_API_KEY>" \
     -d '{"type":"ESP32_EVENT","message":"EVENTO_TEST","deviceId":"ESP32_GUARDIAN","usuarioId":"<código de 6 caracteres>"}'
   ```

   Para que el segundo comando notifique a algún teléfono de verdad, tiene que existir (1) un documento en `apoderadoTokens` con `apoderadoId` (lo crea la app al abrir el rol Apoderado) y (2) un documento en `vinculaciones/<código>` que apunte a ese mismo `apoderadoId` (lo crea la app cuando el Apoderado escribe el código del Usuario y toca "Vincular"). Si `usuarioId` no está vinculado a nadie, la respuesta es `{"ok":true,"notified":0,"warning":"..."}` — no es un error, es el comportamiento correcto de la Fase 6 (sin broadcast).

## Desplegar en Render

1. Sube este repo a GitHub si todavía no lo está (Render se conecta a un repo Git).
2. En [render.com](https://render.com) → **New** → **Web Service** → conecta el repo `GuardianApp`.
3. Configuración del servicio:
   - **Root Directory**: `backend`
   - **Runtime**: Node
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
   - **Instance Type**: Free
4. **Environment** → variable de entorno:
   - `EVENTS_API_KEY` = la misma clave que vas a poner en `local.properties` del lado Android (ver `firebase/README.md` § Fase 5 Android). Generarla de nuevo con `openssl rand -hex 32` si quieres una distinta a la de desarrollo local.
5. **Secret Files** (no "Environment") → agrega un archivo:
   - Nombre/ruta: `/etc/secrets/serviceAccountKey.json`
   - Contenido: pega el JSON completo de la cuenta de servicio (el mismo del paso 1 de "Correr localmente", o genera uno nuevo).
6. De vuelta en **Environment**, agrega:
   - `GOOGLE_APPLICATION_CREDENTIALS` = `/etc/secrets/serviceAccountKey.json` (la ruta donde Render monta el Secret File del paso anterior).
7. **Create Web Service**. Cuando termine el deploy, Render te da una URL pública como `https://guardianapp-backend-xxxx.onrender.com`.
8. Probar contra esa URL con los mismos `curl` de arriba (cambiando `localhost:8080` por la URL de Render). La primera petición después de un rato de inactividad puede tardar 30-50s (el plan free "despierta" el servicio) — es esperado, no un error.
9. Copiar esa URL a `local.properties` del proyecto Android (ver `firebase/README.md`).

## Multi-usuario (Fase 6)

El backend ya soporta múltiples pares Usuario↔Apoderado independientes —
no hay nada que cambiar en código para agregar un par nuevo, es puramente
un problema de datos: cada Usuario tiene su propio `usuarioId` (código de 6
caracteres, generado y persistido en su teléfono) y cada Apoderado su
propio `apoderadoId` (UUID, generado y persistido en el suyo); la
vinculación entre ambos vive en `vinculaciones/{usuarioId}`. Un evento con
el `usuarioId` de un par nunca notifica a un Apoderado de otro par.

**Limitación conocida, documentada a propósito** (ver
`src/routes/events.js`, comentario `TODO(Fase futura — Firebase
Authentication)`): ni `usuarioId` ni `apoderadoId` están autenticados —
son IDs anónimos generados por el propio teléfono
(`LocalIdentity.kt`), y `EVENTS_API_KEY` es un secreto compartido, no una
verificación de identidad real. El código de 6 caracteres de vinculación
**no es una medida de seguridad**, es solo un mecanismo de emparejamiento
sencillo para esta fase. La resolución real llega con **Firebase
Authentication** (fase futura, no implementada todavía): ese día,
`usuarioId`/`apoderadoId` pasan a ser el `uid` verificado de cada cuenta
(vía ID Token validado en el backend), y `EVENTS_API_KEY` se reemplaza por
esa misma verificación — el esquema de Firestore no cambia.

**Migración desde la Fase 5**: los documentos de `apoderadoTokens` creados
antes de esta fase no tienen `apoderadoId` y quedan inservibles para las
consultas filtradas (no rompen nada, simplemente ninguna búsqueda los
encuentra). Bórralos en Firestore Console antes de probar la Fase 6 — ver
`firebase/README.md` § 7f.

## Variables de entorno — resumen

| Variable | Local (`.env`) | Render |
|---|---|---|
| `EVENTS_API_KEY` | sí | sí (Environment) |
| `GOOGLE_APPLICATION_CREDENTIALS` | `./serviceAccountKey.json` | `/etc/secrets/serviceAccountKey.json` (Secret File) |
| `PORT` | opcional (default 8080) | la inyecta Render sola |
