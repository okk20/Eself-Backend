#!/usr/bin/env bash
# =============================================================================
# Eself PostgreSQL Restore Script
#
# Usage:
#   ./scripts/restore.sh <backup-file>
#
# Example:
#   ./scripts/restore.sh backups/daily/eself_2026-07-05_12-00-00.dump
#
# CAUTION: This will DROP and recreate the target database.
# All existing data in the target database will be lost.
# =============================================================================

set -euo pipefail

# ─── Configuration ──────────────────────────────────────────────────────────
DB_NAME="${ESELF_DB_NAME:-eself}"
DB_USER="${ESELF_DB_USER:-postgres}"
DB_HOST="${ESELF_DB_HOST:-localhost}"
DB_PORT="${ESELF_DB_PORT:-5432}"

# ─── Validation ─────────────────────────────────────────────────────────────
if [ $# -lt 1 ]; then
  echo "ERROR: No backup file specified."
  echo ""
  echo "Usage: $0 <backup-file>"
  echo ""
  echo "Examples:"
  echo "  $0 backups/daily/eself_2026-07-05.dump"
  echo "  $0 backups/weekly/eself_2026_week27.dump"
  echo "  $0 backups/monthly/eself_2026_07.dump"
  exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "${BACKUP_FILE}" ]; then
  echo "ERROR: Backup file not found: ${BACKUP_FILE}"
  exit 1
fi

echo "╔══════════════════════════════════════════════════════════╗"
echo "║          ESELF DATABASE RESTORE                         ║"
echo "╠══════════════════════════════════════════════════════════╣"
echo "║ This will DROP and recreate the database:               ║"
echo "║   Database: ${DB_NAME}"
echo "║   Host:     ${DB_HOST}:${DB_PORT}"
echo "║   User:     ${DB_USER}"
echo "║   Backup:   ${BACKUP_FILE}"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
read -p "Are you sure you want to proceed? (yes/N): " CONFIRM

if [ "${CONFIRM}" != "yes" ]; then
  echo "Restore cancelled."
  exit 0
fi

# ─── Terminate existing connections ─────────────────────────────────────────
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Terminating existing connections..."
psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -c "
  SELECT pg_terminate_backend(pid)
  FROM pg_stat_activity
  WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();
" 2>/dev/null || echo "  (no active connections or database does not exist)"

# ─── Drop and recreate the database ─────────────────────────────────────────
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Dropping database ${DB_NAME}..."
dropdb --host="${DB_HOST}" --port="${DB_PORT}" --username="${DB_USER}" \
  --if-exists "${DB_NAME}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Creating database ${DB_NAME}..."
createdb --host="${DB_HOST}" --port="${DB_PORT}" --username="${DB_USER}" \
  "${DB_NAME}"

# ─── Restore from backup ────────────────────────────────────────────────────
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Restoring from ${BACKUP_FILE}..."
pg_restore \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${DB_USER}" \
  --dbname="${DB_NAME}" \
  --verbose \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  "${BACKUP_FILE}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Restore complete!"
echo ""
echo "Next steps:"
echo "  1. Verify data: pgcli -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME}"
echo "  2. Start the server: npm start"
