#!/usr/bin/env bash
# =============================================================================
# Eself PostgreSQL Backup Script
# Creates compressed daily backups with automatic retention:
#   - Daily backups:   kept for 7 days
#   - Weekly backups:  kept for 4 weeks
#   - Monthly backups: kept for 12 months
# =============================================================================

set -euo pipefail

# ─── Configuration ──────────────────────────────────────────────────────────
# These can be overridden via environment variables
DB_NAME="${ESELF_DB_NAME:-eself}"
DB_USER="${ESELF_DB_USER:-postgres}"
DB_HOST="${ESELF_DB_HOST:-localhost}"
DB_PORT="${ESELF_DB_PORT:-5432}"
BACKUP_DIR="${ESELF_BACKUP_DIR:-./backups}"
DATE_FORMAT="%Y-%m-%d_%H-%M-%S"

# ─── Ensure backup directories exist ────────────────────────────────────────
mkdir -p "${BACKUP_DIR}/daily"
mkdir -p "${BACKUP_DIR}/weekly"
mkdir -p "${BACKUP_DIR}/monthly"

# ─── Create the backup ──────────────────────────────────────────────────────
TIMESTAMP=$(date +"${DATE_FORMAT}")
DAILY_FILE="${BACKUP_DIR}/daily/${DB_NAME}_${TIMESTAMP}.dump"
WEEKLY_FILE="${BACKUP_DIR}/weekly/${DB_NAME}_$(date +%Y_week%V).dump"
MONTHLY_FILE="${BACKUP_DIR}/monthly/${DB_NAME}_$(date +%Y_%m).dump"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting PostgreSQL backup of ${DB_NAME}..."

pg_dump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${DB_USER}" \
  --dbname="${DB_NAME}" \
  --format=custom \
  --compress=9 \
  --verbose \
  --file="${DAILY_FILE}" 2> "${DAILY_FILE}.log"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Daily backup created: ${DAILY_FILE}"

# ─── Promote to weekly if not already existing this week ────────────────────
if [ ! -f "${WEEKLY_FILE}" ]; then
  cp "${DAILY_FILE}" "${WEEKLY_FILE}"
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Weekly backup created: ${WEEKLY_FILE}"
fi

# ─── Promote to monthly if not already existing this month ──────────────────
if [ ! -f "${MONTHLY_FILE}" ]; then
  cp "${DAILY_FILE}" "${MONTHLY_FILE}"
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Monthly backup created: ${MONTHLY_FILE}"
fi

# ─── Retention: Delete daily backups older than 7 days ──────────────────────
find "${BACKUP_DIR}/daily" -name "*.dump" -type f -mtime +7 -delete
find "${BACKUP_DIR}/daily" -name "*.log" -type f -mtime +7 -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Daily retention applied (7 days)"

# ─── Retention: Delete weekly backups older than 4 weeks ────────────────────
find "${BACKUP_DIR}/weekly" -name "*.dump" -type f -mtime +28 -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Weekly retention applied (4 weeks)"

# ─── Retention: Delete monthly backups older than 12 months ─────────────────
find "${BACKUP_DIR}/monthly" -name "*.dump" -type f -mtime +365 -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Monthly retention applied (12 months)"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup complete."
