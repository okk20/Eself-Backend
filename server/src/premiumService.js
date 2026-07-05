const db = require('./database');
const paystackService = require('./paystack');
const logger = require('./logger');
const { ValidationError, DuplicateError, PaystackError } = require('./errors');

const EXPECTED_AMOUNT_GHS = 20;
const EXPECTED_CURRENCY = 'GHS';

async function verifyAndActivate(paymentReference, userId) {
  logger.audit('PAYMENT_VERIFICATION_STARTED', { paymentReference, userId });

  const existing = await db.getPurchaseByReference(paymentReference);
  if (existing) {
    logger.audit('PAYMENT_ALREADY_VERIFIED', { paymentReference, userId, existingId: existing.id });
    logger.info('Payment reference already processed — returning existing premium', {
      paymentReference,
      userId,
    });

    return {
      success: true,
      premium: true,
      message: 'Payment previously verified successfully!',
    };
  }

  let txData;
  try {
    txData = await paystackService.verifyTransaction(paymentReference);
  } catch (err) {
    logger.audit('PAYSTACK_VERIFICATION_FAILED', {
      paymentReference,
      userId,
      error: err.message,
    });
    throw err;
  }

  if (txData.status !== 'success') {
    logger.audit('PAYMENT_STATUS_NOT_SUCCESS', {
      paymentReference,
      userId,
      status: txData.status,
    });
    logger.warn('Paystack transaction not successful', {
      reference: paymentReference,
      status: txData.status,
    });
    return {
      success: false,
      premium: false,
      error: `Transaction verification failed: Payment status is '${txData.status}'`,
    };
  }

  if (txData.currency !== EXPECTED_CURRENCY) {
    logger.audit('INVALID_CURRENCY', {
      paymentReference,
      userId,
      expected: EXPECTED_CURRENCY,
      received: txData.currency,
    });
    return {
      success: false,
      premium: false,
      error: `Invalid currency: expected ${EXPECTED_CURRENCY}, received ${txData.currency}`,
    };
  }

  if (txData.amount < EXPECTED_AMOUNT_GHS) {
    logger.audit('INVALID_AMOUNT', {
      paymentReference,
      userId,
      expected: EXPECTED_AMOUNT_GHS,
      received: txData.amount,
    });
    return {
      success: false,
      premium: false,
      error: `Invalid amount: expected GH¢${EXPECTED_AMOUNT_GHS}, received GH¢${txData.amount}`,
    };
  }

  let purchase;
  try {
    purchase = await db.savePurchase({
      userId,
      paymentReference,
      amount: txData.amount,
      currency: txData.currency,
      paymentStatus: txData.status,
    });
  } catch (err) {
    logger.audit('DATABASE_SAVE_FAILED', {
      paymentReference,
      userId,
      error: err.message,
    });
    throw err;
  }

  if (purchase.existing) {
    logger.audit('DUPLICATE_GRANT_PREVENTED', { paymentReference, userId });
    return {
      success: true,
      premium: true,
      message: 'Payment reference previously processed. Premium already active.',
    };
  }

  logger.audit('PREMIUM_ACTIVATED', {
    userId,
    paymentReference,
    purchaseId: purchase.id,
    amount: txData.amount,
    currency: txData.currency,
  });

  logger.info('Premium activated successfully', {
    userId,
    paymentReference,
    purchaseId: purchase.id,
  });

  return {
    success: true,
    premium: true,
    message: 'Payment successfully verified and premium account activated!',
  };
}

async function getPremiumStatus(userId) {
  const isPremium = await db.getPremiumStatus(userId);
  logger.info('Premium status checked', { userId, isPremium });
  return {
    success: true,
    premium: isPremium,
  };
}

module.exports = { verifyAndActivate, getPremiumStatus };
