const express = require('express');

const router = express.Router();

// Sin autenticación a propósito: sirve para que Render confirme que el
// servicio está vivo, y para "despertarlo" manualmente (plan free duerme
// tras inactividad) antes de la prueba física, con un simple GET.
router.get('/health', (_req, res) => {
  res.status(200).json({ ok: true, service: 'guardianapp-backend' });
});

module.exports = router;
