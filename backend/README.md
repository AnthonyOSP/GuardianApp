# GuardianApp — backend (Fase 5)

Backend HTTP pequeño (Express) que:

1. Recibe `POST /api/events` desde el Android del Usuario cuando llega un evento del ESP32.
2. Busca los tokens FCM de Apoderado registrados en Firestore (`apoderadoTokens`, ver `firebase/README.md`).
3. Envía la notificación con Firebase Admin SDK.
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
     -d '{"type":"ESP32_EVENT","message":"EVENTO_TEST","deviceId":"ESP32_GUARDIAN"}'
   ```

   Para que el segundo comando notifique a algún teléfono de verdad, primero tiene que existir al menos un documento en `apoderadoTokens` (lo crea solo la app, al abrir el rol Apoderado).

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

## Camino a multi-usuario (todavía no implementado)

Hoy hay un solo Usuario/ESP32 de prueba y el backend notifica a **todos** los tokens en `apoderadoTokens` (broadcast temporal). El punto exacto donde se resolverá `deviceId → Usuario → Apoderado → token` cuando exista esa relación real (y Firebase Authentication) está marcado con `TODO(Fase futura...)` en `src/routes/events.js`. `deviceId` ya viaja en cada evento precisamente para no tener que cambiar este contrato después.

## Variables de entorno — resumen

| Variable | Local (`.env`) | Render |
|---|---|---|
| `EVENTS_API_KEY` | sí | sí (Environment) |
| `GOOGLE_APPLICATION_CREDENTIALS` | `./serviceAccountKey.json` | `/etc/secrets/serviceAccountKey.json` (Secret File) |
| `PORT` | opcional (default 8080) | la inyecta Render sola |
