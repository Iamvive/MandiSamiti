#!/usr/bin/env bash
# Nightly staging backup. Run on the VPS from cron. Reads credentials from .env.staging only.
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/mandisamiti-staging}"
BACKUP_DIR="${BACKUP_DIR:-/root/mandi-backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"
DB_CONTAINER="${DB_CONTAINER:-mandisamiti_staging_db}"

umask 077
mkdir -p "$BACKUP_DIR"
OUT="$BACKUP_DIR/staging-$(date +%Y%m%d-%H%M%S).sql.gz"

# pg_dump runs inside the DB container; its own env supplies the user and database, no password in this script.
docker exec "$DB_CONTAINER" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' | gzip > "$OUT"

gzip -t "$OUT"
TABLES=$(gunzip -c "$OUT" | grep -c '^CREATE TABLE' || true)
if [ "$TABLES" -lt 1 ]; then
  echo "backup has no tables, removing $OUT" >&2
  rm -f "$OUT"
  exit 1
fi

find "$BACKUP_DIR" -name 'staging-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "backup ok: $OUT ($TABLES tables)"
