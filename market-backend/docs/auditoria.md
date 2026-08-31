# Auditoría de seguridad y operaciones críticas

Este documento describía originalmente (versión previa a Fase 7, PLAN_MEJORAS.md)
un subsistema con outbox, base de auditoría separada, poller multi-instancia,
dead-letter y particionamiento — **nada de eso llegó a implementarse**. Este
documento ahora describe lo que **sí existe en el código**, y por qué se optó por
un diseño más simple.

## 1. Por qué no el outbox

El diseño original resuelve un problema real cuando el destino final está
desacoplado o puede fallar de forma independiente (una cola externa, un SIEM, un
servicio de auditoría aparte). En este proyecto el destino es una tabla
(`audit_event`) en la **misma base de datos operativa** — escribir el evento en
la **misma transacción** que la operación de negocio que audita da mejor
consistencia (ambos comitean o ninguno) que un outbox con poller asíncrono, sin
la complejidad de reclamos, backoff, dead-letter ni coordinación multi-instancia.

No hay una base `MARKET_AUDIT` separada, no hay `AuditEventProcessor`, no hay
`schemaVersion`, no hay claim de outbox. Todo lo que sigue es lo real.

## 2. Dos productores, un mismo destino

### 2.1 `SecurityAuditPublisher` (seguridad: login/logout/refresh/asignaciones)

Interfaz existente desde antes de Fase 7, implementada por
`SecurityAuditPublisherImpl` (`seguridad/infrastructure/security/`). Cubre los
14 call sites ya existentes en `AuthServiceImpl` (login, refresh, refresh
reutilizado, logout) y `UsuarioServiceImpl` (alta de usuario, asignación de
tienda/grupo, cambio/restablecimiento de contraseña, revocación de sesiones) —
**ninguno de esos call sites cambió** para lograr esto. `publicar(tipo,
correlationId, detalle)` ahora, además de logear a `SECURITY_AUDIT` y contar
`market.security.evento` (como siempre hacía), también:

- Persiste una fila en `audit_event` (resuelve el actor de
  `SecurityContextHolder` si hay uno autenticado — caso de las asignaciones
  hechas por un admin sobre otro usuario — o, si no, del propio `usuarioId`
  que el detalle ya venía incluyendo de forma consistente en los 14 call
  sites — caso de login/refresh, donde sujeto y actor son la misma persona).
- Dispara una alerta por correo para `REFRESH_REUTILIZADO` (señal fuerte de
  secuestro de sesión) y `RATE_LIMIT_ALCANZADO` (fuerza bruta/credential
  stuffing) — ver §4.

`RATE_LIMIT_ALCANZADO` estaba declarado en `TipoEventoAuditoria` desde antes
pero nunca se disparaba (confirmado al auditar el código antes de esta fase).
Se agregó el llamado en `GlobalExceptionHandler.handleRateLimit` — punto único
ya usado para traducir esa excepción a HTTP en cualquier endpoint
rate-limitado, no solo login.

### 2.2 `@Auditable` + `AuditoriaAspect` (operaciones de negocio)

Nueva anotación `auditoria.infrastructure.aop.Auditable` + un solo
`AuditoriaAspect` (Spring AOP, primer uso de AOP en este proyecto — requirió
agregar `spring-boot-starter-aspectj`, el reemplazo de
`spring-boot-starter-aop` en Spring Boot 4.x). Se anota el método de servicio,
sin tocar su cuerpo:

```java
@Auditable(accion = "VENTA_COMPLETADA", entidad = "VENTA", tiendaIdParam = "tiendaId", entidadIdParam = "id")
public VentaResumen completar(Long tiendaId, Long id, List<PagoInmediato> pagosInmediatos) { ... }
```

El aspecto resuelve `tienda`/`entidadId` por el **nombre real del parámetro**
(`-parameters` ya activo en `maven-compiler-plugin`) o, para métodos `crear`
donde el id solo existe en el objeto devuelto, por reflexión sobre `.id()` del
resultado (`entidadIdFromReturn = true` — todos los DTO `*Resumen` de este
proyecto son records con ese accessor). El actor se resuelve de
`SecurityContextHolder` — seguro, porque todo método cubierto está detrás de
`@RequiresPermission` en su controller. El `resultado` (`EXITO`/`FALLO`) se
deriva de si el método lanzó excepción.

Cubre 8 de las 9 categorías de auditoría que pedía Fase 7: precios/configuración
por tienda (`ProductoTiendaServiceImpl.asignar/actualizar`), ajustes de
inventario (`InventarioServiceImpl.registrarMovimiento`), caja
(`CajaServiceImpl.abrir/registrarMovimiento/cerrar`), ventas
(`VentaServiceImpl.completar/anular`), compras
(`CompraServiceImpl.crear/recibir/anular`), cuentas por pagar
(`CuentaPorPagarServiceImpl.registrarPago/anular`), traslados
(`TrasladoServiceImpl.crear/completar/anular`) y FEL
(`FelServiceImpl.emitir/reintentar/anular`).

**"Exportaciones de reportes" queda fuera** — confirmado que
`ReporteController` solo devuelve JSON hoy, no existe ningún endpoint de
exportación (CSV/PDF/Excel) para auditar. Es alcance de Fase 10, no se inventó
esa función acá.

