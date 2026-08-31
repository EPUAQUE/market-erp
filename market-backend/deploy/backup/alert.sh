#!/bin/sh
# Envía una alerta por correo vía msmtp. Si no hay SMTP configurado, solo
# deja constancia en los logs del contenedor (docker compose logs backup) —
# nunca falla el llamador por no poder avisar.
# Uso: alert.sh "<asunto>" "<cuerpo>"
set -eu

asunto="${1:?asunto requerido}"
cuerpo="${2:?cuerpo requerido}"

log() { echo "[$(date -Iseconds)] $*" >&2; }

if [ -z "${ALERT_SMTP_HOST:-}" ] || [ -z "${ALERT_EMAIL_TO:-}" ]; then
    log "SMTP no configurado (ALERT_SMTP_HOST/ALERT_EMAIL_TO vacíos) — solo log, sin enviar correo"
    log "ALERTA: ${asunto} — ${cuerpo}"
    exit 0
fi

msmtprc=$(mktemp)
trap 'rm -f "$msmtprc"' EXIT
cat > "$msmtprc" <<EOF
account default
host ${ALERT_SMTP_HOST}
port ${ALERT_SMTP_PORT:-587}
auth on
user ${ALERT_SMTP_USER}
password ${ALERT_SMTP_PASSWORD}
tls on
tls_starttls on
from ${ALERT_EMAIL_FROM:-backups@localhost}
EOF
chmod 600 "$msmtprc"

if {
    echo "To: ${ALERT_EMAIL_TO}"
    echo "Subject: [market-backend backup] ${asunto}"
    echo
    echo "${cuerpo}"
} | msmtp --file="$msmtprc" -t; then
    log "alerta enviada por correo a ${ALERT_EMAIL_TO}"
else
    log "no se pudo enviar la alerta por correo (revisar ALERT_SMTP_*) — ALERTA original: ${asunto} — ${cuerpo}"
fi
