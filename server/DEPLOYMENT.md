# Production Deployment Guide

## Prerequisites

- Node.js >= 18
- A Paystack account with API keys
- A hosting provider (Render, Railway, Fly.io, or any Node.js host)

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `PORT` | No | Server port (default: 3000) |
| `NODE_ENV` | No | `development` or `production` |
| `PAYSTACK_SECRET_KEY` | **Yes** | Your Paystack secret key (`sk_test_...` or `sk_live_...`) |
| `PAYSTACK_PUBLIC_KEY` | No | Your Paystack public key (for reference only) |
| `JWT_SECRET` | **Yes** | Strong random string (min 32 chars). Generate with: `openssl rand -hex 32` |
| `DATABASE_URL` | No | PostgreSQL connection string (optional — uses local JSON file by default) |
| `CORS_ORIGINS` | No | Comma-separated allowed origins for CORS in production |
| `RATE_LIMIT_WINDOW_MS` | No | Rate limit window in ms (default: 900000 = 15 min) |
| `RATE_LIMIT_MAX` | No | Max requests per window (default: 100) |
| `PAYSTACK_TIMEOUT_MS` | No | Paystack API timeout (default: 15000) |
| `RETRY_MAX_ATTEMPTS` | No | Paystack verification retries (default: 3) |

---

## Quick Deploy

### Render

1. Push this repo to GitHub/GitLab.
2. In Render dashboard: **New → Web Service**.
3. Connect your repository.
4. Settings:
   - **Root Directory**: `server`
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
5. Add environment variables (see table above).
6. Deploy.

### Railway

1. Push to GitHub.
2. In Railway: **New Project → Deploy from GitHub**.
3. Set root directory to `server`.
4. Add environment variables in the Variables tab.
5. Deploy.

### Fly.io

```bash
fly launch --region lhr
fly secrets set PAYSTACK_SECRET_KEY=sk_live_...
fly secrets set JWT_SECRET=<generated>
fly deploy
```

---

## Verifying Deployment

```bash
curl https://your-app.com/health
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "...",
  "uptime": 123.45,
  "version": "2.0.0",
  "database": {
    "ok": true,
    "purchaseCount": 0,
    "premiumUserCount": 0,
    "auditLogCount": 0
  },
  "paystack": {
    "configured": true,
    "mode": "test"
  }
}
```

---

## Configuring Paystack Webhooks

1. Go to [Paystack Dashboard → Settings → API Keys & Webhooks](https://dashboard.paystack.com/#/settings/developer).
2. Set **Webhook URL** to: `https://your-app.com/payments/webhook`
3. Save.

Paystack will send `charge.success` events to this endpoint asynchronously.

---

## Database

By default, the server uses a local JSON file stored at `server/data/db.json`.

For production with multiple instances, switch to PostgreSQL:
1. Set `DATABASE_URL` environment variable.
2. The server will detect PostgreSQL and use it automatically (future feature).

The JSON database includes:
- **purchases**: Verified payment records with unique reference constraint
- **premiumUsers**: Active premium user records
- **auditLog**: All payment-related events for compliance and debugging

---

## Logging

Logs are written to stdout in structured format:
```
[2026-07-05T12:00:00.000Z] [INFO] Premium activated successfully { userId: "42", paymentReference: "ref_xxx", purchaseId: 1 }
```

Sensitive data (secret keys, tokens, card info) is automatically redacted.

In production, redirect stdout to your logging service:
```bash
npm start | tee -a /var/log/eself.log
```

---

## Monitoring

- **Health Check**: `GET /health` — database connectivity, Paystack mode, uptime
- **Audit Log**: All payment events are logged to the database and stdout
- **Error Tracking**: Unhandled errors log stack traces (development) or safe messages (production)
