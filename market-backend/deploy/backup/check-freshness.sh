#!/bin/sh
# Alerta si el dump local más reciente es más viejo que BACKUP_MAX_AGE_HOURS
# — cubre "el loop sigue vivo pero viene fallando hace rato" sin esperar a
# que alguien mire logs a mano. Corrido al inicio de cada vuelta de
# loop.sh, antes de intentar el próximo backup.
set -eu

max_age_hours="${BACKUP_MAX_AGE_HOURS:-30}"

latest=$(ls -t /backups/"${PGDATABASE}"_*.sql.gz 2>/dev/null | head -n1)

if [ -z "$latest" ]; then
    /alert.sh "sin backups todavía" "No se encontró ningún ${PGDATABASE}_*.sql.gz en /backups. Si el servicio recién arrancó esto es esperado; si no, revisar por qué nunca corrió un backup exitoso."
    exit 0
fi

mtime=$(stat -c %Y "$latest")
now=$(date +%s)
age_hours=$(( (now - mtime) / 3600 ))

if [ "$age_hours" -gt "$max_age_hours" ]; then
    /alert.sh "backup desactualizado (${age_hours}h)" "El backup local más reciente (${latest}) tiene ${age_hours} horas de antigüedad — supera el límite de ${max_age_hours}h configurado en BACKUP_MAX_AGE_HOURS. Revisar por qué backup.sh viene fallando (docker compose logs backup)."
fi
