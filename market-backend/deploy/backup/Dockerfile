# syntax=docker/dockerfile:1

# Misma versión mayor que el servicio "db" — pg_dump de una versión más
# nueva puede fallar contra un servidor más viejo.
FROM postgres:16-alpine

COPY backup.sh loop.sh /
RUN chmod +x /backup.sh /loop.sh

ENTRYPOINT ["/loop.sh"]