> **No es estrictamente atómico con la operación de negocio.** El aspecto corre
> alrededor del proxy transaccional del método (orden por defecto de Spring
> para aspectos sin `@Order`), así que `registrar(...)` corre en su propia
> transacción, inmediatamente después de que la transacción de negocio ya
> comiteó (caso éxito) o ya hizo rollback (caso fallo — y ahí es exactamente
> el comportamiento deseado: el evento de auditoría de un fallo debe
> sobrevivir aunque el cambio de negocio se revierta). El único riesgo real es
> una falla de infraestructura entre ambos commits en el caso éxito — raro, y
> de todos modos mejor que no tener auditoría. `SecurityAuditPublisherImpl`,
> llamado desde DENTRO del método de negocio (no vía este aspecto), sí escribe
> en la misma transacción.

## 3. `audit_event` — append-only, sin outbox

Una sola tabla, en la base operativa (`auditoria/001-audit-event.xml`):
`id, fecha, actor_id, actor_username, tienda_id, accion, entidad, entidad_id,
resultado, correlation_id, detalle`. `actor_id`/`tienda_id` son nullable (no
todo evento tiene uno — login fallido no tiene tienda; rate limit no tiene
actor todavía). `actor_username` está denormalizado a propósito: sobrevive
aunque el usuario cambie de nombre o se desactive después.

**Append-only vía trigger**, mismo patrón exacto que `movimiento_inventario`
(`inventario/001-inventario.xml`): `BEFORE UPDATE OR DELETE` con `RAISE
EXCEPTION`. Nadie, ni la propia aplicación, puede editar o borrar una fila ya
escrita.

**Retención — decisión explícita, no automatizada.** El propio trigger que
protege contra modificación bloquearía también un borrado automático
programado — automatizarlo necesitaría deshabilitar la protección recién
construida, lo que la vacía de sentido (mismo criterio que
`movimiento_inventario`, que tampoco tiene borrado automático). Política:
**retener indefinidamente**; una purga real, si algún día hace falta por
espacio, es un proceso manual de DBA fuera de la aplicación (deshabilitar el
trigger a mano, purgar, re-habilitarlo) — nunca código de aplicación.

## 4. Alertas por correo

`shared.infrastructure.alertas.AlertaEmailService` (`JavaMailSender`,
`spring-boot-starter-mail`), configurado con las mismas variables de entorno
`ALERT_SMTP_*`/`ALERT_EMAIL_*` que ya usa `deploy/backup/alert.sh` (Fase 6,
PLAN_MEJORAS.md) — un solo canal, no dos configuraciones distintas. Sin
`ALERT_EMAIL_TO` configurado, cae a solo-log (mismo criterio que el lado
shell) — nunca propaga una excepción hacia el flujo que la disparó.

Dos tipos de alerta, disparados desde `SecurityAuditPublisherImpl`:

| Tipo | Qué significa | Qué hacer |
| --- | --- | --- |
| `REFRESH_REUTILIZADO` | Alguien reenvió un refresh token ya consumido — posible sesión comprometida (token robado, o dos dispositivos compitiendo por el mismo). El backend ya revoca toda la familia de tokens del usuario automáticamente. | Revisar `audit_event` filtrando por ese `actor_id` para ver el patrón (IP, hora). Si se confirma compromiso, contactar al usuario y considerar `POST /usuarios/{id}/sesiones/revocar` igual (ya son revocadas, pero fuerza el flujo de aviso). |
| `RATE_LIMIT_ALCANZADO` | Se agotó el límite de intentos de login para una IP o un usuario — posible fuerza bruta o credential stuffing. | Revisar `audit_event`/logs por esa IP/usuario. Si es un patrón sostenido, considerar bloqueo a nivel de firewall/Caddy (fuera del alcance de este backend). |

## 5. Observabilidad — solo lo mínimo, sin Prometheus/Grafana todavía

`/actuator/prometheus` expuesto (`micrometer-registry-prometheus`), mismo
alcance de red que `/actuator/health` (`127.0.0.1:8080` únicamente vía
`docker-compose.yml`) — decisión explícita del usuario: no se monta
Prometheus/Grafana en la VM todavía, esto solo deja el endpoint listo para
cuando se decida dónde correr ese stack. Dashboards y alertas basadas en
umbrales de métricas (latencia HTTP, tasa de error) quedan pendientes de esa
decisión de infraestructura — no se puede completar sin ella.

`CorrelationIdFilter` (`shared.infrastructure.web`) pone un `correlationId`
en `MDC` en cada request (header `X-Correlation-Id` de entrada, o un UUID
nuevo) — aparece en toda línea de log (`logging.pattern.level` en
`application.yml`) y se agrega como header en toda respuesta, no solo en
errores como antes. `GlobalExceptionHandler`, `SecurityAuditPublisherImpl` y
`AuditoriaAspect` lo leen de MDC en vez de generar cada uno el suyo.

## 6. Privacidad

`detalle` es texto sanitizado que arma el productor antes de llegar a
`AuditEvent` — nunca contraseñas, hashes, tokens completos ni cuerpos de
request/response. Mismo criterio que ya tenía `SecurityAuditPublisherImpl`
desde antes de esta fase.

## 7. Consulta

`GET /api/v1/auditoria` (todo, paginado) y `GET /api/v1/auditoria/tiendas/{tiendaId}`
(filtrado por tienda), permiso `AUDITORIA_VER` — sembrado para `ADMIN` y
`AUDITOR` (rol ya existente, ver `seguridad/010-seed-auditor-permisos.xml`).
Sin filtros adicionales (por actor, acción, rango de fechas) en esta pasada —
si hace falta, es una extensión chica sobre `AuditEventRepository`, no un
rediseño.
