#!/bin/sh
# Restaura un bundle cifrado desde GCS: descarga (el más reciente, o uno
# exacto pasado como argumento), verifica el checksum, descifra, destar, y
# restaura la base de datos directamente. Imágenes de producto también se
# restauran directas (bajo riesgo, son solo archivos). Certs y .env NO se
# sobreescriben solos — quedan guardados en /backups para revisión manual,
# son demasiado sensibles para aplicarlos sin que una persona los revise.
#
# Uso:
#   docker compose exec backup /restore.sh                  # el más reciente
#   docker compose exec backup /restore.sh <nombre-bundle>  # uno específico
#
# ADVERTENCIA: esto SOBREESCRIBE la base de datos "${PGDATABASE}" actual.
# Pensado para correr manual, nunca automático — ver deploy/README.md.
set -eu

: "${GCS_BUCKET:?GCS_BUCKET requerido}"
: "${BACKUP_ENCRYPTION_PASSPHRASE:?BACKUP_ENCRYPTION_PASSPHRASE requerido}"

log() { echo "[$(date -Iseconds)] $*"; }

bundle_name="${1:-}"
if [ -z "$bundle_name" ]; then
    log "buscando el bundle más reciente en gcs:${GCS_BUCKET}/backups/ ..."
    bundle_name=$(rclone lsf "gcs:${GCS_BUCKET}/backups/" --include "*.bundle.tar.gz.gpg" | sort | tail -n1)
    if [ -z "$bundle_name" ]; then
        echo "No se encontró ningún bundle en gcs:${GCS_BUCKET}/backups/" >&2
        exit 1
    fi
fi

log "restaurando desde: ${bundle_name}"

work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT

rclone copyto "gcs:${GCS_BUCKET}/backups/${bundle_name}" "${work_dir}/${bundle_name}"
rclone copyto "gcs:${GCS_BUCKET}/backups/${bundle_name}.sha256" "${work_dir}/${bundle_name}.sha256"

log "verificando checksum..."
expected=$(cat "${work_dir}/${bundle_name}.sha256")
actual=$(sha256sum "${work_dir}/${bundle_name}" | awk '{print $1}')
if [ "$expected" != "$actual" ]; then
    echo "Checksum no coincide — el bundle pudo corromperse en tránsito. Abortando." >&2
    exit 1
fi
log "checksum ok"

log "descifrando..."
gpg --batch --yes --decrypt --passphrase "$BACKUP_ENCRYPTION_PASSPHRASE" \
    -o "${work_dir}/bundle.tar.gz" "${work_dir}/${bundle_name}"

extract_dir="${work_dir}/extracted"
mkdir -p "$extract_dir"
tar -xzf "${work_dir}/bundle.tar.gz" -C "$extract_dir"

if [ -f "${extract_dir}/dump.sql.gz" ]; then
    log "restaurando base de datos ${PGDATABASE} (SOBREESCRIBE lo actual)..."
    gunzip -c "${extract_dir}/dump.sql.gz" | psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE"
    log "base de datos restaurada"
else
    echo "El bundle no trae dump.sql.gz — no se restauró nada de base de datos." >&2
fi

if [ -f "${extract_dir}/productos-imagenes.tar.gz" ] && [ -d /productos-imagenes ]; then
    log "restaurando imágenes de producto en /productos-imagenes ..."
    tar -xzf "${extract_dir}/productos-imagenes.tar.gz" -C /productos-imagenes
fi

marca=$(date +%Y%m%d_%H%M%S)

if [ -f "${extract_dir}/certs.tar.gz" ]; then
    destino="/backups/certs-restaurados-${marca}"
    mkdir -p "$destino"
    tar -xzf "${extract_dir}/certs.tar.gz" -C "$destino"
    log "certs del bundle guardados en ${destino} (dentro del volumen ./deploy/backups del host) — copialos a mano a deploy/certs/ si hace falta, no se sobreescriben solos."
fi

if [ -f "${extract_dir}/.env" ]; then
    cp "${extract_dir}/.env" "/backups/env-restaurado-${marca}"
    log ".env del bundle guardado en /backups/env-restaurado-${marca} para revisar a mano — el .env real del servidor no se sobreescribe solo."
fi

log "restauración completa."
