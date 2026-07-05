const logger = require('./logger');
const { AppError } = require('./errors');

function errorHandler(err, req, res, _next) {
  if (err instanceof AppError) {
    logger.warn(`${req.method} ${req.path} — ${err.message}`, {
      code: err.code,
      statusCode: err.statusCode,
    });
    return res.status(err.statusCode).json(err.toJSON());
  }

  if (err.name === 'SyntaxError' && err.status === 400 && 'body' in err) {
    logger.warn('Invalid JSON in request body', { path: req.path });
    return res.status(400).json({
      success: false,
      error: 'Invalid JSON in request body',
      code: 'INVALID_JSON',
    });
  }

  if (err.code === 'VALIDATION_ERROR') {
    return res.status(400).json({
      success: false,
      error: err.message,
      code: 'VALIDATION_ERROR',
    });
  }

  logger.error(`Unhandled error on ${req.method} ${req.path}`, {
    message: err.message,
    stack: process.env.NODE_ENV !== 'production' ? err.stack : undefined,
  });

  res.status(500).json({
    success: false,
    error: 'Internal server error',
    code: 'INTERNAL_ERROR',
  });
}

function notFoundHandler(req, res) {
  res.status(404).json({
    success: false,
    error: `Route ${req.method} ${req.path} not found`,
    code: 'NOT_FOUND',
  });
}

module.exports = { errorHandler, notFoundHandler };
