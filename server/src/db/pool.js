const { Pool } = require('pg');
const logger = require('../logger');

let pool = null;

const QUERY_TIMEOUT_MS = 10000;

function createPool(databaseUrl) {
  pool = new Pool({
    connectionString: databaseUrl,
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 5000,
    query_timeout: QUERY_TIMEOUT_MS,
  });

  pool.on('connect', () => {
    logger.debug('PostgreSQL connection acquired from pool');
  });

  pool.on('acquire', () => {
    const total = pool.totalCount;
    const idle = pool.idleCount;
    const waiting = pool.waitingCount;
    if (total > 15) {
      logger.warn('PG pool nearing capacity', { total, idle, waiting });
    }
  });

  pool.on('remove', () => {
    logger.debug('PostgreSQL connection removed from pool');
  });

  pool.on('error', (err) => {
    logger.error('Unexpected PostgreSQL pool error', { error: err.message });
  });

  return pool;
}

async function getPool() {
  if (!pool) {
    throw new Error('Database pool not initialized. Call initPool() first.');
  }

  try {
    const client = await pool.connect();
    client.release();
  } catch (err) {
    logger.error('PG pool health check failed', { error: err.message });
    throw err;
  }

  return pool;
}

async function query(text, params) {
  if (!pool) {
    throw new Error('Database pool not initialized. Call initPool() first.');
  }

  const start = Date.now();
  try {
    const result = await pool.query(text, params);
    const duration = Date.now() - start;

    if (duration > 500) {
      logger.warn('Slow query detected', {
        durationMs: duration,
        text: text.substring(0, 200),
      });
    }

    return result;
  } catch (err) {
    const duration = Date.now() - start;

    if (err.message && err.message.includes('connect')) {
      logger.error('PG connection failure', { error: err.message, durationMs: duration });
    }

    logger.error('PG query failed', {
      error: err.message,
      text: text.substring(0, 200),
      durationMs: duration,
    });

    throw err;
  }
}

async function transaction(callback) {
  if (!pool) {
    throw new Error('Database pool not initialized. Call initPool() first.');
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (err) {
    try {
      await client.query('ROLLBACK');
    } catch (rollbackErr) {
      logger.error('PG transaction rollback failed', { error: rollbackErr.message });
    }
    logger.warn('PG transaction rolled back', { error: err.message });
    throw err;
  } finally {
    client.release();
  }
}

async function healthCheck() {
  if (!pool) {
    return { connected: false, error: 'Pool not initialized' };
  }

  try {
    const client = await pool.connect();
    await client.query('SELECT 1');
    client.release();
    return {
      connected: true,
      totalConnections: pool.totalCount,
      idleConnections: pool.idleCount,
      waitingClients: pool.waitingCount,
    };
  } catch (err) {
    return { connected: false, error: err.message };
  }
}

async function closePool() {
  if (pool) {
    await pool.end();
    pool = null;
    logger.info('PostgreSQL pool closed');
  }
}

async function waitForConnection(retries = 5, delay = 2000) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const client = await pool.connect();
      await client.query('SELECT 1');
      client.release();
      logger.info('PostgreSQL connection established');
      return true;
    } catch (err) {
      if (attempt < retries) {
        logger.warn(`Waiting for PostgreSQL (attempt ${attempt}/${retries})`, {
          error: err.message,
          nextRetryMs: delay,
        });
        await new Promise((r) => setTimeout(r, delay));
      } else {
        logger.error('Could not connect to PostgreSQL after all retries', {
          error: err.message,
        });
        throw err;
      }
    }
  }
}

module.exports = {
  createPool,
  getPool,
  query,
  transaction,
  healthCheck,
  closePool,
  waitForConnection,
};
