const admin = require('firebase-admin');

let app = null;

/**
 * Inicializa Firebase Admin SDK de forma perezosa (no al arrancar el
 * proceso, sino en el primer request que lo necesita) para que `/health`
 * responda igual aunque las credenciales todavía no estén configuradas —
 * el servidor nunca debe caerse por esto, solo esa ruta debe fallar con un
 * error claro (ver routes/events.js).
 *
 * Usa `admin.credential.applicationDefault()`, el mecanismo estándar de
 * Google Cloud: lee el archivo JSON de la cuenta de servicio desde la ruta
 * indicada por la variable de entorno GOOGLE_APPLICATION_CREDENTIALS. Es el
 * MISMO código para desarrollo local (la variable apunta a un archivo en tu
 * disco, nunca commiteado) y para Render (la variable apunta a un Secret
 * File montado por Render) — no hay que ramificar por entorno.
 *
 * IMPORTANTE: estas son credenciales administrativas. Nunca deben viajar a
 * Android ni vivir en este repositorio (ver backend/.gitignore y
 * backend/README.md).
 */
function getFirebaseAdmin() {
  if (app) return admin;
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    throw new Error(
      'GOOGLE_APPLICATION_CREDENTIALS no está configurada. Ver backend/README.md.'
    );
  }
  app = admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });
  return admin;
}

module.exports = { getFirebaseAdmin };
