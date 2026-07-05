const logger = require('./logger');

const RETRYABLE_HTTP_STATUSES = [502, 503, 504];

const RETRYABLE_ERROR_CODES = [
  'ECONNRESET',
  'ETIMEDOUT',
  'ECONNREFUSED',
  'ENETUNREACH',
  'ENOTFOUND',
  'EAI_AGAIN',
];

function isRetryableError(error) {
  if (error.code && RETRYABLE_ERROR_CODES.includes(error.code)) {
    return true;
  }

  if (error.response && RETRYABLE_HTTP_STATUSES.includes(error.response.status)) {
    return true;
  }

  if (
    error.message &&
    (error.message.includes('timeout') || error.message.includes('socket hang up'))
  ) {
    return true;
  }

  return false;
}

async function withRetry(fn, options = {}) {
  const {
    maxAttempts = 3,
    baseDelayMs = 1000,
    shouldRetry = isRetryableError,
    context = 'operation',
  } = options;

  let lastError;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;

      if (attempt === maxAttempts || !shouldRetry(error)) {
        if (attempt > 1) {
          logger.warn(`${context}: All ${maxAttempts} attempts failed`, {
            message: error.message,
          });
        }
        throw error;
      }

      const delay = baseDelayMs * Math.pow(2, attempt - 1);
      const jitter = Math.random() * 200;
      const totalDelay = delay + jitter;

      logger.warn(`${context}: Attempt ${attempt}/${maxAttempts} failed, retrying in ${Math.round(totalDelay)}ms`, {
        message: error.message,
        attempt,
        nextDelayMs: Math.round(totalDelay),
      });

      await new Promise((resolve) => setTimeout(resolve, totalDelay));
    }
  }

  throw lastError;
}

module.exports = { withRetry, isRetryableError };
