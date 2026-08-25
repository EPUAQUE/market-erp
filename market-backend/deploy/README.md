# Deploy en un servidor Linux (GCP Compute Engine u otro)

Verificado en este repo: `docker build` de las 2 imágenes (backend:
multi-stage Java 25/Maven → JRE-alpine; backoffice: multi-stage
pnpm/Vite → Nginx-alpine) y `docker compose up` completo (Postgres +
backend + backoffice) arrancan limpio con el perfil `prod` activo —
Liquibase aplica las 76 changesets, login devuelve un JWT válido, el
backoffice sirve la SPA con fallback de rutas correcto, y un preflight
CORS desde el origin del backoffice contra el backend responde
`Access-Control-Allow-Origin` correcto. También verificado: el backend
rechaza el arranque en `prod` si `SEED_ENABLED=true`, si falta cualquier
secreto requerido, o si una llave JWT apunta a un certificado `dev-*`
(ver `docs/plan-mejoras.md`, Fase 1).

## En el servidor

1. Instalar Docker + el plugin de Compose (`docker compose version` debe
   funcionar).
2. Copiar al servidor los dos proyectos con la misma estructura relativa
   que en este repo — `docker-compose.yml` referencia
   `../market-backoffice` como build context, así que ambos deben quedar
   como hermanos:
   ```
   market-erp/
     market-backend/      (Dockerfile, docker-compose.yml, pom.xml, src/, deploy/certs/)
     market-backoffice/   (Dockerfile, nginx.conf, package.json, src/, ...)
   ```
   Incluir `deploy/certs/` completa (con las llaves `prod-*.pem` ya
   generadas — no regenerarlas en el servidor a menos que se quiera
   rotarlas).
3. En `market-backend/`, copiar `.env.example` a `.env` y llenar los
   valores reales: contraseña de Postgres, `CORS_ALLOWED_ORIGINS` (debe
   incluir el origin público del backoffice), `BACKOFFICE_API_BASE_URL`
   (la URL pública del backend, la que el navegador va a resolver — nunca
   `backend`, ese nombre solo existe dentro de la red de docker compose),
   contraseña del admin. Dejar `SEED_ENABLED=false` (o quitar la línea —
   ese es el default si se omite): `docker-compose.yml` arranca el backend
   con `SPRING_PROFILES_ACTIVE=prod`, y bajo ese perfil `ProdSafetyGuard`
   **rechaza el arranque si `SEED_ENABLED=true`** — ni siquiera en el primer
   deploy (ver "Primer admin" abajo, es un paso aparte).
4. `docker compose up -d --build` (desde `market-backend/`).
5. Verificar:
   - `docker compose logs backend --tail 50` termina con
     `Started MarketBackendApplication`.
   - `curl http://localhost:8080/actuator/health` devuelve
     `{"status":"UP",...}`.
   - `curl http://localhost:8081/` devuelve el `index.html` del
     backoffice.

### Primer admin (una sola vez, base de datos vacía)

Como el perfil `prod` nunca permite `SEED_ENABLED=true`, sembrar el primer
usuario ADMIN es un paso manual aparte — un contenedor de un solo uso con
el perfil `local` (que sí permite sembrar) apuntando a la misma base de
datos real, usando las llaves JWT reales de `deploy/certs/` (nunca las de
`local-dev/certs/`, esas son solo para desarrollo en la máquina de un
desarrollador). Verificado end-to-end en este repo:

```
docker compose run --rm \
  -e SPRING_PROFILES_ACTIVE=local \
  -e DB_URL=jdbc:postgresql://db:5432/${POSTGRES_DB} \
  -e DB_USERNAME=${POSTGRES_USER} \
  -e DB_PASSWORD=${POSTGRES_PASSWORD} \
  -e SEED_ENABLED=true \
  -e SEED_ADMIN_USERNAME=admin \
  -e SEED_ADMIN_PASSWORD=<clave real, no la de ejemplo> \
  -e JWT_PRIVATE_KEY_LOCATION=file:/certs/prod-private.pem \
  -e JWT_PUBLIC_KEY_LOCATION=file:/certs/prod-public.pem \
  --no-deps -d --name seed-once backend

# Esperar a "Started MarketBackendApplication" en los logs, luego:
docker logs seed-once --tail 30
docker rm -f seed-once
```

Confirmar con el login normal contra el backend real (perfil `prod`, ya
corriendo aparte):
```
curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"<la que pusiste>"}'
```
Si hace falta un segundo admin más adelante, repetir el mismo contenedor
de un solo uso con `SEED_ADMIN_USERNAME` distinto — `AdminUserSeeder` no
siembra si el username ya existe, así que es seguro repetir el comando.

## Backups de Postgres

