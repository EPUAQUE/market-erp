# syntax=docker/dockerfile:1

# Misma versión mayor que el servicio "db" — pg_dump de una versión más
# nueva puede fallar contra un servidor más viejo.
FROM postgres:16-alpine

# rclone: sube el bundle cifrado a GCS (o cualquier backend que rclone
# soporte) y verifica el hash después de subir — más robusto que hablar la
# REST API de GCS a mano. gnupg: cifra el bundle antes de subirlo. msmtp:
# alertas por correo cuando algo falla.
RUN apk add --no-cache rclone gnupg msmtp

COPY backup.sh loop.sh check-freshness.sh restore.sh alert.sh /
RUN chmod +x /backup.sh /loop.sh /check-freshness.sh /restore.sh /alert.sh

ENTRYPOINT ["/loop.sh"]
