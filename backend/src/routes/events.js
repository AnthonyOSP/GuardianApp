const express = require('express');
const { getFirebaseAdmin } = require('../firebaseAdmin');

const router = express.Router();

const TOKENS_COLLECTION = 'apoderadoTokens';
const VINCULACIONES_COLLECTION = 'vinculaciones';

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
  const { type, message, deviceId, usuarioId } = body;

  // Validación del body: evento sin información válida -> 400, nunca 500.
  // FASE 6: usuarioId ahora es obligatorio (identifica a quién notificar,
  // ver README.md § Fase 6 — reemplaza el broadcast de la Fase 5).
  if (
    !isNonEmptyString(type) ||
    !isNonEmptyString(message) ||
    !isNonEmptyString(deviceId) ||
    !isNonEmptyString(usuarioId)
  ) {
    return res.status(400).json({
      ok: false,
      error: 'Body inválido: se requieren "type", "message", "deviceId" y "usuarioId" como strings no vacíos.',
    });
  }
  // El código lo genera Android en mayúsculas (ver LocalIdentity.kt); se
  // normaliza igual acá por si alguien prueba el endpoint a mano con curl.
  const usuarioCode = usuarioId.trim().toUpperCase();

  let admin;
  try {
    admin = getFirebaseAdmin();
  } catch (err) {
    console.error('Firebase Admin no se pudo inicializar:', err.message);
    return res.status(500).json({ ok: false, error: 'El backend no tiene Firebase configurado.' });
  }

  const firestore = admin.firestore();

  // ------------------------------------------------------------------
  // FASE 6 — deviceId → Usuario → Apoderado → token(s) FCM.
  //
  // usuarioId ya viene identificado desde Android (LocalIdentity, código de
  // 6 caracteres persistido localmente — NO es una medida de seguridad,
  // solo un mecanismo de emparejamiento; ver firestore.rules). Se busca la
  // vinculación de ESE usuario, y solo se notifica al/a los token(s) del
  // apoderadoId vinculado. Ya no hay broadcast a toda la colección.
  //
  // TODO(Fase futura — Firebase Authentication): reemplazar `usuarioId`
  // (código local sin autenticar) por el `uid` verificado de un ID Token de
  // Firebase Authentication, y `EVENTS_API_KEY` por esa misma verificación
  // en vez de un secreto compartido fijo.
  // ------------------------------------------------------------------
  let vinculacionDoc;
  try {
    vinculacionDoc = await firestore.collection(VINCULACIONES_COLLECTION).doc(usuarioCode).get();
  } catch (err) {
    console.error('Error leyendo vinculaciones de Firestore:', err.message);
    return res.status(502).json({ ok: false, error: 'No se pudo leer Firestore.' });
  }

  if (!vinculacionDoc.exists) {
    // No es un error: es un estado válido y esperable (Usuario todavía sin
    // Apoderado vinculado). Mismo estilo que "0 tokens" de la Fase 5.
    return res.status(200).json({
      ok: true,
      notified: 0,
      warning: `El usuario "${usuarioCode}" no está vinculado a ningún Apoderado todavía.`,
    });
  }

  const apoderadoId = vinculacionDoc.data()?.apoderadoId;
  if (!isNonEmptyString(apoderadoId)) {
    console.error(`vinculaciones/${usuarioCode} existe pero no tiene apoderadoId válido.`);
    return res.status(200).json({
      ok: true,
      notified: 0,
      warning: `La vinculación de "${usuarioCode}" está incompleta.`,
    });
  }

  let tokensSnapshot;
  try {
    tokensSnapshot = await firestore
      .collection(TOKENS_COLLECTION)
      .where('apoderadoId', '==', apoderadoId)
      .get();
  } catch (err) {
    console.error('Error leyendo apoderadoTokens de Firestore:', err.message);
    return res.status(502).json({ ok: false, error: 'No se pudo leer Firestore.' });
  }

  const tokens = tokensSnapshot.docs.map((doc) => doc.id);
  if (tokens.length === 0) {
    return res.status(200).json({
      ok: true,
      notified: 0,
      warning: `El Apoderado vinculado a "${usuarioCode}" no tiene ningún token FCM registrado todavía.`,
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
