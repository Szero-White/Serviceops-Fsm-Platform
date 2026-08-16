#!/bin/sh
set -eu
umask 077

if [ "$#" -ne 1 ]; then
  echo "Usage: RESTORE_CONFIRM=serviceops-restore $0 path/to/backup.sql.gz" >&2
  exit 2
fi

if [ "${RESTORE_CONFIRM:-}" != "serviceops-restore" ]; then
  echo "Refusing restore. Set RESTORE_CONFIRM=serviceops-restore after verifying the target database." >&2
  exit 2
fi

BACKUP_FILE="$1"
[ -f "$BACKUP_FILE" ] || { echo "Backup not found: $BACKUP_FILE" >&2; exit 2; }
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"

TMP_SQL="$(mktemp "${TMPDIR:-/tmp}/serviceops-restore.XXXXXX.sql")"
cleanup() {
  rm -f "$TMP_SQL"
}
trap cleanup EXIT HUP INT TERM

gzip -t "$BACKUP_FILE"
gzip -dc "$BACKUP_FILE" > "$TMP_SQL"

# Keep application traffic out of the database while destructive restore runs.
docker compose -f docker-compose.prod.yml stop backend >/dev/null 2>&1 || true
restart_backend() {
  docker compose -f docker-compose.prod.yml start backend >/dev/null 2>&1 || true
}
trap 'restart_backend; cleanup' EXIT HUP INT TERM

docker compose -f docker-compose.prod.yml exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  < "$TMP_SQL"

restart_backend
rm -f "$TMP_SQL"
trap - EXIT HUP INT TERM

echo "Restore completed. Run application smoke tests before reopening external traffic."
