/**
 * One-time migration: imports existing JSON database records into PostgreSQL.
 *
 * Usage:
 *   node scripts/migrate-json-to-pg.js
 *
 * Set DATABASE_URL in .env or environment before running.
 * The JSON file at data/db.json will be read but NOT deleted.
 */

require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });

const fs = require('fs');
const path = require('path');
const { Pool } = require('pg');

const DB_FILE = path.join(__dirname, '..', 'data', 'db.json');

function loadJsonDb() {
  if (!fs.existsSync(DB_FILE)) {
    console.log('No JSON database found at', DB_FILE);
    return null;
  }
  const raw = fs.readFileSync(DB_FILE, 'utf8');
  const data = JSON.parse(raw);
  console.log(`Loaded JSON database: ${data.purchases?.length || 0} purchases, ${
    Object.keys(data.premiumUsers || {}).length
  } premium users, ${data.auditLog?.length || 0} audit entries`);
  return data;
}

async function migrate() {
  const jsonDb = loadJsonDb();
  if (!jsonDb) {
    console.log('Nothing to migrate.');
    process.exit(0);
  }

  const databaseUrl = process.env.DATABASE_URL;
  if (!databaseUrl) {
    console.error('DATABASE_URL environment variable is required.');
    process.exit(1);
  }

  const pool = new Pool({ connectionString: databaseUrl });
  const client = await pool.connect();

  try {
    // ─── Migrate purchases ─────────────────────────────────────
    let purchasesImported = 0;
    let purchasesSkipped = 0;

    for (const purchase of jsonDb.purchases || []) {
      const exists = await client.query(
        'SELECT 1 FROM purchases WHERE payment_reference = $1',
        [purchase.paymentReference]
      );

      if (exists.rows.length > 0) {
        purchasesSkipped++;
        continue;
      }

      await client.query(
        `INSERT INTO purchases (user_id, payment_reference, amount, currency, payment_status, provider, created_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7)
         ON CONFLICT (payment_reference) DO NOTHING`,
        [
          purchase.userId || 'unknown',
          purchase.paymentReference,
          purchase.amount || 0,
          purchase.currency || 'GHS',
          purchase.paymentStatus || 'success',
          'Paystack',
          purchase.purchaseDate || new Date().toISOString(),
        ]
      );
      purchasesImported++;
    }

    // ─── Migrate premium users ─────────────────────────────────
    let usersImported = 0;
    let usersSkipped = 0;

    for (const [userId, info] of Object.entries(jsonDb.premiumUsers || {})) {
      if (!info || !info.isPremium) continue;

      const exists = await client.query(
        'SELECT 1 FROM users WHERE id = $1',
        [userId]
      );

      if (exists.rows.length > 0) {
        usersSkipped++;
        continue;
      }

      await client.query(
        `INSERT INTO users (id, email, display_name, is_premium, premium_activated_at)
         VALUES ($1, '', '', TRUE, $2)
         ON CONFLICT (id) DO UPDATE
           SET is_premium = TRUE,
               premium_activated_at = COALESCE(users.premium_activated_at, $2)`,
        [userId, info.unlockedAt || new Date().toISOString()]
      );
      usersImported++;
    }

    // ─── Migrate audit log ─────────────────────────────────────
    let auditImported = 0;
    let auditSkipped = 0;

    for (const entry of jsonDb.auditLog || []) {
      if (!entry.action) continue;

      await client.query(
        `INSERT INTO payment_audit (payment_reference, user_id, action, status, created_at)
         VALUES ($1, $2, $3, $4, $5)`,
        [
          entry.details?.paymentReference || entry.paymentReference || null,
          entry.details?.userId || entry.userId || null,
          entry.action,
          entry.status || null,
          entry.timestamp || new Date().toISOString(),
        ]
      );
      auditImported++;
    }

    console.log('\n── Migration Summary ──');
    console.log(`Purchases imported: ${purchasesImported} (${purchasesSkipped} skipped as duplicates)`);
    console.log(`Users activated:   ${usersImported} (${usersSkipped} already exist)`);
    console.log(`Audit entries:     ${auditImported}`);
    console.log('─────────────────────');
    console.log('Migration complete. JSON data has been imported into PostgreSQL.');
    console.log('The data/db.json file has NOT been deleted — you may remove it manually after verification.');
  } catch (err) {
    console.error('Migration failed:', err.message);
    process.exit(1);
  } finally {
    client.release();
    await pool.end();
  }
}

migrate();
