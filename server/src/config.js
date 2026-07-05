const logger = require('./logger');

const REQUIRED_VARS = ['PAYSTACK_SECRET_KEY', 'JWT_SECRET', 'DATABASE_URL', 'GEMINI_API_KEY'];

function validateEnv() {
  const missing = REQUIRED_VARS.filter(
    (key) => !process.env[key] || process.env[key].includes('placeholder') || process.env[key].includes('your_')
  );

  if (missing.length > 0) {
    logger.error(`Missing or placeholder environment variables: ${missing.join(', ')}`);
    logger.error('Create a .env file in the server/ directory with valid values.');
    logger.error('See .env.example for the required variables.');
    process.exit(1);
  }

  if (!process.env.DATABASE_URL.startsWith('postgres://') && !process.env.DATABASE_URL.startsWith('postgresql://')) {
    logger.error('DATABASE_URL must start with "postgres://" or "postgresql://".');
    logger.error('Example: DATABASE_URL=postgres://user:password@host:5432/database');
    process.exit(1);
  }

  if (process.env.PAYSTACK_SECRET_KEY && !process.env.PAYSTACK_SECRET_KEY.startsWith('sk_')) {
    logger.error('PAYSTACK_SECRET_KEY must start with "sk_" (test) or "sk_live_" (live).');
    process.exit(1);
  }

  if (process.env.JWT_SECRET && process.env.JWT_SECRET.length < 16) {
    logger.warn('JWT_SECRET is too short. Use at least 32 characters in production.');
  }

  const port = parseInt(process.env.PORT || '3000', 10);
  if (isNaN(port) || port < 1 || port > 65535) {
    logger.error(`Invalid PORT: ${process.env.PORT}. Must be between 1 and 65535.`);
    process.exit(1);
  }

  logger.info('Environment variables validated successfully');
  logger.info(`Node environment: ${process.env.NODE_ENV || 'development'}`);
  logger.info(`Server port: ${port}`);
  logger.info(`Database: ${process.env.DATABASE_URL.replace(/\/\/.*@/, '//***@')}`);
  logger.info(`Paystack mode: ${process.env.PAYSTACK_SECRET_KEY.startsWith('sk_live_') ? 'LIVE' : 'TEST'}`);
}

const config = {
  port: parseInt(process.env.PORT || '3000', 10),
  paystackSecretKey: process.env.PAYSTACK_SECRET_KEY,
  paystackPublicKey: process.env.PAYSTACK_PUBLIC_KEY || '',
  jwtSecret: process.env.JWT_SECRET,
  geminiApiKey: process.env.GEMINI_API_KEY,
  databaseUrl: process.env.DATABASE_URL,
  nodeEnv: process.env.NODE_ENV || 'development',
  isProduction: process.env.NODE_ENV === 'production',
  corsOrigins: process.env.CORS_ORIGINS
    ? process.env.CORS_ORIGINS.split(',').map((s) => s.trim())
    : ['http://localhost:3000'],
  rateLimitWindowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '900000', 10),
  rateLimitMax: parseInt(process.env.RATE_LIMIT_MAX || '100', 10),
  paystackApiBase: 'https://api.paystack.co',
  paystackTimeout: parseInt(process.env.PAYSTACK_TIMEOUT_MS || '15000', 10),
  retryMaxAttempts: parseInt(process.env.RETRY_MAX_ATTEMPTS || '3', 10),
  retryBaseDelayMs: parseInt(process.env.RETRY_BASE_DELAY_MS || '1000', 10),
  isDatabaseUrlValid: (url) => {
    return url && (url.startsWith('postgres://') || url.startsWith('postgresql://'));
  },
};

module.exports = { config, validateEnv };
