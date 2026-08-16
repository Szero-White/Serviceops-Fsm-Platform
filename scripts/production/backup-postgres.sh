#!/bin/sh
set -eu
umask 077

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"

: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"

SQL_FILE="$BACKUP_DIR/.serviceops_${TIMESTAMP}.sql.tmp"
ARCHIVE_FILE="$BACKUP_DIR/serviceops_${TIMESTAMP}.sql.gz"
cleanup() {
  rm -f "$SQL_FILE"
}
trap cleanup EXIT HUP INT TERM

docker compose -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner \
  > "$SQL_FILE"

gzip -c "$SQL_FILE" > "$ARCHIVE_FILE"
rm -f "$SQL_FILE"
trap - EXIT HUP INT TERM

find "$BACKUP_DIR" -type f -name 'serviceops_*.sql.gz' -mtime "+$RETENTION_DAYS" -delete
printf 'Backup created: %s\n' "$ARCHIVE_FILE"
