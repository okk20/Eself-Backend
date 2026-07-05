const fs = require('fs');
const path = require('path');
const { query, transaction } = require('./pool');
const logger = require('../logger');

const MIGRATIONS_DIR = path.join(__dirname, '..', '..', 'migrations');

async function ensureMigrationsTable() {
  const sql = `
    CREATE TABLE IF NOT EXISTS _migrations (
      id          SERIAL PRIMARY KEY,
      filename    VARCHAR(255) UNIQUE NOT NULL,
      applied_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
    )
  `;
  await query(sql);
}

async function getAppliedMigrations() {
  const result = await query('SELECT filename FROM _migrations ORDER BY id');
  return new Set(result.rows.map((r) => r.filename));
}

async function runMigrations() {
  await ensureMigrationsTable();
  const applied = await getAppliedMigrations();

  const files = fs
    .readdirSync(MIGRATIONS_DIR)
    .filter((f) => f.endsWith('.sql'))
    .sort();

  let count = 0;

  for (const file of files) {
    if (applied.has(file)) {
      logger.debug(`Migration already applied: ${file}`);
      continue;
    }

    const filePath = path.join(MIGRATIONS_DIR, file);
    const sql = fs.readFileSync(filePath, 'utf8');

    try {
      await transaction(async (client) => {
        await client.query(sql);
        await client.query(
          'INSERT INTO _migrations (filename) VALUES ($1)',
          [file]
        );
      });
      logger.info(`Migration applied: ${file}`);
      count++;
    } catch (err) {
      logger.error(`Migration failed: ${file}`, { error: err.message });
      throw err;
    }
  }

  if (count === 0) {
    logger.info('All migrations already applied');
  }

  return count;
}

module.exports = { runMigrations };
