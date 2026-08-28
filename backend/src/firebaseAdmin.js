const { initializeApp, applicationDefault, getApps, getApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');

let app = null;

/**
 * Inicializa Firebase Admin SDK de forma perezosa (no al arrancar el
 * proceso, sino en el primer request que lo necesita) para que `/health`
 * responda igual aunque las credenciales todavía no estén configuradas —
 * el servidor nunca debe caerse por esto, solo esa ruta debe fallar con un
 * error claro (ver routes/events.js).
 *
 * Usa `applicationDefault()`, el mecanismo estándar de Google Cloud: lee el
 * archivo JSON de la cuenta de servicio desde la ruta indicada por la
 * variable de entorno GOOGLE_APPLICATION_CREDENTIALS. Es el MISMO código
 * para desarrollo local (la variable apunta a un archivo en tu disco, nunca
 * commiteado) y para Render (la variable apunta a un Secret File montado
 * por Render) — no hay que ramificar por entorno.
 *
 * Nota de compatibilidad (descubierta en producción, no documentada de
 * forma obvia): `firebase-admin` 14.3.0 (ver package.json) YA NO expone la
 * API "namespaced" clásica de versiones anteriores
 * (`admin.credential.applicationDefault()`, `admin.firestore()`,
 * `admin.messaging()` como métodos de un único objeto). El paquete
 * `require('firebase-admin')` solo exporta funciones sueltas
 * (`initializeApp`, `applicationDefault`, `getApps`, `getApp`, ...) — la API
 * real es modular, por subpath: `firebase-admin/app`, `firebase-admin/firestore`,
 * `firebase-admin/messaging` (cada uno con su propio `getX(app)`). Para no
 * tener que tocar routes/events.js (que llama a `admin.firestore()` /
 * `admin.messaging()`), esta función arma un objeto con esa misma forma por
 * dentro, usando las funciones modulares reales.
 *
 * IMPORTANTE: estas son credenciales administrativas. Nunca deben viajar a
 * Android ni vivir en este repositorio (ver backend/.gitignore y
 * backend/README.md).
 */
function getFirebaseAdmin() {
  if (!app) {
    if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      throw new Error(
        'GOOGLE_APPLICATION_CREDENTIALS no está configurada. Ver backend/README.md.'
      );
    }
    // Defensivo: si algo más ya inicializó la app por defecto en este
    // proceso, se reutiliza en vez de fallar con "app already exists".
    app = getApps().length > 0 ? getApp() : initializeApp({ credential: applicationDefault() });
  }
  return {
    firestore: () => getFirestore(app),
    messaging: () => getMessaging(app),
  };
}

module.exports = { getFirebaseAdmin };
