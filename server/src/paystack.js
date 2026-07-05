const axios = require('axios');
const { config } = require('./config');
const { withRetry } = require('./retry');
const { PaystackError } = require('./errors');
const logger = require('./logger');

const apiClient = axios.create({
  baseURL: config.paystackApiBase,
  timeout: config.paystackTimeout,
  headers: {
    Authorization: `Bearer ${config.paystackSecretKey}`,
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      logger.error('Paystack API error', {
        status: error.response.status,
        message: error.response.data?.message || error.message,
      });
    }
    return Promise.reject(error);
  }
);

const paystackService = {
  async initializeTransaction(email, amountGhs, metadata = {}) {
    const amountInPesewas = Math.round(parseFloat(amountGhs || 20) * 100);

    const response = await withRetry(
      () =>
        apiClient.post('/transaction/initialize', {
          email,
          amount: amountInPesewas,
          currency: 'GHS',
          metadata,
        }),
      {
        context: `Initialize payment for ${email}`,
        maxAttempts: 2,
      }
    );

    if (!response.data || !response.data.status) {
      throw new PaystackError(
        response.data?.message || 'Failed to initialize transaction on Paystack.'
      );
    }

    logger.info('Paystack transaction initialized', {
      reference: response.data.data.reference,
      email,
    });

    return response.data.data;
  },

  async verifyTransaction(reference) {
    const response = await withRetry(
      () => apiClient.get(`/transaction/verify/${encodeURIComponent(reference)}`),
      {
        context: `Verify payment ${reference}`,
        maxAttempts: config.retryMaxAttempts,
        baseDelayMs: config.retryBaseDelayMs,
      }
    );

    if (!response.data || !response.data.status) {
      throw new PaystackError(
        response.data?.message || 'Failed to verify transaction on Paystack.'
      );
    }

    const txData = response.data.data;

    return {
      status: txData.status,
      reference: txData.reference,
      amount: txData.amount / 100,
      currency: txData.currency,
      paidAt: txData.paid_at,
      channel: txData.channel,
      gatewayResponse: txData.gateway_response,
      metadata: txData.metadata || {},
    };
  },
};

module.exports = paystackService;
