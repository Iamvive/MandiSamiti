#!/usr/bin/env bash
# Weekly off-VPS copy of the newest staging dump to Google Drive. Run on the VPS from cron.
# Encrypts with a passphrase file (mode 600, VPS only) before upload; Drive only ever sees ciphertext.
# One-time setup: rclone remote named "gdrive" (scope drive.file), passphrase file at $PASS_FILE.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/root/mandi-backups}"
PASS_FILE="${PASS_FILE:-/root/.mandi-offsite-pass}"
REMOTE="${REMOTE:-gdrive:MandiSamiti-backups}"
KEEP="${KEEP:-4}"

umask 077
LATEST=$(ls -1t "$BACKUP_DIR"/staging-2*.sql.gz | head -1)
[ -n "$LATEST" ] && [ -s "$PASS_FILE" ] || { echo "offsite: no dump or no passphrase file" >&2; exit 1; }

TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT
openssl enc -aes-256-cbc -pbkdf2 -salt -pass "file:$PASS_FILE" -in "$LATEST" -out "$TMP"
NAME="$(basename "$LATEST").enc"
rclone copyto "$TMP" "$REMOTE/$NAME"
rclone lsf "$REMOTE" | grep -c "$NAME" >/dev/null || { echo "offsite: upload not found on remote" >&2; exit 1; }

# keep newest $KEEP copies
rclone lsf "$REMOTE" | sort -r | tail -n +"$((KEEP + 1))" | while read -r f; do rclone deletefile "$REMOTE/$f"; done
echo "offsite ok: $NAME"
