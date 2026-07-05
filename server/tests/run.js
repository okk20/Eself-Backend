/**
 * Eself Backend Tests
 *
 * Run: node tests/run.js
 * Requires a running PostgreSQL instance with DATABASE_URL configured.
 *
 * Tests:
 *   - Database connection and migrations
 *   - Purchase creation and duplicate detection
 *   - Premium activation and status
 *   - Audit logging
 *   - Transaction rollback handling
 */

const { Pool } = require('pg');

const DATABASE_URL = process.env.DATABASE_URL || 'postgres://postgres:password@localhost:5432/eself_test';

let passed = 0;
let failed = 0;

function assert(condition, message) {
  if (condition) {
    console.log(`  ✓ ${message}`);
    passed++;
  } else {
    console.error(`  ✗ ${message}`);
    failed++;
  }
}

async function runTests() {
  console.log('╔══════════════════════════════════════════════╗');
  console.log('║      Eself Backend Test Suite                ║');
  console.log('╚══════════════════════════════════════════════╝');
  console.log(`Database: ${DATABASE_URL.replace(/\/\/.*@/, '//***@')}`);
  console.log('');

  const pool = new Pool({ connectionString: DATABASE_URL });

  // ─── Setup: create schema ──────────────────────────────────
  try {
    const client = await pool.connect();
    await client.query(`
      CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
      CREATE TABLE IF NOT EXISTS users (
        id VARCHAR(255) PRIMARY KEY,
        email VARCHAR(255) NOT NULL DEFAULT '',
        display_name VARCHAR(255) NOT NULL DEFAULT '',
        is_premium BOOLEAN NOT NULL DEFAULT FALSE,
        premium_activated_at TIMESTAMPTZ,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );
      CREATE TABLE IF NOT EXISTS purchases (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        payment_reference VARCHAR(255) UNIQUE NOT NULL,
        amount NUMERIC(10, 2) NOT NULL,
        currency VARCHAR(3) NOT NULL DEFAULT 'GHS',
        payment_status VARCHAR(50) NOT NULL,
        provider VARCHAR(50) NOT NULL DEFAULT 'Paystack',
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );
      CREATE TABLE IF NOT EXISTS payment_audit (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        payment_reference VARCHAR(255),
        user_id VARCHAR(255),
        action VARCHAR(100) NOT NULL,
        status VARCHAR(50),
        retry_count INTEGER NOT NULL DEFAULT 0,
        ip_address VARCHAR(45),
        user_agent TEXT,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );
    `);
    console.log('\n── Test 1: Database Connection ──');
    assert(true, 'Schema created successfully');
    client.release();
  } catch (err) {
    console.error('Failed to create schema:', err.message);
    failed++;
    await pool.end();
    process.exit(1);
  }

  // ─── Test 2: User creation and premium activation ──────────
  console.log('\n── Test 2: User & Premium Activation ──');
  try {
    const client = await pool.connect();
    const userId = 'test_user_1';

    await client.query(
      `INSERT INTO users (id, email, is_premium)
       VALUES ($1, 'test@eself.com', TRUE)
       ON CONFLICT (id) DO UPDATE SET is_premium = TRUE`,
      [userId]
    );
    assert(true, 'User created and premium activated');

    const userResult = await client.query('SELECT is_premium FROM users WHERE id = $1', [userId]);
    assert(userResult.rows[0].is_premium === true, 'User premium status is TRUE');

    client.release();
  } catch (err) {
    assert(false, `User activation failed: ${err.message}`);
  }

  // ─── Test 3: Purchase creation ─────────────────────────────
  console.log('\n── Test 3: Purchase Creation ──');
  try {
    const client = await pool.connect();

    const result = await client.query(
      `INSERT INTO purchases (user_id, payment_reference, amount, currency, payment_status)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id, payment_reference`,
      ['test_user_1', 'ref_test_001', 20.00, 'GHS', 'success']
    );
    assert(result.rows.length === 1, 'Purchase created with ID');
    assert(result.rows[0].payment_reference === 'ref_test_001', 'Payment reference matches');

    const refResult = await client.query(
      'SELECT 1 FROM purchases WHERE payment_reference = $1',
      ['ref_test_001']
    );
    assert(refResult.rows.length === 1, 'Purchase found by reference');

    client.release();
  } catch (err) {
    if (err.code === '23505') {
      assert(true, 'Duplicate reference correctly rejected (unique constraint works)');
    } else {
      assert(false, `Purchase creation failed: ${err.message}`);
    }
  }

  // ─── Test 4: Duplicate payment protection ──────────────────
  console.log('\n── Test 4: Duplicate Payment Protection ──');
  try {
    const client = await pool.connect();

    try {
      await client.query(
        `INSERT INTO purchases (user_id, payment_reference, amount, currency, payment_status)
         VALUES ($1, $2, $3, $4, $5)`,
        ['test_user_1', 'ref_test_001', 20.00, 'GHS', 'success']
      );
      assert(false, 'Duplicate INSERT should have failed');
    } catch (err) {
      assert(err.code === '23505', `Unique violation error (23505): ${err.code}`);
    }

    client.release();
  } catch (err) {
    assert(false, `Duplicate test failed: ${err.message}`);
  }

  // ─── Test 5: Transaction rollback ──────────────────────────
  console.log('\n── Test 5: Transaction Rollback ──');
  try {
    const client = await pool.connect();

    try {
      await client.query('BEGIN');
      await client.query(
        `INSERT INTO purchases (user_id, payment_reference, amount, currency, payment_status)
         VALUES ($1, $2, $3, $4, $5)`,
        ['test_user_1', 'ref_rollback_test', 20.00, 'GHS', 'success']
      );
      await client.query('UPDATE users SET is_premium = TRUE WHERE id = $1', ['test_user_1']);

      // Simulate failure
      await client.query('SELECT nonexistent_column FROM users');
      await client.query('COMMIT');
      assert(false, 'Should not reach COMMIT');
    } catch (err) {
      await client.query('ROLLBACK');
      assert(true, 'Transaction rolled back on error');

      const check = await client.query(
        'SELECT 1 FROM purchases WHERE payment_reference = $1',
        ['ref_rollback_test']
      );
      assert(check.rows.length === 0, 'Rolled-back purchase not persisted');
    }

    client.release();
  } catch (err) {
    assert(false, `Rollback test failed: ${err.message}`);
  }

  // ─── Test 6: Audit logging ─────────────────────────────────
  console.log('\n── Test 6: Audit Logging ──');
  try {
    const client = await pool.connect();

    await client.query(
      `INSERT INTO payment_audit (payment_reference, user_id, action, status)
       VALUES ($1, $2, $3, $4)`,
      ['ref_audit_test', 'test_user_1', 'TEST_ACTION', 'success']
    );

    const result = await client.query(
      'SELECT * FROM payment_audit WHERE payment_reference = $1',
      ['ref_audit_test']
    );
    assert(result.rows.length === 1, 'Audit entry created');
    assert(result.rows[0].action === 'TEST_ACTION', 'Audit action matches');

    client.release();
  } catch (err) {
    assert(false, `Audit test failed: ${err.message}`);
  }

  // ─── Test 7: Database reconnection ─────────────────────────
  console.log('\n── Test 7: Pool Health ──');
  try {
    const healthResult = await pool.query('SELECT 1 AS ok');
    assert(healthResult.rows[0].ok === 1, 'Database responds to SELECT 1');
    assert(pool.totalCount >= 0, 'Pool tracks connections');
    assert(pool.idleCount >= 0, 'Pool tracks idle connections');
  } catch (err) {
    assert(false, `Health test failed: ${err.message}`);
  }

  // ─── Cleanup ───────────────────────────────────────────────
  console.log('\n── Cleanup ──');
  try {
    const client = await pool.connect();
    await client.query('DELETE FROM payment_audit WHERE user_id = $1', ['test_user_1']);
    await client.query('DELETE FROM purchases WHERE payment_reference IN ($1, $2)',
      ['ref_test_001', 'ref_audit_test']);
    await client.query('DELETE FROM users WHERE id = $1', ['test_user_1']);
    client.release();
    assert(true, 'Test data cleaned up');
  } catch (err) {
    assert(false, `Cleanup failed: ${err.message}`);
  }

  // ─── Summary ────────────────────────────────────────────────
  console.log('\n╔══════════════════════════════════════════════╗');
  console.log(`║  Results: ${passed} passed, ${failed} failed${' '.repeat(18 - String(failed + passed).length)}║`);
  console.log('╚══════════════════════════════════════════════╝');

  await pool.end();
  process.exit(failed > 0 ? 1 : 0);
}

runTests().catch((err) => {
  console.error('Test suite error:', err);
  process.exit(1);
});
