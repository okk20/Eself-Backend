require('dotenv').config();

const { config, validateEnv } = require('./config');
const db = require('./database');
const logger = require('./logger');

validateEnv();

async function start() {
  try {
    await db.init();
  } catch (err) {
    logger.error('Failed to initialize database', { error: err.message });
    process.exit(1);
  }

  const app = require('./app');

  function gracefulShutdown(signal) {
    logger.info(`${signal} received — starting graceful shutdown...`);
    server.close(async () => {
      logger.info('HTTP server closed');
      try {
        await db.close();
        logger.info('Database connections closed');
      } catch (err) {
        logger.warn('Error closing database', { error: err.message });
      }
      logger.info('Shutdown complete');
      process.exit(0);
    });

    setTimeout(() => {
      logger.error('Forced shutdown after timeout');
      process.exit(1);
    }, 10000);
  }

  const server = app.listen(config.port, () => {
    logger.info('========================================');
    logger.info(`Eself Backend Engine v3.0.0`);
    logger.info(`Port: ${config.port}`);
    logger.info(`Environment: ${config.nodeEnv}`);
    logger.info(`Database: PostgreSQL`);
    logger.info(`Paystack: ${config.paystackSecretKey.startsWith('sk_live_') ? 'LIVE' : 'TEST'} mode`);
    logger.info(`Gemini AI: ${config.geminiApiKey ? 'configured' : 'not configured'}`);
    logger.info(`CORS: ${config.isProduction ? config.corsOrigins.join(', ') : 'all origins'}`);
    logger.info('========================================');
  });

  process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
  process.on('SIGINT', () => gracefulShutdown('SIGINT'));

  process.on('uncaughtException', (err) => {
    logger.error('Uncaught exception', { message: err.message, stack: err.stack });
    process.exit(1);
  });

  process.on('unhandledRejection', (reason) => {
    logger.error('Unhandled rejection', { reason: reason?.message || reason });
  });
}

start();
