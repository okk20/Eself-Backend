const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
const crypto = require('crypto');

const { config } = require('./config');
const { authenticateUser } = require('./auth');
const { errorHandler, notFoundHandler } = require('./errorHandler');
const premiumService = require('./premiumService');
const paystackService = require('./paystack');
const aiService = require('./aiService');
const db = require('./database');
const logger = require('./logger');
const { ValidationError, AuthenticationError } = require('./errors');

const app = express();

// ─── Security Middleware ───────────────────────────────────────
app.use(helmet());
app.use(compression());

const corsOptions = {
  origin: config.isProduction
    ? config.corsOrigins
    : '*',
  methods: ['GET', 'POST'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-user-id', 'x-paystack-signature'],
  credentials: true,
};
app.use(cors(corsOptions));

const limiter = rateLimit({
  windowMs: config.rateLimitWindowMs,
  max: config.rateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: 'Too many requests, please try again later.',
    code: 'RATE_LIMITED',
  },
});
app.use(limiter);

app.use(express.json({ limit: '1mb' }));

// ─── Request Logging ──────────────────────────────────────────
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    if (req.path !== '/health') {
      logger.info(`${req.method} ${req.path} ${res.statusCode} ${duration}ms`, {
        method: req.method,
        path: req.path,
        status: res.statusCode,
        durationMs: duration,
      });
    }
  });
  next();
});

// ─── Health Endpoint ──────────────────────────────────────────
app.get('/health', async (req, res) => {
  const dbHealth = await db.getHealth();
  const paystackConfigured = config.paystackSecretKey &&
    config.paystackSecretKey.startsWith('sk_') &&
    !config.paystackSecretKey.includes('placeholder');

  const status = dbHealth.ok && dbHealth.connected ? 'healthy' : 'degraded';
  const httpStatus = status === 'healthy' ? 200 : 503;

  res.status(httpStatus).json({
    status,
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    version: '3.0.0',
    database: {
      connected: dbHealth.connected || false,
      pool: dbHealth.pool || null,
      purchaseCount: dbHealth.purchaseCount || 0,
    },
    paystack: {
      configured: paystackConfigured,
      mode: paystackConfigured && config.paystackSecretKey.startsWith('sk_live_')
        ? 'live'
        : 'test',
    },
  });
});

// ─── GET / — Root info ────────────────────────────────────────
app.get('/', (req, res) => {
  res.json({
    name: 'Eself Paystack Secure Backend Engine',
    version: '3.0.0',
    status: 'active',
    database: 'PostgreSQL',
    health: '/health',
    endpoints: {
      initializePayment: 'POST /payments/initialize',
      verifyPayment: 'POST /payments/verify',
      premiumStatus: 'GET /premium/status',
      webhook: 'POST /payments/webhook',
      aiTheoryFeedback: 'POST /api/ai/theory-feedback',
      aiChat: 'POST /api/ai/chat',
    },
  });
});

// ─── POST /payments/initialize ────────────────────────────────
app.post('/payments/initialize', async (req, res, next) => {
  try {
    const { email, amount, studentId } = req.body;

    if (!email || !email.includes('@')) {
      throw new ValidationError('A valid email is required');
    }

    const targetStudentId = studentId || req.headers['x-user-id'] || 'anonymous';

    const data = await paystackService.initializeTransaction(email, amount, {
      student_id: targetStudentId.toString(),
      custom_fields: [
        {
          display_name: 'Student ID',
          variable_name: 'student_id',
          value: targetStudentId.toString(),
        },
      ],
    });

    logger.info('Payment initialized', {
      reference: data.reference,
      studentId: targetStudentId,
      email,
    });

    res.json({
      success: true,
      authorization_url: data.authorization_url,
      reference: data.reference,
      access_code: data.access_code,
    });
  } catch (err) {
    next(err);
  }
});

// ─── POST /payments/verify ────────────────────────────────────
app.post('/payments/verify', authenticateUser, async (req, res, next) => {
  try {
    const { paymentReference } = req.body;
    const userId = req.userId;

    if (!paymentReference || typeof paymentReference !== 'string' || paymentReference.trim().length === 0) {
      throw new ValidationError('paymentReference is required in body.');
    }

    const result = await premiumService.verifyAndActivate(paymentReference.trim(), userId);
    res.json(result);
  } catch (err) {
    next(err);
  }
});

// ─── GET /premium/status ──────────────────────────────────────
app.get('/premium/status', authenticateUser, async (req, res, next) => {
  try {
    const userId = req.userId;
    const result = await premiumService.getPremiumStatus(userId);
    res.json(result);
  } catch (err) {
    next(err);
  }
});

// ─── POST /payments/webhook ───────────────────────────────────
app.post('/payments/webhook', async (req, res) => {
  try {
    const signature = req.headers['x-paystack-signature'];

    if (!signature) {
      logger.warn('Webhook received without signature header');
      return res.status(401).json({ error: 'Missing signature header' });
    }

    const hash = crypto
      .createHmac('sha512', config.paystackSecretKey)
      .update(JSON.stringify(req.body))
      .digest('hex');

    if (hash !== signature) {
      logger.warn('Webhook signature verification failed — possible security alert');
      return res.status(401).json({ error: 'Signature verification failed' });
    }

    const event = req.body;
    logger.info(`Webhook received: ${event.event}`);

    if (event.event === 'charge.success') {
      const data = event.data;
      const reference = data.reference;
      const studentId = data.metadata?.student_id;

      if (studentId && reference) {
        logger.info('Processing webhook charge.success', { reference, studentId });

        const alreadyProcessed = await db.hasReferenceBeenProcessed(reference);
        if (!alreadyProcessed) {
          try {
            await db.savePurchase({
              userId: studentId.toString(),
              paymentReference: reference,
              amount: data.amount / 100,
              currency: data.currency,
              paymentStatus: data.status,
            });
          } catch (err) {
            logger.warn('Webhook database save issue', { error: err.message });
          }
        } else {
          logger.info('Webhook: reference already processed', { reference });
        }
      }
    }

    res.sendStatus(200);
  } catch (err) {
    logger.error('Webhook handler error', { error: err.message });
    res.sendStatus(500);
  }
});

// ─── POST /api/ai/theory-feedback ─────────────────────────────
app.post('/api/ai/theory-feedback', async (req, res, next) => {
  try {
    const { questionText, markingScheme, totalMarks, studentAnswer } = req.body;

    if (!questionText || !studentAnswer) {
      return res.status(400).json({ success: false, error: 'questionText and studentAnswer are required' });
    }

    const result = await aiService.getTheoryFeedback({
      questionText,
      markingScheme: markingScheme || 'Award marks based on accuracy',
      totalMarks: totalMarks || 10,
      studentAnswer,
    });

    res.json({ success: true, data: result });
  } catch (err) {
    logger.error('AI theory feedback error', { error: err.message });
    res.status(500).json({ success: false, error: 'AI grading service unavailable' });
  }
});

// ─── POST /api/ai/chat ────────────────────────────────────────
app.post('/api/ai/chat', async (req, res, next) => {
  try {
    const { userMessage, chatHistory } = req.body;

    if (!userMessage) {
      return res.status(400).json({ success: false, error: 'userMessage is required' });
    }

    const result = await aiService.getChatResponse({
      userMessage,
      chatHistory: chatHistory || [],
    });

    res.json({ success: true, data: result });
  } catch (err) {
    logger.error('AI chat error', { error: err.message });
    res.status(500).json({ success: false, error: 'AI chat service unavailable' });
  }
});

// ─── Error Handling ───────────────────────────────────────────
app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
