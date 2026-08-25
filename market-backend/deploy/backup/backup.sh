#!/bin/sh
# Un solo dump: pg_dump -> gzip -> /backups, más limpieza de dumps viejos.
# Usado tanto por loop.sh (automático) como a mano:
#   docker compose exec backup /backup.sh
set -eu

mkdir -p /backups
ts=$(date +%Y%m%d_%H%M%S)
file="/backups/${PGDATABASE}_${ts}.sql.gz"
tmp="${file}.tmp"

echo "[$(date -Iseconds)] iniciando backup -> ${file}"

if pg_dump -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" | gzip > "$tmp"; then
    mv "$tmp" "$file"
    echo "[$(date -Iseconds)] backup ok ($(du -h "$file" | cut -f1))"
else
    rm -f "$tmp"
    echo "[$(date -Iseconds)] backup FALLÓ" >&2
    exit 1
fi

find /backups -name "${PGDATABASE}_*.sql.gz" -mtime "+${BACKUP_RETENTION_DAYS:-14}" -print -delete
