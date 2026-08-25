# Auditoría de sesiones, navegación y acciones

Subsistema de auditoría de **actividad de usuario y seguridad**. No sustituye a la
auditoría de seguridad existente (`SecurityAuditService` + logger `SECURITY_AUDIT`),
que se conserva como respaldo operativo; ambos coexisten.

> **Distinción importante.** Si existen tablas de histórico por trigger de BD sobre
> filas de seguridad (usuarios, roles, permisos…), registran *cambios de estado de
> fila*. `AUDIT_EVENT` registra *actividad* (autenticación, navegación, negocio,
> seguridad) observada por el backend. Son cosas distintas y viven en bases distintas.

## 1. Flujo

```text
Controller / Service / Seguridad
        -> AuditPublisher            (contrato; los productores solo conocen esto)
        -> OutboxAuditPublisher      (escribe en AUDIT_OUTBOX, base OPERATIVA,
                                       dentro de la transacción en curso)
        -> [ AUDIT_OUTBOX ]          (patrón outbox, entrega confiable)
        -> AuditEventProcessor       (poller programado; reclama lote seguro)
        -> AuditWriter               (escribe idempotente en la base de AUDITORÍA)
        -> [ AUDIT_EVENT / AUDIT_SESSION ]   (MARKET_AUDIT, PostgreSQL aparte)
```

- El request de negocio **no** espera la escritura final en `MARKET_AUDIT`: solo
  inserta una fila en el outbox (misma transacción que el cambio de negocio).
- El **único propietario** de las escrituras en `MARKET_AUDIT` es el procesador.
- Una librería/productor **nunca** escribe directamente en la base de auditoría.

## 2. Responsabilidades

| Componente | Responsabilidad |
| --- | --- |
| `AuditPublisher` | Contrato de publicación. Única dependencia de los productores. |
| `OutboxAuditPublisher` | Serializa el evento y lo guarda en `AUDIT_OUTBOX` (base operativa). Fail-open. |
| `AuditOutbox` / `AuditOutboxRepository` | Cola durable + reclamo seguro multi-instancia. |
| `AuditEventProcessor` | Poller: reclama, procesa, reintenta, dead-letter. Recuperación tras reinicio. |
| `OutboxManager` | Transiciones del outbox (reclamo/DONE/fallo/backoff) en la base operativa. |
| `AuditWriter` | Escribe eventos y materializa el ciclo de vida de sesiones en la base de auditoría. Idempotente. |
| `AuditQueryService` | Consultas y exportación del historial (solo lectura). |
| `AuthAuditRecorder` | Eventos de login/logout en transacción propia y corta. |
| `AuditCaptureInterceptor` | Captura automática de acceso/denegación en endpoints `@RequiresPermission`. |

### Extraer el procesador a un servicio independiente

Los productores dependen solo de `AuditPublisher`. Para separar el procesador:

1. Sustituir `OutboxAuditPublisher` por una implementación que publique al transporte
   nuevo (p. ej. Kafka) — **sin tocar los módulos de negocio**.
2. Mover `AuditEventProcessor`, `AuditWriter` y los repositorios/entidades de
   `audit.persistence` a un servicio aparte que consuma ese transporte y sea el único
   con credenciales de `MARKET_AUDIT`.
3. El contrato `AuditEvent` (JSON, versionado con `schemaVersion`) es la frontera.

## 3. Entrega confiable, reintentos y dead-letter

- **Outbox** en la base operativa. Cada evento tiene `eventId` (UUID) único.
- **Reclamo multi-instancia**: `findClaimableIds` + `claim(... WHERE status=PENDING)` con
  `claimToken` único por lote. El bloqueo de fila serializa los UPDATE concurrentes:
  cada fila la toma exactamente una instancia.
- **Idempotencia**: `AUDIT_EVENT.EVENT_ID` es UNIQUE y `AuditWriter` comprueba
  `existsByEventId` antes de insertar. El outbox se marca `DONE` **solo tras** confirmar
  la escritura → semántica efectivamente-una-vez sobre entrega at-least-once.
- **Sin transacción distribuida**: la escritura en auditoría (tx de auditoría) y el
  marcado del outbox (tx operativa) son transacciones separadas.
