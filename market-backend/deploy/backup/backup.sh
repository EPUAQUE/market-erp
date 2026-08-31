#!/bin/sh
# 1) Dump de Postgres a local (igual que siempre — camino rápido de
#    restauración sin depender de red). 2) Si hay GCS_BUCKET/passphrase
#    configurados: arma un bundle cifrado (dump + imágenes de producto +
#    certs + .env) y lo sube a almacenamiento externo con rclone, que ya
#    verifica el hash después de subir. Sin esos dos env vars, el paso 2 se
#    omite sin error — sigue funcionando como backup solo-local (ver
#    deploy/README.md, sección Backups, para la configuración completa).
# Usado tanto por loop.sh (automático) como a mano:
#   docker compose exec backup /backup.sh
set -eu

log() { echo "[$(date -Iseconds)] $*"; }
fail() {
    log "FALLÓ: $*" >&2
    /alert.sh "backup falló" "$*"
    exit 1
}

mkdir -p /backups
ts=$(date +%Y%m%d_%H%M%S)
dump_file="/backups/${PGDATABASE}_${ts}.sql.gz"
dump_tmp="${dump_file}.tmp"

log "iniciando dump -> ${dump_file}"
if pg_dump -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" | gzip > "$dump_tmp"; then
    mv "$dump_tmp" "$dump_file"
    log "dump ok ($(du -h "$dump_file" | cut -f1))"
else
    rm -f "$dump_tmp"
    fail "pg_dump/gzip"
fi

# Retención local del dump plano — sin cambios respecto al comportamiento previo.
find /backups -maxdepth 1 -name "${PGDATABASE}_*.sql.gz" -mtime "+${BACKUP_RETENTION_DAYS:-14}" -print -delete

# --- Bundle cifrado a almacenamiento externo (nuevo) ---
if [ -z "${GCS_BUCKET:-}" ] || [ -z "${BACKUP_ENCRYPTION_PASSPHRASE:-}" ]; then
    log "GCS_BUCKET/BACKUP_ENCRYPTION_PASSPHRASE no configurados — se omite la copia externa cifrada (solo queda el dump local en /backups)."
    exit 0
fi

work_dir=$(mktemp -d)
bundle_name="${PGDATABASE}_${ts}.bundle.tar.gz.gpg"
bundle_path="/tmp/${bundle_name}"
trap 'rm -rf "$work_dir" "$bundle_path" "${bundle_path}.sha256" 2>/dev/null' EXIT

cp "$dump_file" "$work_dir/dump.sql.gz"

if [ -d /productos-imagenes ] && [ -n "$(ls -A /productos-imagenes 2>/dev/null)" ]; then
    tar -czf "$work_dir/productos-imagenes.tar.gz" -C /productos-imagenes .
fi

if [ -d /certs ] && [ -n "$(ls -A /certs 2>/dev/null)" ]; then
    tar -czf "$work_dir/certs.tar.gz" -C /certs .
fi

if [ -f /env-backup/.env ]; then
    cp /env-backup/.env "$work_dir/.env"
fi

tar -C "$work_dir" -cz . | gpg --batch --yes --symmetric --cipher-algo AES256 \
    --passphrase "$BACKUP_ENCRYPTION_PASSPHRASE" -o "$bundle_path" \
    || fail "cifrado del bundle (tar+gpg)"

sha256sum "$bundle_path" | awk '{print $1}' > "${bundle_path}.sha256"

log "subiendo bundle a gcs:${GCS_BUCKET}/backups/"
rclone copyto "$bundle_path" "gcs:${GCS_BUCKET}/backups/${bundle_name}" \
    || fail "subida del bundle a GCS (rclone) — revisar que la VM tenga la cuenta de servicio adjunta y permiso de escritura en el bucket"
rclone copyto "${bundle_path}.sha256" "gcs:${GCS_BUCKET}/backups/${bundle_name}.sha256" \
    || fail "subida del checksum a GCS (rclone)"

log "bundle subido y verificado ok: ${bundle_name}"
