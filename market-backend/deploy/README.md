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

TLS es vía **Caddy** (`deploy/Caddyfile`), con certificado automático de
Let's Encrypt — un solo dominio público, ruteado por path: `/` va al
backoffice, `/api/*` va al backend. Backend y backoffice ya no exponen
puerto directo a internet (solo `127.0.0.1` en la VM, para `curl` de
verificación) — todo el tráfico público entra por Caddy en 80/443.

## DNS

Antes de levantar `caddy`, apuntar un registro **A** del dominio a la IP
pública (externa) de la VM. Confirmar que ya propagó antes del siguiente
paso:
```
dig +short tu-dominio.com
```
Debe devolver la IP de la VM. Si Caddy arranca antes de que el DNS
propague, el challenge ACME de Let's Encrypt falla — solo hay que esperar
a que propague y Caddy reintenta solo (no hace falta reiniciar el
contenedor a mano, aunque un `docker compose restart caddy` no hace daño
si se quiere forzar un reintento inmediato).

## Firewall de GCP

Abrir **80** (challenge ACME + redirect a HTTPS) y **443** (tráfico real)
en las reglas de firewall de la VM — por ejemplo:
```
gcloud compute firewall-rules create allow-http-https \
  --allow=tcp:80,tcp:443 \
  --target-tags=<tag-de-la-vm> \
  --direction=INGRESS
```
(o el equivalente desde la consola web: VPC network → Firewall). **8080 y
8081 ya no necesitan regla pública** — quedan atados a `127.0.0.1` en el
`docker-compose.yml`.

## En el servidor

1. Instalar Docker + el plugin de Compose (`docker compose version` debe
   funcionar).
2. Copiar al servidor los dos proyectos con la misma estructura relativa
   que en este repo — `docker-compose.yml` referencia
   `../market-backoffice` como build context, así que ambos deben quedar
   como hermanos:
   ```
   market-erp/
     market-backend/      (Dockerfile, docker-compose.yml, pom.xml, src/, deploy/certs/, deploy/Caddyfile)
     market-backoffice/   (Dockerfile, nginx.conf, package.json, src/, ...)
   ```
   Incluir `deploy/certs/` completa (con las llaves `prod-*.pem` ya
   generadas — no regenerarlas en el servidor a menos que se quiera
   rotarlas).
3. En `market-backend/`, copiar `.env.example` a `.env` y llenar los
   valores reales: `DOMAIN` (el dominio real, ya apuntado por DNS —
   `CORS_ALLOWED_ORIGINS` y la URL base del backoffice se derivan de este
   valor solos, no hay que repetirlo en otra variable), contraseña de
   Postgres, contraseña del admin. Dejar `SEED_ENABLED=false` (o quitar la
   línea — ese es el default si se omite): `docker-compose.yml` arranca el
   backend con `SPRING_PROFILES_ACTIVE=prod`, y bajo ese perfil
   `ProdSafetyGuard` **rechaza el arranque si `SEED_ENABLED=true`** — ni
   siquiera en el primer deploy (ver "Primer admin" abajo, es un paso
   aparte).
4. `docker compose up -d --build` (desde `market-backend/`).
5. Verificar:
   - `docker compose logs backend --tail 50` termina con
     `Started MarketBackendApplication`.
   - `docker compose logs caddy --tail 50` — sin errores de ACME/TLS (la
     primera emisión de certificado puede tardar unos segundos a un par de
     minutos).
   - `curl http://localhost:8080/actuator/health` (desde la VM) devuelve
     `{"status":"UP",...}`.
   - `curl http://localhost:8081/` (desde la VM) devuelve el `index.html`
     del backoffice.
   - `curl -i https://tu-dominio.com/api/v1/auth/login -X POST -H "Content-Type: application/json" -d '{}'`
     (desde donde sea) devuelve `400`/`401` de la API real — no el
     `index.html` del backoffice — confirmando que Caddy sí está enrutando
     `/api/*` al backend con certificado válido. (`/actuator/health` no
     está expuesto públicamente a propósito — sigue siendo solo
     `127.0.0.1`, ver "Monitoreo" abajo.)
   - `curl -I https://tu-dominio.com/` devuelve `200` con el `index.html`
     del backoffice.

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

