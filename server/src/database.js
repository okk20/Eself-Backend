const { createPool, query, transaction, healthCheck, closePool, waitForConnection } = require('./db/pool');
const { runMigrations } = require('./db/migrator');
const userRepository = require('./db/repositories/userRepository');
const purchaseRepository = require('./db/repositories/purchaseRepository');
const auditRepository = require('./db/repositories/auditRepository');
const logger = require('./logger');

const db = {
  async init() {
    const databaseUrl = process.env.DATABASE_URL;
    if (!databaseUrl) {
      throw new Error(
        'DATABASE_URL is required. Set it in .env or environment variables.\n' +
        'Example: DATABASE_URL=postgres://user:password@host:5432/database'
      );
    }

    createPool(databaseUrl);
    await waitForConnection();
    await runMigrations();

    logger.info('PostgreSQL database initialized');
  },

  async savePurchase(purchaseData) {
    const { userId, paymentReference, amount, currency, paymentStatus } = purchaseData;
    if (!userId || !paymentReference) {
      const err = new Error('userId and paymentReference are required');
      err.code = 'VALIDATION_ERROR';
      throw err;
    }

    const existing = await purchaseRepository.findByReference(paymentReference);
    if (existing) {
      logger.warn('Duplicate payment reference detected', { paymentReference, userId });
      return {
        id: existing.id,
        userId: existing.user_id,
        paymentReference: existing.payment_reference,
        amount: parseFloat(existing.amount),
        currency: existing.currency,
        paymentStatus: existing.payment_status,
        purchaseDate: existing.created_at,
        isPremium: true,
        existing: true,
      };
    }

    const ghsAmount = parseFloat(amount) || 0;

    let purchase;
    try {
      purchase = await transaction(async (client) => {
        const p = await purchaseRepository.createInTx(client, {
          userId,
          paymentReference,
          amount: ghsAmount,
          currency: currency || 'GHS',
          paymentStatus: paymentStatus || 'success',
          provider: 'Paystack',
        });

        await userRepository.setPremiumInTx(client, userId);

        return p;
      });
    } catch (err) {
      if (err.code === '23505') {
        const dup = await purchaseRepository.findByReference(paymentReference);
        if (dup) {
          logger.warn('Duplicate caught by unique constraint', { paymentReference });
          return {
            id: dup.id,
            userId: dup.user_id,
            paymentReference: dup.payment_reference,
            amount: parseFloat(dup.amount),
            currency: dup.currency,
            paymentStatus: dup.payment_status,
            purchaseDate: dup.created_at,
            isPremium: true,
            existing: true,
          };
        }
      }
      logger.error('Database save purchase failed', { error: err.message, paymentReference });
      throw err;
    }

    logger.info('Purchase saved and premium activated', {
      userId,
      paymentReference,
      purchaseId: purchase.id,
    });

    return {
      id: purchase.id,
      userId: purchase.user_id,
      paymentReference: purchase.payment_reference,
      amount: parseFloat(purchase.amount),
      currency: purchase.currency,
      paymentStatus: purchase.payment_status,
      purchaseDate: purchase.created_at,
      isPremium: true,
      existing: false,
    };
  },

  async getPremiumStatus(userId) {
    if (!userId) return false;
    return userRepository.getPremiumStatus(userId);
  },

  async getPremiumDetails(userId) {
    if (!userId) return null;
    return userRepository.getPremiumDetails(userId);
  },

  async hasReferenceBeenProcessed(reference) {
    if (!reference) return false;
    return purchaseRepository.exists(reference);
  },

  async getPurchaseByReference(reference) {
    if (!reference) return null;
    const p = await purchaseRepository.findByReference(reference);
    if (!p) return null;
    return {
      id: p.id,
      userId: p.user_id,
      paymentReference: p.payment_reference,
      amount: parseFloat(p.amount),
      currency: p.currency,
      paymentStatus: p.payment_status,
      purchaseDate: p.created_at,
      isPremium: true,
    };
  },

  async getAllPurchasesByUser(userId) {
    if (!userId) return [];
    const rows = await purchaseRepository.findByUserId(userId);
    return rows.map((p) => ({
      id: p.id,
      userId: p.user_id,
      paymentReference: p.payment_reference,
      amount: parseFloat(p.amount),
      currency: p.currency,
      paymentStatus: p.payment_status,
      purchaseDate: p.created_at,
    }));
  },

  async appendAuditLog(entry) {
    await auditRepository.create(entry);
  },

  async getHealth() {
    const pg = await healthCheck();
    if (!pg.connected) {
      return { ok: false, error: pg.error };
    }
    try {
      const purchaseCount = await purchaseRepository.count();
      return {
        ok: true,
        connected: true,
        purchaseCount,
        pool: {
          total: pg.totalConnections,
          idle: pg.idleConnections,
          waiting: pg.waitingClients,
        },
      };
    } catch (err) {
      return { ok: true, connected: true, error: err.message };
    }
  },

  async close() {
    await closePool();
  },
};

module.exports = db;
