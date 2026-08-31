# Llaves JWT de desarrollo local

`dev-private.pem` / `dev-public.pem` son un par RSA-2048 **solo para
`application-local.yml`** — deliberadamente fuera de `src/main/resources`
para que nunca queden empacadas en el jar que se despliega (ver
`docs/plan-mejoras.md`, Fase 1). Nunca usar este par en ningún ambiente
real; `deploy/certs/` tiene el par real de producción, generado aparte.

**No están commiteadas** (Fase 4, PLAN_MEJORAS.md) — generarlas una vez al
clonar el repo:

```
./generar-llaves.sh
```

(No hace nada si ya existen. Equivale a correr a mano:
`openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out dev-private.pem`
seguido de `openssl rsa -pubout -in dev-private.pem -out dev-public.pem`.)

`application-local.yml` las referencia por defecto vía `file:./local-dev/certs/dev-*.pem`
— ruta relativa al directorio desde el que se ejecuta `mvn spring-boot:run`
(la raíz del módulo `market-backend/`).
