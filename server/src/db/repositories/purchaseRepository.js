const { query, transaction } = require('../pool');
const logger = require('../../logger');

const purchaseRepository = {
  async findByReference(reference) {
    const text = 'SELECT * FROM purchases WHERE payment_reference = $1';
    const result = await query(text, [reference]);
    return result.rows[0] || null;
  },

  async exists(reference) {
    const text = 'SELECT 1 FROM purchases WHERE payment_reference = $1';
    const result = await query(text, [reference]);
    return result.rows.length > 0;
  },

  async create(data) {
    const { userId, paymentReference, amount, currency, paymentStatus, provider } = data;
    const text = `
      INSERT INTO purchases (user_id, payment_reference, amount, currency, payment_status, provider)
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING *
    `;
    const result = await query(text, [
      userId,
      paymentReference,
      amount,
      currency || 'GHS',
      paymentStatus || 'success',
      provider || 'Paystack',
    ]);
    return result.rows[0];
  },

  async findByUserId(userId) {
    const text = 'SELECT * FROM purchases WHERE user_id = $1 ORDER BY created_at DESC';
    const result = await query(text, [userId]);
    return result.rows;
  },

  async createInTx(client, data) {
    const { userId, paymentReference, amount, currency, paymentStatus, provider } = data;
    const text = `
      INSERT INTO purchases (user_id, payment_reference, amount, currency, payment_status, provider)
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING *
    `;
    const result = await client.query(text, [
      userId,
      paymentReference,
      amount,
      currency || 'GHS',
      paymentStatus || 'success',
      provider || 'Paystack',
    ]);
    return result.rows[0];
  },

  async findByReferenceInTx(client, reference) {
    const text = 'SELECT * FROM purchases WHERE payment_reference = $1';
    const result = await client.query(text, [reference]);
    return result.rows[0] || null;
  },

  async count() {
    const text = 'SELECT COUNT(*)::int AS count FROM purchases';
    const result = await query(text);
    return result.rows[0].count;
  },
};

module.exports = purchaseRepository;
