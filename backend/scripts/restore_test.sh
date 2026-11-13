#!/usr/bin/env bash
# Restore drill: pull an encrypted offsite dump, decrypt it, load it into a throwaway Postgres and sanity-check it.
# Run on the VPS (needs docker, rclone, openssl). Usage: restore_test.sh [remote-file-name]  (default: newest on remote)
# No secrets here: passphrase comes from $PASS_FILE, the scratch DB uses trust auth and lives only for this run.
set -euo pipefail

PASS_FILE="${PASS_FILE:-/root/.mandi-offsite-pass}"
REMOTE="${REMOTE:-gdrive:MandiSamiti-backups}"
IMAGE="${IMAGE:-postgres:16-alpine}"
NAME="${1:-$(rclone lsf "$REMOTE" | sort | tail -1)}"
[ -n "$NAME" ] && [ -s "$PASS_FILE" ] || { echo "restore-test: no remote dump or no passphrase file" >&2; exit 1; }

umask 077
WORK=$(mktemp -d)
CT="mandi_restore_test_$$"
cleanup() { docker rm -f "$CT" >/dev/null 2>&1 || true; rm -rf "$WORK"; }
trap cleanup EXIT

rclone copyto "$REMOTE/$NAME" "$WORK/dump.sql.gz.enc"
openssl enc -d -aes-256-cbc -pbkdf2 -pass "file:$PASS_FILE" -in "$WORK/dump.sql.gz.enc" -out "$WORK/dump.sql.gz"
gzip -t "$WORK/dump.sql.gz"

docker run -d --name "$CT" -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=restore "$IMAGE" >/dev/null
for _ in $(seq 1 30); do
  docker exec "$CT" pg_isready -U postgres -d restore >/dev/null 2>&1 && break
  sleep 1
done
docker exec "$CT" pg_isready -U postgres -d restore >/dev/null || { echo "restore-test: scratch postgres did not start" >&2; exit 1; }

# The dump assigns ownership to the app role; recreate whichever roles it names.
gunzip -c "$WORK/dump.sql.gz" | grep -oE 'OWNER TO [A-Za-z0-9_]+' | awk '{print $3}' | sort -u | while read -r role; do
  [ "$role" = "postgres" ] || docker exec "$CT" psql -U postgres -d restore -qc "CREATE ROLE \"$role\" LOGIN" >/dev/null
done

gunzip -c "$WORK/dump.sql.gz" | docker exec -i "$CT" psql -U postgres -d restore -q -v ON_ERROR_STOP=1 >/dev/null

EXPECTED=$(gunzip -c "$WORK/dump.sql.gz" | grep -c '^CREATE TABLE' || true)
ACTUAL=$(docker exec "$CT" psql -U postgres -d restore -Atc "select count(*) from information_schema.tables where table_schema='public'")
ALEMBIC=$(docker exec "$CT" psql -U postgres -d restore -Atc "select count(*) from alembic_version")
ROWS=$(docker exec "$CT" psql -U postgres -d restore -Atc "select coalesce(sum(n_live_tup),0) from pg_stat_user_tables")

[ "$EXPECTED" -ge 1 ] && [ "$ACTUAL" -ge "$EXPECTED" ] || { echo "restore-test: FAIL tables expected $EXPECTED, restored $ACTUAL" >&2; exit 1; }
[ "$ALEMBIC" -eq 1 ] || { echo "restore-test: FAIL alembic_version rows = $ALEMBIC (want 1)" >&2; exit 1; }
echo "restore-test ok: $NAME ($ACTUAL tables, alembic row present, ~$ROWS rows)"
