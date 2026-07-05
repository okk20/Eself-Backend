CREATE TABLE IF NOT EXISTS payment_audit (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  payment_reference VARCHAR(255),
  user_id           VARCHAR(255),
  action            VARCHAR(100) NOT NULL,
  status            VARCHAR(50),
  retry_count       INTEGER NOT NULL DEFAULT 0,
  ip_address        VARCHAR(45),
  user_agent        TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_audit_payment_reference ON payment_audit(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payment_audit_user_id           ON payment_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_audit_action            ON payment_audit(action);
CREATE INDEX IF NOT EXISTS idx_payment_audit_created_at        ON payment_audit(created_at);