Servicio `backup` (`deploy/backup/`, imagen `postgres:16-alpine` +
`pg_dump`) corre junto a los demás. Automático: `pg_dump | gzip` al
arrancar y cada `BACKUP_INTERVAL_SECONDS` (default 86400 = 24h, ver
`.env.example`). Intervalo desde arranque, no hora fija — tolera
reinicios del contenedor en cualquier momento.

Dumps quedan en `market-backend/deploy/backups/` en el HOST (bind mount,
no volumen con nombre) como `<db>_<timestamp>.sql.gz` — sobreviven a
`docker compose down -v` aunque se borre `pgdata`. Se borran solos los de
más de `BACKUP_RETENTION_DAYS` días (default 14).

Backup manual on-demand:
```
docker compose exec backup /backup.sh
```

Listar/inspeccionar backups:
```
ls -lh deploy/backups/
```

Restaurar un dump (reemplaza los datos actuales de `PGDATABASE`):
```
gunzip -c deploy/backups/<db>_<timestamp>.sql.gz | docker compose exec -T db psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}
```

## Rollback

**Aplicación (imagen del backend/backoffice):** `docker compose build` etiqueta la
imagen como `market-backend-backend:latest` — no hay versionado de imágenes por
release en este `docker-compose.yml`. Para volver a una versión anterior: `git
checkout <commit-o-tag-anterior>` en el servidor (o en la máquina que construye) y
`docker compose up -d --build backend` de nuevo. Recomendado antes de cada deploy:
etiquetar el commit desplegado (`git tag deploy-<fecha>`) para poder ubicar
rápidamente a qué volver.

**Migraciones de Liquibase:** casi todos los changesets de este proyecto son
operaciones que Liquibase sabe revertir automáticamente sin bloque `<rollback>`
explícito (`createTable`, `addColumn`, `addUniqueConstraint`, `createIndex`,
`insert`, etc.) — la única excepción es el trigger de `movimiento_inventario`
(`inventario-001-movimiento-inventario-append-only`), que sí trae su
`<rollback>` manual porque es SQL crudo. Revertir el último changeset aplicado:
```
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```
(o `liquibase:rollback -Dliquibase.rollbackTag=<tag>` si se etiquetó el estado
previo con `liquibase:tag` antes del deploy). **Nunca editar un changeset ya
aplicado** — cualquier corrección va en un changeset nuevo (ver convención en
`CLAUDE.md`).

**Restaurar desde backup** (camino probado end-to-end, ver "Backups de Postgres"
arriba) es la opción de último recurso cuando el rollback de un changeset puntual
no alcanza — por ejemplo, si ya se aplicaron varios changesets después del que
falló, o si una migración de datos (no solo de esquema) necesita revertirse. Trae
consigo perder cualquier escritura posterior al backup restaurado, así que
solo tiene sentido junto con una ventana de mantenimiento.

## Logs

Todos los servicios loguean a stdout (`docker compose logs <servicio>`); el driver
`json-file` de Docker se configuró con rotación (10 MB × 5 archivos por contenedor)
para que no crezcan sin límite y llenen el disco del servidor con el tiempo.

## Monitoreo

`GET /actuator/health` está expuesto sin autenticar (único endpoint de Actuator
habilitado — `management.endpoints.web.exposure.include: health` en
`application.yml`) para health checks de infraestructura (load balancer, `docker
compose healthcheck`, Kubernetes). Responde `{"status":"UP",...}` sin detalle
interno (`show-details: never`) — nunca revela la URL de la base de datos ni el
estado de sus componentes a un caller sin JWT. Cualquier otra ruta de Actuator
(`/actuator/metrics`, `/actuator/env`, etc.) exige el mismo JWT que el resto de la
API y hoy no está expuesta de todas formas (`exposure.include` solo lista
`health`).

## Pendiente, fuera de este alcance

- **TLS / reverse proxy**: el backend queda en `8080` y el backoffice en
  `8081`, ambos sin TLS. Para HTTPS real (necesario para que el navegador
  no bloquee por mixed-content) hace falta un proxy delante (Caddy, Nginx,
  o el load balancer de GCP) — no incluido aquí. Si se agrega, recordar
  que `BACKOFFICE_API_BASE_URL` y `CORS_ALLOWED_ORIGINS` deben usar las
  URLs públicas finales (con `https://` y el dominio real), no
  `localhost`.
- **Firewall de GCP**: abrir los puertos que correspondan (8080 y 8081, o
  443/80 si se agrega el proxy) en las reglas de firewall de la VM.
- **market-flutter web**: este deploy no incluye el cliente Flutter web
  (el POS) — solo backend + backoffice. Si también se necesita, es un
  build (`flutter build web`) servido igual por un Nginx estático, mismo
  patrón que el backoffice.