- **Backoff** exponencial acotado (`backoff-base`·2ⁿ ≤ `backoff-max`); tras
  `max-attempts` la fila pasa a `DEAD` (dead-letter).
- **Recuperación tras reinicio**: las filas viven en la BD; `requeueStale` devuelve a
  `PENDING` las que quedaron `PROCESSING` más de `claim-timeout`.
- **Política ante fallos**: `fail-open` para navegación y operaciones ordinarias (un
  fallo de auditoría no rompe el flujo HTTP). Toda pérdida/atraso/dead-letter es
  visible por métricas. Configurable para exigir `fail-closed` en acciones críticas.

## 4. Sesiones y JWT

- Al autenticar se genera un `sessionId` (UUID) y se añade al JWT como claim `sid`.
- Se emite `LOGIN` (AUTH/SUCCESS) asociado a esa sesión; el procesador crea `AUDIT_SESSION`.
- `POST /api/v1/auth/logout` registra `LOGOUT` y el procesador cierra la sesión
  (`end_reason=LOGOUT`). **No** invalida el JWT: la API es stateless y el cliente
  descarta el token. Una revocación real requeriría un registro de sesiones consultado
  por el Resource Server (no implementado; documentado como extensión).
- **Tokens sin `sid`** (emitidos antes de esta función) siguen siendo válidos: `sid` es
  opcional; su ausencia se trata como contexto autenticado sin sesión de auditoría.
- Se conservan `sub`, `jti` y `sver`. El JWT nunca se almacena en auditoría.

### Dos métricas de tiempo (no confundir)

- **Duración calendario**: `ended_at − started_at` (o ahora, si sigue abierta).
- **Tiempo activo estimado** (`active_millis`): acumulado por actividad/heartbeat. Es
  una **estimación**; **no** equivale al tiempo que el usuario observó la pantalla.

## 5. Navegación y heartbeat (declarados por el cliente)

- `POST /api/v1/audit/navigation`: identidad, sesión e IP se toman del **contexto
  autenticado**, nunca del cuerpo. Valida `navType` contra una allowlist y el acceso a
  la pantalla contra los permisos efectivos del usuario. Marca los eventos como
  `CLIENT_DECLARED`.
- `POST /api/v1/audit/heartbeat`: actualiza actividad **como máximo una vez por
  ventana** (`heartbeat-window`); tolerante a duplicados. El cierre por inactividad lo
  hace `AuditSessionMaintenance` (programado).

### Regla técnico vs semántico (anti-doble-registro)

- La **captura automática** (`AuditCaptureInterceptor`) registra: toda **denegación**
  (403) y las **lecturas** exitosas (GET). **No** registra mutaciones exitosas.
- Las **mutaciones** (crear, editar, eliminar, importar, exportar) emiten un **evento
  semántico** desde el servicio de negocio, con tipo e id del objeto afectado. Así no se
  registra dos veces el mismo hecho.

## 6. Consultas administrativas

Bajo el permiso `AUDITORIA_VER` (`@RequiresPermission`, fail-closed):

- `GET /api/v1/audit/events` — búsqueda paginada por usuario, sesión, tienda, módulo,
  categoría, resultado y rango de fechas.
- `GET /api/v1/audit/sessions/{sessionId}` — detalle con duración calendario y tiempo
  activo estimado.
- `GET /api/v1/audit/events/export?format=csv` — exportación **con permiso adicional**
  `AUDITORIA_EXPORTAR`, auditada, con neutralización de inyección de fórmulas CSV.

Permisos planos separados: consultar = `AUDITORIA_VER`, exportar = `AUDITORIA_EXPORTAR`
(ver el modelo RBAC en `seguridad-desarrolladores.md`). No se devuelven entidades JPA;
solo DTOs y respuestas paginadas.

## 7. Privacidad y datos prohibidos

Nunca se almacena en logs, outbox ni auditoría: contraseñas/hashes, JWT/refresh tokens
ni `Authorization`, llaves/secretos/cadenas de conexión, cuerpos completos de
request/response, ni datos personales innecesarios.

- IP: `MASK` (enmascara último octeto/segmento), `HASH` (HMAC-SHA-256) o `NONE`.
- User-Agent: hasheado y acotado.
- `metadata`: mapa pequeño, saneado (anti log-injection) y de tamaño acotado.
- Ningún trigger ni proceso de auditoría copia jamás `password_hash` (ni ningún otro
  campo sensible) hacia una tabla de histórico: los triggers de fila deben excluir
  explícitamente esas columnas o escribir `NULL` en su lugar.