## RPO/RTO

Acordado (Fase 6, `PLAN_MEJORAS.md`):

- **RPO (pérdida máxima de datos aceptable): ≤24h** — cubierto por el
  intervalo diario actual de `backup.sh` (`BACKUP_INTERVAL_SECONDS`,
  default 86400). Si el negocio necesita menos pérdida más adelante, basta
  con bajar ese intervalo — el resto del pipeline (cifrado, subida,
  checksum, alertas) no cambia.
- **RTO (tiempo máximo para reconstruir el ambiente): unas horas** — el
  runbook de "Restauración automatizada" de abajo es lo que hay que seguir
  para cumplirlo: provisionar una VM nueva, `docker compose up` vacío,
  `restore.sh` con el bundle más reciente de GCS, verificar que el
  backoffice/POS puedan loguearse contra los datos restaurados.

## Backups de Postgres

Servicio `backup` (`deploy/backup/`, imagen `postgres:16-alpine` +
`pg_dump` + `rclone` + `gnupg` + `msmtp`) corre junto a los demás. Dos
capas, la segunda es opcional pero muy recomendada antes de producción:

**1. Dump local (sin cambios respecto a antes)**: `pg_dump | gzip` al
arrancar y cada `BACKUP_INTERVAL_SECONDS` (default 86400 = 24h). Intervalo
desde arranque, no hora fija — tolera reinicios del contenedor en
cualquier momento. Queda en `market-backend/deploy/backups/` en el HOST
(bind mount, no volumen con nombre) como `<db>_<timestamp>.sql.gz` —
sobrevive a `docker compose down -v` aunque se borre `pgdata`. Se borran
solos los de más de `BACKUP_RETENTION_DAYS` días (default 14).

**2. Bundle cifrado a almacenamiento externo (GCS)**: si `GCS_BUCKET` y
`BACKUP_ENCRYPTION_PASSPHRASE` están configurados (`.env.example` los
documenta, vacíos por default — sin ellos el backup sigue funcionando
solo-local, igual que antes), cada corrida arma un solo archivo
`<db>_<timestamp>.bundle.tar.gz.gpg` con: el dump, un tar de
`productos_imagenes`, un tar de `deploy/certs/*.pem`, y una copia del
`.env` — cifrado simétrico AES256 (GPG) con la passphrase, más un
`.sha256` al lado. Sube ambos a `gs://$GCS_BUCKET/backups/` con `rclone`
(que ya verifica el hash después de subir — si algo se corrompe en
tránsito, el job falla). Cualquier fallo en cualquier paso (dump, cifrado,
subida) manda una alerta por correo (ver "Alertas" abajo) y termina en
`exit 1`; además, al inicio de cada vuelta del loop, `check-freshness.sh`
alerta si el dump local más reciente supera `BACKUP_MAX_AGE_HOURS`
(default 30h) — para el caso "el contenedor sigue vivo pero viene
fallando hace rato".

**`rclone` nunca usa una llave JSON estática en el servidor** — toma
credenciales de la cuenta de servicio adjunta a la VM vía el metadata
server de GCE (`RCLONE_CONFIG_GCS_ENV_AUTH=true`, ya en
`docker-compose.yml`). Configuración necesaria del lado de GCP, una sola
vez:

