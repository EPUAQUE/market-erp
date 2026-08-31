#!/bin/sh
# Genera el par RSA-2048 de desarrollo local (dev-private.pem/dev-public.pem)
# si no existen todavía. Nunca se commitean (ver .gitignore) — cada quien
# corre esto una vez al clonar el repo. Nunca usar este par en ningún
# ambiente real; deploy/certs/ tiene el par real de producción, generado
# aparte (ver deploy/certs/README.md).
set -eu
cd "$(dirname "$0")"

if [ -f dev-private.pem ] && [ -f dev-public.pem ]; then
    echo "dev-private.pem / dev-public.pem ya existen — no se regeneran (borralos a mano si querés uno nuevo)."
    exit 0
fi

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out dev-private.pem
openssl rsa -pubout -in dev-private.pem -out dev-public.pem
echo "Generado dev-private.pem / dev-public.pem en $(pwd)"
