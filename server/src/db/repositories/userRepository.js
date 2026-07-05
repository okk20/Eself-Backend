const { query, transaction } = require('../pool');
const logger = require('../../logger');

const userRepository = {
  async findOrCreate(userId, email = '') {
    const text = `
      INSERT INTO users (id, email)
      VALUES ($1, $2)
      ON CONFLICT (id) DO UPDATE
        SET email = COALESCE(NULLIF($2, ''), users.email),
            updated_at = NOW()
      RETURNING *
    `;
    const result = await query(text, [userId, email]);
    return result.rows[0];
  },

  async getPremiumStatus(userId) {
    const text = 'SELECT is_premium FROM users WHERE id = $1';
    const result = await query(text, [userId]);
    if (result.rows.length === 0) return false;
    return result.rows[0].is_premium;
  },

  async getPremiumDetails(userId) {
    const text = `
      SELECT id, is_premium, premium_activated_at, created_at
      FROM users WHERE id = $1
    `;
    const result = await query(text, [userId]);
    if (result.rows.length === 0) return null;
    return result.rows[0];
  },

  async setPremium(userId) {
    const text = `
      UPDATE users
      SET is_premium = TRUE,
          premium_activated_at = COALESCE(premium_activated_at, NOW()),
          updated_at = NOW()
      WHERE id = $1
      RETURNING *
    `;
    const result = await query(text, [userId]);
    if (result.rows.length === 0) return null;
    return result.rows[0];
  },

  async setPremiumInTx(client, userId) {
    const text = `
      INSERT INTO users (id, email, is_premium, premium_activated_at)
      VALUES ($1, '', TRUE, NOW())
      ON CONFLICT (id) DO UPDATE
        SET is_premium = TRUE,
            premium_activated_at = COALESCE(users.premium_activated_at, NOW()),
            updated_at = NOW()
      RETURNING *
    `;
    const result = await client.query(text, [userId]);
    return result.rows[0];
  },
};

module.exports = userRepository;
