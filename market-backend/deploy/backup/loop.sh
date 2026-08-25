#!/bin/sh
# Entrypoint del contenedor: corre backup.sh una vez al arrancar y después
# cada BACKUP_INTERVAL_SECONDS (default 24h). No es un cron de hora fija
# (ej. "siempre 3am") a propósito — un contenedor puede reiniciarse en
# cualquier momento (deploy, reboot del host) y un intervalo desde el
# arranque es más simple y no depende de que crond esté vivo a esa hora
# exacta. Si de verdad se necesita una hora fija, reemplazar este loop por
# un cron real del sistema operativo del host.
set -eu

while true; do
    /backup.sh || echo "[$(date -Iseconds)] intento de backup falló, se reintenta en el próximo ciclo" >&2
    sleep "${BACKUP_INTERVAL_SECONDS:-86400}"
done