```bash
# 1. Crear el bucket, con versioning (protege contra sobrescritura/borrado
#    accidental de un backup — GCS guarda versiones anteriores del mismo
#    nombre de objeto).
gcloud storage buckets create gs://<nombre-del-bucket> \
  --location=<región, ej. us-central1> \
  --uniform-bucket-level-access

gcloud storage buckets update gs://<nombre-del-bucket> --versioning

# 2. (Opcional) Regla de ciclo de vida — borra versiones viejas después de
#    90 días y cualquier objeto después de 365 días. Ver
#    deploy/backup/gcs-lifecycle-example.json (ajustar antes de aplicar).
gcloud storage buckets update gs://<nombre-del-bucket> \
  --lifecycle-file=deploy/backup/gcs-lifecycle-example.json

# 3. Cuenta de servicio de ESCRITURA, alcance mínimo (solo este bucket) —
#    la que usará la VM en producción.
gcloud iam service-accounts create market-backup-writer \
  --display-name="Market ERP - backup writer"

gcloud storage buckets add-iam-policy-binding gs://<nombre-del-bucket> \
  --member="serviceAccount:market-backup-writer@<project-id>.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin"

# 4. Adjuntar esa cuenta de servicio a la VM (requiere pararla una vez si
#    ya está corriendo — no se puede cambiar en caliente).
gcloud compute instances stop <nombre-vm> --zone=<zona>
gcloud compute instances set-service-account <nombre-vm> --zone=<zona> \
  --service-account=market-backup-writer@<project-id>.iam.gserviceaccount.com \
  --scopes=https://www.googleapis.com/auth/devstorage.read_write
gcloud compute instances start <nombre-vm> --zone=<zona>
```

Después, en el `.env` del servidor: `GCS_BUCKET=<nombre-del-bucket>` y una
`BACKUP_ENCRYPTION_PASSPHRASE` real.

> **Crítico**: `BACKUP_ENCRYPTION_PASSPHRASE` es la única llave que
> descifra todo lo que hay en GCS. Guardala en un gestor de contraseñas o
> vault **fuera** de este servidor (nunca solo en su `.env`) — si el
> servidor se pierde y la passphrase se perdió con él, los backups en GCS
> quedan inservibles para siempre, ni siquiera vos podés recuperarlos.

### Alertas

`ALERT_SMTP_HOST`/`PORT`/`USER`/`PASSWORD`/`ALERT_EMAIL_FROM`/
`ALERT_EMAIL_TO` en `.env` (ver `.env.example`) — sirve con cualquier SMTP
que ya tengas (Gmail con contraseña de aplicación, SMTP corporativo,
etc.). Sin configurar, las alertas quedan solo en
`docker compose logs backup`.

### Comandos sueltos

Backup manual on-demand:
```
docker compose exec backup /backup.sh
```

Listar/inspeccionar backups locales:
```
ls -lh deploy/backups/
```

Restaurar el dump local (reemplaza los datos actuales de `PGDATABASE`,
sin tocar imágenes/certs/`.env` — para eso ver "Restauración automatizada"
abajo):
```
gunzip -c deploy/backups/<db>_<timestamp>.sql.gz | docker compose exec -T db psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}
```

## Restauración automatizada

`docker compose exec backup /restore.sh [nombre-del-bundle]` — sin
argumento, toma el bundle más reciente de GCS. Verifica el `.sha256`,
descifra con `BACKUP_ENCRYPTION_PASSPHRASE`, y restaura:

- **Base de datos**: se aplica directo (`psql`), **sobreescribe**
  `PGDATABASE` actual.
- **Imágenes de producto**: se aplican directo sobre el volumen
  `productos_imagenes` (bajo riesgo, son solo archivos).
- **Certs y `.env` del bundle**: **no se sobreescriben solos** — quedan
  guardados en `deploy/backups/certs-restaurados-<fecha>/` y
  `deploy/backups/env-restaurado-<fecha>` para que una persona los revise
  y copie a mano si hace falta. Son demasiado sensibles para aplicarlos
  sin supervisión.

Pensado para correr manual siempre — restaurar sobre producción no debe
pasar solo por accidente (nunca se dispara desde `loop.sh`).

### Ensayo mensual automatizado