## 8. Migraciones (dos bases, Liquibase)

| Base | Location | Contenido |
| --- | --- | --- |
| Operativa | `classpath:db/changelog` (`db.changelog-master.xml`) | `audit_outbox`, tablas de negocio y seguridad de los demás módulos. |
| Auditoría | `classpath:db/audit-changelog` (`db.changelog-master.xml`) | `audit_session`, `audit_event` (append-only), índices, particionamiento mensual. |

- Liquibase operativo: datasource primario, autoconfiguración de Spring Boot.
- Liquibase de auditoría: bean dedicado con su propio `DataSource`, activado con
  `app.audit.liquibase.enabled=true`.
- `audit_event` es append-only: un trigger `BEFORE UPDATE OR DELETE` con
  `RAISE EXCEPTION` bloquea cualquier modificación o borrado de fila (PostgreSQL no
  soporta `INSTEAD OF` sobre tablas base, solo sobre vistas). Cuenta de mínimos
  privilegios: procesador con `INSERT` (+ `UPDATE` en sesiones), consulta/exportación
  con `SELECT` únicamente.

> La tabla `audit_outbox` (base operativa) se crea por changeset Liquibase; en pruebas
> puede generarse por entidades JPA (`ddl-auto=create-drop`), pero en producción
> (`ddl-auto=none`) solo existe por migración.

## 9. Retención y rendimiento

- Retención caliente (`retention.hot-days`) y archivado (`archive-days`) configurables.
- **Particionamiento declarativo nativo de PostgreSQL** (`PARTITION BY RANGE` sobre
  `occurred_at`, una partición mensual). Eliminación **por partición completa**
  (`DROP TABLE audit_event_2026_01`), no fila a fila.
- Índices por usuario, sesión, fecha, tienda, permiso y resultado (ver
  `db/audit-changelog`).
- Métricas: publicados, pendientes, edad del más antiguo, procesados, duplicados,
  reintentos, dead-letter, duración de lote, fallos de conexión.

### Fórmula de capacidad (no inventar cifras)

```text
eventos_por_día = usuarios_activos × sesiones_por_usuario × eventos_por_sesión
almacenamiento  = eventos × tamaño_promedio × factor_de_índices_y_respaldo
```

### Procedimiento de prueba de carga

1. Configurar `MARKET_AUDIT` (Testcontainers PostgreSQL o instancia dedicada).
2. Generar N publicaciones concurrentes (`AuditPublisher.publish`) y medir la latencia
   añadida al request (debe tender a ~coste de un INSERT en la base operativa).
3. Ejecutar el procesador y medir throughput (eventos/s), tamaño de lote efectivo y
   atraso de la cola (`audit.outbox.oldest_age_seconds`).
4. Reportar throughput, latencia añadida, tamaño de lote y atraso. Si no hay
   infraestructura, dejar el escenario preparado y documentar cómo ejecutarlo.

## 10. Observabilidad

Micrometer/Actuator (sin datos sensibles): contadores `audit.events.*`
(published/processed/duplicates/retries/dead_letter), `audit.db.connection_failures`,
gauges `audit.outbox.pending|dead_letter|oldest_age_seconds`, timer `audit.processor.batch`.
Health separados: `auditDb` (conectividad) y `auditProcessor` (atraso/dead-letter).

## 11. Criterios medibles para adoptar Kafka / ClickHouse / OpenSearch

Mantener PostgreSQL como fuente de verdad hasta que se cumpla alguno de forma sostenida:

- **Kafka/broker**: el atraso del outbox (`oldest_age_seconds`) crece de forma
  monótona bajo carga normal pese a lotes máximos, o se requieren varios consumidores
  independientes del evento.
- **ClickHouse**: el volumen supera lo que el particionamiento mensual + índices
  sostienen para las consultas analíticas objetivo (p. ej. > cientos de millones de
  filas con agregaciones interactivas).
- **OpenSearch/Elasticsearch**: se requiere búsqueda de texto libre/ad-hoc sobre la
  metadata que los índices relacionales no cubren.

Hasta entonces, solo se documentan como puntos de extensión.
