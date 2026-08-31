#!/bin/sh
# Genera el par RSA-2048 de test (test-private.pem/test-public.pem) si no
# existen todavía. Nunca se commitean (ver .gitignore) — hace falta correrlo
# antes de "mvn test"/"mvn verify" en cualquier checkout nuevo (CI ya lo
# corre como paso propio, ver .github/workflows/ci.yml). Solo usado para
# firmar tokens dentro de la JVM de test — no tiene ningún valor fuera de
# esa ejecución.
set -eu
cd "$(dirname "$0")"

if [ -f test-private.pem ] && [ -f test-public.pem ]; then
    echo "test-private.pem / test-public.pem ya existen — no se regeneran."
    exit 0
fi

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out test-private.pem
openssl rsa -pubout -in test-private.pem -out test-public.pem
echo "Generado test-private.pem / test-public.pem en $(pwd)"
