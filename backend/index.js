const express = require('express');
const healthRouter = require('./src/routes/health');
const eventsRouter = require('./src/routes/events');
const { requireApiKey } = require('./src/middleware/apiKey');

const app = express();
app.use(express.json());

app.use('/', healthRouter);
app.use('/api', requireApiKey, eventsRouter);

app.use((_req, res) => {
  res.status(404).json({ ok: false, error: 'Ruta no encontrada.' });
});

// Manejador de errores central: nada de lo que pase en un handler debe
// tirar abajo el proceso (equivalente en el backend a "la app no debe
// cerrarse por estos errores" del lado Android). Express 5 reenvía acá
// tanto errores síncronos como promesas rechazadas de los handlers async.
// eslint-disable-next-line no-unused-vars
app.use((err, _req, res, _next) => {
  if (err?.type === 'entity.parse.failed') {
    // JSON mal formado en el body (express.json()).
    return res.status(400).json({ ok: false, error: 'Body JSON inválido.' });
  }
  console.error('Error no manejado:', err);
  res.status(500).json({ ok: false, error: 'Error interno del servidor.' });
});

const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`GuardianApp backend escuchando en el puerto ${port}`);
});
