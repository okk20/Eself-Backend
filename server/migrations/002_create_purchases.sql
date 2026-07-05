CREATE TABLE IF NOT EXISTS purchases (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id           VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  payment_reference VARCHAR(255) UNIQUE NOT NULL,
  amount            NUMERIC(10, 2) NOT NULL,
  currency          VARCHAR(3) NOT NULL DEFAULT 'GHS',
  payment_status    VARCHAR(50) NOT NULL,
  provider          VARCHAR(50) NOT NULL DEFAULT 'Paystack',
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_purchases_user_id            ON purchases(user_id);
CREATE INDEX IF NOT EXISTS idx_purchases_payment_reference   ON purchases(payment_reference);
CREATE INDEX IF NOT EXISTS idx_purchases_payment_status      ON purchases(payment_status);
CREATE INDEX IF NOT EXISTS idx_purchases_created_at          ON purchases(created_at);
