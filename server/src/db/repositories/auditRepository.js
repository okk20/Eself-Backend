const { query } = require('../pool');

const auditRepository = {
  async create(entry) {
    const { paymentReference, userId, action, status, retryCount, ipAddress, userAgent } = entry;
    const text = `
      INSERT INTO payment_audit (payment_reference, user_id, action, status, retry_count, ip_address, user_agent)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING *
    `;
    const result = await query(text, [
      paymentReference || null,
      userId || null,
      action,
      status || null,
      retryCount || 0,
      ipAddress || null,
      userAgent || null,
    ]);
    return result.rows[0];
  },

  async createInTx(client, entry) {
    const { paymentReference, userId, action, status, retryCount, ipAddress, userAgent } = entry;
    const text = `
      INSERT INTO payment_audit (payment_reference, user_id, action, status, retry_count, ip_address, user_agent)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING *
    `;
    const result = await client.query(text, [
      paymentReference || null,
      userId || null,
      action,
      status || null,
      retryCount || 0,
      ipAddress || null,
      userAgent || null,
    ]);
    return result.rows[0];
  },

  async count() {
    const text = 'SELECT COUNT(*)::int AS count FROM payment_audit';
    const result = await query(text);
    return result.rows[0].count;
  },
};

module.exports = auditRepository;
