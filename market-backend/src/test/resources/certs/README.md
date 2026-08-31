# Llaves JWT de test

`test-private.pem` / `test-public.pem` son un par RSA-2048 usado únicamente
para firmar/validar tokens dentro de la JVM de los tests (`application-test.yml`).
Sin valor fuera de esa ejecución — nunca usar en ningún ambiente real.

**No están commiteadas** (Fase 4, PLAN_MEJORAS.md) — generarlas antes de
correr `mvn test`/`mvn verify` en cualquier checkout nuevo:

```
./generar-llaves.sh
```

CI ya corre este mismo script como paso propio antes de `mvn verify` (ver
`.github/workflows/ci.yml`) — no hace falta tocar nada ahí al clonar.
