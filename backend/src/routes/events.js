const express = require('express');
const { getFirebaseAdmin } = require('../firebaseAdmin');

const router = express.Router();

const TOKENS_COLLECTION = 'apoderadoTokens';

// Códigos de error de FCM que significan "este token ya no sirve, bórralo".
// Ver https://firebase.google.com/docs/cloud-messaging/send-message#admin-error
const STALE_TOKEN_ERROR_CODES = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
]);

function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0;
}

router.post('/events', async (req, res) => {
  const body = req.body ?? {};
  const { type, message, deviceId } = body;

  // Validación del body: evento sin información válida -> 400, nunca 500.
  if (!isNonEmptyString(type) || !isNonEmptyString(message) || !isNonEmptyString(deviceId)) {
    return res.status(400).json({
      ok: false,
      error: 'Body inválido: se requieren "type", "message" y "deviceId" como strings no vacíos.',
    });
  }

  let admin;
  try {
    admin = getFirebaseAdmin();
  } catch (err) {
    console.error('Firebase Admin no se pudo inicializar:', err.message);
    return res.status(500).json({ ok: false, error: 'El backend no tiene Firebase configurado.' });
  }

  const firestore = admin.firestore();

  // ------------------------------------------------------------------
  // TODO(Fase futura — Firebase Authentication + relación Usuario↔Apoderado
  // real, ver README.md § "Camino a multi-usuario"):
  //
  //   deviceId  →  Usuario  →  Apoderado(s)  →  token(s) FCM
  //
  // Ese es el punto exacto donde debe resolverse la relación real, en vez
  // del broadcast de abajo. Cuando exista:
  //   1. Autenticar el request (ID Token de Firebase Authentication en vez
  //      de EVENTS_API_KEY) e identificar al Usuario dueño de la sesión, o
  //      resolverlo a partir de `deviceId` (colección `usuarios`, campo
  //      `deviceId` o `esp32Id`).
  //   2. Leer qué Apoderado(s) están vinculados a ese Usuario (p. ej.
  //      `usuarios/{usuarioId}.apoderadoIds`, o una colección `vinculos`).
  //   3. Leer el/los token(s) FCM SOLO de esos Apoderados (una subconsulta
  //      filtrada, no toda la colección `apoderadoTokens`).
  //
  // Hoy (Fase 5, un solo Usuario/Apoderado de prueba, sin cuentas todavía)
  // se notifica a TODOS los tokens registrados. `deviceId` ya viaja en el
  // evento precisamente para no tener que tocar este contrato después.
  // ------------------------------------------------------------------
  let tokensSnapshot;
  try {
    tokensSnapshot = await firestore.collection(TOKENS_COLLECTION).get();
  } catch (err) {
    console.error('Error leyendo apoderadoTokens de Firestore:', err.message);
    return res.status(502).json({ ok: false, error: 'No se pudo leer Firestore.' });
  }

  const tokens = tokensSnapshot.docs.map((doc) => doc.id);
  if (tokens.length === 0) {
    return res.status(200).json({
      ok: true,
      notified: 0,
      warning: 'No hay ningún Apoderado registrado todavía en apoderadoTokens.',
    });
  }

  // Mismo contrato de payload documentado en firebase/README.md § "contrato
  // del mensaje FCM": notification + data, para que Android muestre la
  // notificación aunque esté en segundo plano o cerrada.
  const multicastMessage = {
    tokens,
    notification: {
      title: 'Nuevo evento',
      body: `Se recibió un evento desde ${deviceId}`,
    },
    data: { type, message, deviceId },
  };

  // Nota de compatibilidad: el `firebase-admin` instalado (14.3.0, ver
  // package.json) marca este overload de sendEachForMulticast() (el que
  // recibe tokens de registro) como obsoleto a favor de uno nuevo basado en
  // "FIDs" (Firebase Installation IDs) — mismo cambio de fondo que
  // encontramos en el SDK de Android (ver FcmTokenRepository.kt). Se
  // mantiene deliberadamente la ruta de tokens clásica: es la que documenta
  // Firebase públicamente y la que coincide con el token que sube
  // FcmTokenRepository a Firestore.
  let response;
  try {
    response = await admin.messaging().sendEachForMulticast(multicastMessage);
  } catch (err) {
    console.error('Error enviando FCM:', err.message);
    return res.status(502).json({ ok: false, error: 'No se pudo enviar la notificación por FCM.' });
  }

  // Limpieza de tokens inválidos (Admin SDK ignora firestore.rules, puede
  // borrar directamente). No bloquea la respuesta si falla: es best-effort.
  const staleTokens = [];
  response.responses.forEach((result, index) => {
    if (!result.success && STALE_TOKEN_ERROR_CODES.has(result.error?.code)) {
      staleTokens.push(tokens[index]);
    }
  });
  if (staleTokens.length > 0) {
    const deletions = staleTokens.map((token) =>
      firestore.collection(TOKENS_COLLECTION).doc(token).delete()
    );
    const results = await Promise.allSettled(deletions);
    results.forEach((result, i) => {
      if (result.status === 'rejected') {
        console.error(`No se pudo borrar el token inválido ${staleTokens[i]}:`, result.reason);
      }
    });
  }

  return res.status(200).json({
    ok: true,
    notified: response.successCount,
    failed: response.failureCount,
    staleTokensRemoved: staleTokens.length,
  });
});

module.exports = router;
