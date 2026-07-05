CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
  id            VARCHAR(255) PRIMARY KEY,
  email         VARCHAR(255) NOT NULL DEFAULT '',
  display_name  VARCHAR(255) NOT NULL DEFAULT '',
  is_premium    BOOLEAN NOT NULL DEFAULT FALSE,
  premium_activated_at TIMESTAMPTZ,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email      ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_is_premium  ON users(is_premium);