`.github/workflows/backup-restore-drill.yml` corre el día 1 de cada mes
(y bajo demanda, `workflow_dispatch`): baja el bundle más reciente,
verifica checksum, descifra, restaura contra un Postgres descartable del
propio job, y corre un sanity check de conteo de filas. Si algo falla, el
job falla — eso es la alerta, GitHub ya notifica fallos de workflow al
equipo. Cumple "ejecutar una restauración programada al menos
mensualmente" sin depender de que alguien se acuerde.

Requiere 3 secrets nuevos en la configuración del repo de GitHub
(**Settings → Secrets and variables → Actions**), que hay que crear a
mano — no soy yo quien puede agregarlos:

- `GCS_BUCKET`: mismo nombre que en el `.env` del servidor.
- `BACKUP_ENCRYPTION_PASSPHRASE`: misma passphrase que en el `.env` del
  servidor.
- `GCP_BACKUP_READONLY_SA_KEY`: el JSON de una cuenta de servicio nueva,
  de **solo lectura** sobre el bucket (distinta de la que usa la VM,
  alcance mínimo — este es el único lugar de todo el sistema donde sí
  hace falta una llave JSON estática, porque un runner de GitHub Actions
  no tiene metadata server de GCE):
  ```bash
  gcloud iam service-accounts create market-backup-ci-reader \
    --display-name="Market ERP - backup CI reader (solo lectura)"

  gcloud storage buckets add-iam-policy-binding gs://<nombre-del-bucket> \
    --member="serviceAccount:market-backup-ci-reader@<project-id>.iam.gserviceaccount.com" \
    --role="roles/storage.objectViewer"

  gcloud iam service-accounts keys create ci-reader-key.json \
    --iam-account=market-backup-ci-reader@<project-id>.iam.gserviceaccount.com
  # Pegar el contenido de ci-reader-key.json como el secret
  # GCP_BACKUP_READONLY_SA_KEY, y después borrar el archivo local.
  ```

### Pendiente (requiere ejecución manual, no automatizable desde acá)

- **Probar recuperación cuando el volumen Docker completo se pierde**:
  simular `docker compose down -v` (o perder el disco) contra una copia de
  prueba del servidor, no contra producción — demasiado destructivo para
  automatizarlo sin supervisión directa. El pipeline de
  `backup.sh`/`restore.sh` en sí ya se probó de punta a punta (dump →
  cifrado → subida → descarga → descifrado → restauración, con datos,
  imágenes, certs y `.env` de prueba) contra un Postgres descartable antes
  de cerrar esta fase — lo que falta es el ensayo contra el disco/VM real.

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
`application.yml`) para health checks de infraestructura (`docker compose
healthcheck`, un script de monitoreo corriendo en la propia VM). Responde
`{"status":"UP",...}` sin detalle interno (`show-details: never`) — nunca
revela la URL de la base de datos ni el estado de sus componentes a un
caller sin JWT. Solo alcanzable en `127.0.0.1:8080` (no pasa por Caddy, no
está expuesto a internet — ver arriba). Cualquier otra ruta de Actuator
(`/actuator/metrics`, `/actuator/env`, etc.) exige el mismo JWT que el resto de la
API y hoy no está expuesta de todas formas (`exposure.include` solo lista
`health`).

## Pendiente, fuera de este alcance

- **market-flutter web**: este deploy no incluye el cliente Flutter web
  (el POS) — solo backend + backoffice. Si también se necesita, es un
  build (`flutter build web`) servido igual por un Nginx estático detrás
  de Caddy — agregar un tercer `handle` en el `Caddyfile` (ej. bajo
  `/pos/*` o un subdominio nuevo) apuntando a ese contenedor.
- **Rotación/renovación de certificado**: Caddy la maneja sola (renueva
  automáticamente antes de que expire) — no requiere `cron` ni paso manual,
  solo que el volumen `caddy_data` no se borre entre deploys.
