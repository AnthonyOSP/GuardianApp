/**
 * Protección TEMPORAL del endpoint (ver backend/README.md y el comentario
 * en firebase/firestore.rules sobre reglas temporales — mismo espíritu):
 * compara el header "X-API-Key" contra un secreto compartido fijo
 * (EVENTS_API_KEY). Esto NO es autenticación real — cualquiera que
 * consiga la clave (incluido alguien que decompile el APK de Android,
 * donde no hay ofuscación: `optimization.enable = false`) puede llamar al
 * endpoint. Solo evita abuso casual mientras no existe una cuenta real por
 * Usuario.
 *
 * TODO(Fase futura — Firebase Authentication): reemplazar esto por
 * verificación de un ID Token de Firebase
 * (`admin.auth().verifyIdToken(token)`) enviado por Android, ya asociado a
 * un Usuario autenticado real.
 */
function requireApiKey(req, res, next) {
  const expected = process.env.EVENTS_API_KEY;
  if (!expected) {
    // Backend mal configurado: mejor rechazar todo que quedar abierto.
    console.error('EVENTS_API_KEY no está configurada en el servidor.');
    return res.status(500).json({ ok: false, error: 'El servidor no tiene configurada la API key.' });
  }

  const provided = req.get('X-API-Key');
  if (!provided || provided !== expected) {
    return res.status(401).json({ ok: false, error: 'API key inválida o ausente.' });
  }

  next();
}

module.exports = { requireApiKey };
