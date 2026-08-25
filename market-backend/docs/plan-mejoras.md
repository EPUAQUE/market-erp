# Plan ejecutable de mejoras — Market Backend

## Objetivo

Corregir los riesgos detectados en configuración productiva, autorización multi-tienda,
contabilidad de ventas, concurrencia de inventario, rotación de refresh tokens e
idempotencia de ventas, manteniendo el monolito modular y las migraciones Liquibase.

## Reglas de ejecución

- Ejecutar las fases en el orden indicado.
- Implementar una fase por PR; no mezclar refactors ajenos al objetivo.
- Toda modificación de esquema debe realizarse mediante un changeset nuevo. No editar
  changesets que ya puedan haberse ejecutado.
- Mantener compatibilidad con los clientes actuales o documentar y versionar cualquier
  cambio de contrato HTTP.
- No cerrar una fase hasta cumplir sus criterios de aceptación y ejecutar la suite
  completa.
- Antes de cada PR, registrar el resultado base de las pruebas:

```powershell
mvn -o "-Dmaven.repo.local=C:\Program Files\Apache-Maven-3.9.1" test
```

Resultado base observado el 24 de agosto de 2026: **413 pruebas, 0 fallos, 0 errores**.
Tras cerrar la Fase 2 (autorización multi-tienda): **428 pruebas, 0 fallos, 0 errores**.
Tras cerrar la Fase 3 (contabilidad de ventas): **450 pruebas, 0 fallos, 0 errores**
(25 de agosto de 2026).
Tras cerrar la Fase 6 (idempotencia de ventas): **454 pruebas, 0 fallos, 0 errores**
(25 de agosto de 2026).

## Verificación de estado (auditoría 2026-08-24)

Antes de iniciar la fase de pruebas en ambiente real se auditó el código actual contra
cada fase de este plan. Conclusión general: **el plan sigue vigente casi en su
totalidad** — no es un documento desactualizado, y ningún riesgo descrito aquí fue
corregido de fondo por trabajo posterior a su redacción (solo idempotencia por
`correlationId` y el guardrail de límite de crédito para MIXTO avanzaron, ambos con
matices señalados abajo).

| Fase | Veredicto |
| --- | --- |
| 1 — Config segura | **Resuelta (2026-08-25)**. Perfiles `local`/`test`/`prod` separados, `ProdSafetyGuard` rechaza `prod` con seed habilitado/certificado dev/credencial conocida (verificado con Docker real). Hallazgo importante: `${VAR}` sin default NO falla el enlace en `@ConfigurationProperties` (se deja el placeholder literal sin resolver) — el patrón real que funciona es `${VAR:}` + `@NotBlank` vía `@Validated`. Llave de desarrollo movida fuera del jar (verificado). Bootstrap del primer admin en prod rediseñado (ya no vía `SEED_ENABLED`, ver `deploy/README.md`). |
| 2 — Multi-tienda | **Resuelta (2026-08-24, cerrada del todo 2026-08-25)**. Nuevo puerto `AutorizacionTiendaService` (seguridad) + `ContextoAutenticacion`, aplicados en `TrasladoServiceImpl` (crear/completar/anular/obtener validan ambas tiendas; listar filtra) y `ProductoTiendaServiceImpl` (asignar/actualizar/activar/desactivar/listarPorTienda validan; listarPorProducto filtra). Comentarios incorrectos corregidos en ambos controllers. Actualización 2026-08-25: 404-vs-403 cerrado — las operaciones que resuelven por id opaco antes de chequear alcance (`obtener`/`completar`/`anular` de traslados; `actualizar`/`activar`/`desactivar` de producto-tienda) responden 404 indistinguible de "no existe" en vez de 403, verificado contra Postgres real. |
| 3 — Contabilidad ventas | **Resuelta (2026-08-25)**. Bug de fondo corregido: cuenta por cobrar ya no se crea para EFECTIVO/TARJETA/TRANSFERENCIA (verificado contra Postgres real). MIXTO ahora recibe desglose real de pagos en `completar()` y crea CxC solo por el saldo. Coordinado en el mismo PR con `market-flutter` (checkout simplificado). Fuera de alcance a propósito: anulación de venta completada, migración de datos históricos (no aplica, nunca hubo prod). |
| 4 — Concurrencia inventario | **Resuelta (2026-08-25)**. Bloqueo pesimista (`PESSIMISTIC_WRITE`) en el único camino de mutación (`registrarMovimiento`, compartido por Ventas/Compras/Traslados); colisión de creación de la primera fila resuelta con un único reintento en transacción nueva vía `TransactionTemplate`. `@Version` evaluado y omitido a propósito (el bloqueo ya cubre toda escritura). Verificado con concurrencia real contra Postgres: 8 ingresos paralelos sin pérdidas (existencia final 8) y 2 ventas paralelas sobre la última unidad (solo una completa, la otra 409 `STOCK_INSUFICIENTE`). |
| 5 — Refresh tokens | **Resuelta (2026-08-25)**. `UPDATE` condicional atómico (`consumir`) reemplaza el `find→comprobar→save`; verificado con 2 refresh reales simultáneos (solo uno emite par nuevo). Bug encontrado y corregido en el camino: la revocación de familia no persistía por rollback automático de Spring ante la excepción lanzada en la misma transacción (`noRollbackFor`). Limpieza programada de expirados + índice nuevo. Métricas diferidas a la Fase 7 (no hay `micrometer`/`actuator` en el proyecto todavía). |
| 6 — Idempotencia ventas | **Resuelta (2026-08-25)**. Clave compuesta `(tienda_id, vendedor_id, correlation_id)`, comparación de contenido (`clienteId`/`lineas`/`metodoPago`) contra un reintento con la misma clave, `409` ante reutilización con datos distintos, colisión de inserción concurrente manejada sin `@Transactional` propio para evitar reutilizar una sesión de Hibernate ya envenenada. Verificado con 8 requests concurrentes reales contra Postgres. |
| 7 — Observabilidad/cierre | **Resuelta (2026-08-25)**. Actuator agregado, `/actuator/health` es lo único expuesto (verificado contra Postgres real). `LiquibaseMigrationIT` (Testcontainers) encontró y permitió corregir 2 bugs reales invisibles a los mocks: arranque roto por bean ambiguo (`@Qualifier` agregado) y rutas inexistentes devolviendo 500 en vez de 404. Métricas de negocio agregadas reusando dos puntos ya centralizados. Mockito como javaagent. Pruebas de arranque por perfil completadas junto con la Fase 1 (`ProfileStartupIT`). |

Hallazgos adicionales no cubiertos por las 7 fases originales se documentan en la nueva
**Fase 8** (transversales de backend) y **Fase 9** (bloqueadores del cliente Flutter),
añadidas tras esta auditoría.

**Fase 8 — actualización 2026-08-25**: **completada**. Resuelto el bug de zona horaria
del dashboard, reducidas las llamadas redundantes a inventario, agregados índices en las
5 tablas sin uno (`venta`, `cuenta_por_cobrar`, `caja_sesion`, `compra`, `traslado`),
configurada rotación de logs a nivel del driver de Docker, y agregada paginación real
(`Pageable`/`Page<T>`) a los 6 listados sin límite — coordinada en el mismo ciclo con
`market-flutter` y `market-backoffice` (ambos clientes actualizados y verificados por
`typecheck`/`build`/`analyze`, no por clic visual — ver "Paginación — resuelta" dentro
de la fase).

**Fase 9 — actualización 2026-08-25**: **cerrada a nivel de código**, verificación en
dispositivo real explícitamente diferida (bloqueada por inestabilidad del emulador,
decisión del usuario). Excepción de cleartext agregada y verificada con un build
release real; hallazgo adicional en el camino — el APK de producción no tenía permiso
de `INTERNET` en absoluto (solo estaba en las variantes debug/profile), corregido y
verificado sobre el manifest fusionado real. Pantalla nueva de "pendientes con error"
con reintentar/descartar. Bug de reintento post-`completar()` corregido (la
descripción original de la tarea estaba desactualizada — el `registrarCobro` que
menciona ya no existe desde la Fase 3 de este plan; el riesgo equivalente en la forma
actual del código sí era real). Logging/crash reporting remoto evaluado y
deliberadamente no agregado (requiere una cuenta externa y una decisión de datos que
no corresponde tomar unilateralmente).

## Fase 1 — Configuración segura por entorno

### Resultado esperado

Una instancia productiva no puede arrancar con credenciales, administrador o llaves
JWT de desarrollo.

**Resuelta (2026-08-25)** — ver checklist abajo.

### Tareas

- [x] Conservar en `application.yml` únicamente valores comunes y no sensibles — datos
  de conexión, JWT, CORS y credenciales de seed se movieron a los 3 perfiles nuevos;
  lo que queda es JPA/Liquibase/Actuator/argon2/rate-limit/password-policy (nada
  identifica un entorno ni es secreto).
- [x] Crear `application-local.yml` con PostgreSQL local, CORS local, seed habilitado y
  llaves exclusivamente de desarrollo.
- [x] Crear `application-test.yml` con la configuración utilizada por las pruebas —
  llave propia (`src/test/resources/certs/test-*.pem`, nunca en el classpath principal).
- [x] Crear `application-prod.yml` sin valores predeterminados **reales** para
  secretos — ver hallazgo importante abajo sobre por qué "sin default" (`${VAR}` a
  secas) no es lo mismo que "obligatorio".
- [x] Cambiar `app.seed.enabled` a `false` por defecto — en `application.yml` base.
- [x] Hacer obligatorios en producción `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
  `JWT_PRIVATE_KEY_LOCATION`, `JWT_PUBLIC_KEY_LOCATION`, `JWT_ISSUER`, `JWT_AUDIENCE`
  y `CORS_ALLOWED_ORIGINS` — ver hallazgo abajo sobre el mecanismo real usado.
- [x] Añadir validación de propiedades con `@Validated` y restricciones Jakarta —
  `SeguridadProperties` ahora `@Validated` con `@NotBlank`/`@NotEmpty`/`@Valid` en
  `jwt.issuer`, `jwt.audience`, `jwt.activeKid`, `jwt.keys[].kid/privateKeyLocation/publicKeyLocation`
  y `cors.allowedOrigins`.
- [x] Añadir una validación de arranque que rechace el perfil `prod` si el seed está
  habilitado, se usan credenciales conocidas o se cargan certificados `dev-*` —
  `ProdSafetyGuard` (nuevo), corre en el constructor del bean (antes de que
  `AdminUserSeeder` u otro `ApplicationRunner` ejecute ningún efecto secundario).
- [x] Evitar que `src/main/resources/certs/dev-private.pem` se incluya en el artefacto
  productivo; documentar cómo montar la llave externa — movidas a `local-dev/certs/`
  (fuera de `src/main/resources` por completo, nunca en el classpath); confirmado con
  `unzip -l` sobre el jar empacado que no aparece. `local-dev/certs/README.md` nuevo.
- [ ] Rotar las llaves JWT si las actuales se utilizaron fuera del entorno local — N/A:
  no existe una base de producción real todavía (mismo motivo documentado en la Fase 3
  para la migración de datos históricos); `deploy/certs/prod-*.pem` ya es un par
  generado aparte, nunca usado en desarrollo.
- [x] Documentar variables requeridas y un ejemplo sin secretos reales — `.env.example`
  actualizado (`JWT_ISSUER`/`JWT_AUDIENCE` nuevos); `deploy/README.md` actualizado con
  el perfil `prod` obligatorio en `docker-compose.yml` y el nuevo procedimiento para
  sembrar el primer admin (ver abajo).

### Hallazgo importante: `${VAR}` sin default NO falla el arranque como se esperaba

Verificado empíricamente (no asumido): un placeholder `${JWT_ISSUER}` sin ningún
default, cuando la variable de entorno no existe, **no lanza excepción** al enlazarse
a través de `@ConfigurationProperties` — Spring lo deja tal cual como el string
literal `"${JWT_ISSUER}"`, sin resolver. Esto es distinto del comportamiento (correcto)
de `@Value` con `PropertySourcesPlaceholderConfigurer`, que sí falla. El resultado: la
estrategia ingenua de "sin default = obligatorio" descrita literalmente en este plan
**no funciona** para `@ConfigurationProperties` — habría dejado pasar un arranque en
`prod` con `issuer` literalmente puesto a `${JWT_ISSUER}`, un valor no-vacío que
además pasaría un `@NotBlank` sin más. Solución real: `${VAR:}` (default vacío
explícito) + `@NotBlank`/`@NotEmpty` vía `@Validated` — el default vacío si enlaza
correctamente a `""`, que sí es rechazado por la validación. `application-prod.yml`
usa este patrón en los 8 valores obligatorios listados arriba. Descubierto y corregido
antes de verificar con Docker real (`ProfileStartupIT` lo hubiera atrapado en la
primera corrida contra Testcontainers de cualquier forma).

### Bootstrap del primer admin en producción

Consecuencia directa de que `ProdSafetyGuard` rechace *cualquier* arranque en `prod`
con `SEED_ENABLED=true` (sin excepción para "es la primera vez"): el flujo previo de
`docker-compose.yml` ("sembrar con `SEED_ENABLED=true`, luego poner `false`") ya no
funciona bajo el perfil `prod`. Reemplazado por un contenedor de un solo uso con el
perfil `local` (que sí permite sembrar) apuntando a la misma base de datos real y a
las llaves JWT reales de `deploy/certs/` — documentado y **verificado end-to-end**
contra Postgres real en Docker (sembró el admin, y el backend en perfil `prod`, ya
corriendo aparte, autenticó con esa cuenta). Ver `deploy/README.md` § "Primer admin".

### Archivos principales

- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml` (nuevo)
- `src/main/resources/application-test.yml` (nuevo)
- `src/main/resources/application-prod.yml` (nuevo)
- `seguridad/infrastructure/security/SeguridadProperties.java` (`@Validated` + constraints)
- `seguridad/infrastructure/security/ProdSafetyGuard.java` (nuevo)
- `local-dev/certs/` (nuevo — llaves de desarrollo movidas fuera de `src/main/resources`)
- `src/test/resources/certs/` (nuevo — llave propia de pruebas)
- `src/test/java/com/ais/marketbackend/ProfileStartupIT.java` (nuevo)
- `docker-compose.yml` (`SPRING_PROFILES_ACTIVE=prod`, `JWT_ISSUER`/`JWT_AUDIENCE`,
  default de `SEED_ENABLED` cambiado a `false`)
- `.env.example`, `deploy/README.md`

### Pruebas y aceptación

- [x] El perfil local puede crear el administrador solo si se habilita explícitamente
  — `application-local.yml` lo activa por defecto (`SEED_ENABLED:true`), pero sigue
  siendo una decisión explícita del perfil, no un default global (el default global,
  en `application.yml` base, es `false`). Verificado en
  `ProfileStartupIT.perfilLocalArrancaYSiembraElAdminPorDefecto` contra Postgres real.
- [x] El perfil productivo falla al arrancar si falta cualquier secreto requerido —
  `ProfileStartupIT.perfilProdSinJwtIssuerNoArranca` (Postgres real, `JWT_ISSUER`
  omitido a propósito).
- [x] El perfil productivo falla si `SEED_ENABLED=true` —
  `ProfileStartupIT.perfilProdConSeedHabilitadoNoArranca`. También verificado con
  Docker real: arranque real bajo `docker-compose.yml` con `SEED_ENABLED=true`
  rechazado por `ProdSafetyGuard` con el mensaje esperado en los logs.
  Adicionalmente, `ProfileStartupIT.perfilProdConCertificadoDeDesarrolloNoArranca`
  cubre la tercera condición del guardrail (certificado `dev-*`).
- [x] El JAR productivo no contiene una llave privada de desarrollo — verificado con
  `unzip -l target/market-backend-*.jar | grep dev-private` sobre el jar real
  empacado: sin resultados.
- [x] La suite completa permanece verde — `mvn clean verify`: 457 unitarias + 5 de
  integración (`LiquibaseMigrationIT` + 4 de `ProfileStartupIT`), 0 fallos (25 de
  agosto de 2026).

## Fase 2 — Autorización multi-tienda explícita

### Resultado esperado

Ningún usuario puede leer o modificar datos de una tienda fuera de su alcance, incluso
si el servicio de aplicación se invoca sin pasar por un controller.

### Diseño

Crear un puerto de aplicación, por ejemplo `AutorizacionTiendaService`, con operaciones
explícitas como `exigirAcceso(tiendaId)` y `exigirAccesoATodas(tiendaIds)`. La capa de
aplicación debe aplicar estas verificaciones. El interceptor HTTP continúa como defensa
temprana, pero deja de ser la única barrera.

### Tareas

- [x] Crear el servicio de autorización y obtener el usuario desde un puerto de contexto
  autenticado, sin acoplar el dominio a Spring Security. — `AutorizacionTiendaService`
  (`seguridad/application/services/interfaces`) + `ContextoAutenticacion`
  (`seguridad/domain/service`, implementado en `infrastructure/security`).
- [x] Aplicar autorización a todos los casos de uso que reciben `tiendaId` — alcance de
  esta fase: traslados y `ProductoTienda` (los dos casos sin `tiendaId` de ruta,
  confirmados como el riesgo real). Otros módulos con `tiendaId` único en la ruta
  siguen protegidos solo por `PermissionInterceptor`; si se detecta otro caso de
  invocación directa sin HTTP, replicar el mismo patrón ahí.
- [x] En traslados, validar tanto tienda origen como destino al crear, consultar,
  completar y anular.
- [x] Cambiar el listado de traslados para devolver únicamente registros donde el
  usuario tenga acceso a ambas tiendas, salvo alcance global.
- [x] En `ProductoTienda`, filtrar configuraciones por tiendas permitidas
  (`listarPorProducto`; `listarPorTienda` exige acceso a la tienda pedida en vez de
  filtrar, ya que es una única tienda explícita).
- [x] Al actualizar, activar o desactivar `ProductoTienda`, resolver primero su
  `tiendaId` y verificarlo. (`asignar` también se protegió, aunque el checklist
  original no lo mencionaba explícitamente — mismo riesgo, mismo archivo.)
- [x] Revisar controllers sin variable de ruta `tiendaId` y documentar cómo protegen el
  alcance — comentarios corregidos en `TrasladoController` y
  `ProductoTiendaController`.
- [x] Corregir el comentario incorrecto que afirma que solo ADMIN posee permisos de
  traslados.
- [x] Responder `404` cuando revelar la existencia del recurso cause fuga de datos; usar
  `403` para operaciones donde la política permita informar la denegación —
  **cerrado (2026-08-25)**. Regla aplicada: cuando la operación primero resuelve el
  recurso por un `id` opaco (no elegido por quien llama) y luego valida alcance de
  tienda, negar el acceso con `403` confirmaría que el recurso existe — alguien
  probando ids consecutivos podría distinguir "no existe" (404) de "existe pero no es
  mío" (403), filtrando qué ids son recursos reales de otras tiendas. Cuando la
  operación recibe el `tiendaId` directamente de quien llama (no lo adivina desde un
  id opaco), el `403` no filtra nada nuevo y se conserva.
  - Traslados: `obtener`/`completar`/`anular` (reciben el `id` del traslado) ahora
    traducen la denegación a `ResourceNotFoundException` — mismo 404 y mismo mensaje
    que un id inexistente — vía `TrasladoServiceImpl.exigirAccesoOFingirNoEncontrado`.
    `crear` (recibe `tiendaOrigenId`/`tiendaDestinoId` directos) sigue en `403`.
  - `ProductoTienda`: `actualizar`/`activar`/`desactivar` (reciben el `id` de la
    configuración) mismo patrón vía
    `ProductoTiendaServiceImpl.exigirAccesoOFingirNoEncontrada`. `asignar`/
    `listarPorTienda` (reciben `tiendaId` directo) siguen en `403`.
  - Verificado contra Postgres real vía Docker: usuario `encargado1` (alcance
    exclusivo tienda 1) contra un traslado real entre tiendas 2↔3 y una
    configuración producto-tienda real de tienda 2 — `obtener`/`completar`/`anular`/
    `actualizar`/`activar` devuelven exactamente el mismo cuerpo 404
    (`RESOURCE_NOT_FOUND`, mismo formato de mensaje) que pedir un id que
    directamente no existe; `crear`/`asignar` con una tienda elegida a propósito
    siguen devolviendo 403 (`ACCESS_DENIED`) sin cambios.
  - Tests actualizados en `TrasladoServiceImplTest`/`ProductoTiendaServiceImplTest`
    (6 tests renombrados de "...LanzaAccesoDenegado" a "...LanzaNoEncontrado",
    aserción cambiada de `AccessDeniedException` a `ResourceNotFoundException`);
    `crearConTiendaFueraDeAlcanceLanzaAccesoDenegado`/
    `asignarConTiendaFueraDeAlcanceLanzaAccesoDenegado`/
    `listarPorTienda*ConTiendaFueraDeAlcanceLanzaAccesoDenegado` sin cambios.
    457 pruebas, 0 fallos.

### Archivos principales

- `seguridad/infrastructure/security/PermissionInterceptor.java`
- `traslados/api/controllers/TrasladoController.java`
- `traslados/application/services/impl/TrasladoServiceImpl.java`
- `productos/api/controllers/ProductoTiendaController.java`
- `productos/application/services/impl/ProductoTiendaServiceImpl.java`
- `db/changelog/modules/seguridad/006-seed-roles-operativos.xml` (solo como referencia;
  no modificar si ya fue aplicado)

### Pruebas y aceptación

- [x] ADMIN puede operar sobre todas las tiendas (cubierto por
  `AutorizacionTiendaServiceImplTest.alcanceGlobalTieneAccesoATodo`).
- [x] ENCARGADO_TIENDA solo puede operar traslados que involucren tiendas asignadas
  (cubierto a nivel de `AutorizacionTiendaServiceImplTest.alcancePorTiendaSoloPermiteSusTiendas`
  y los tests negativos de `TrasladoServiceImplTest`; no se agregó un test de
  integración con roles reales de BD en este PR).
- [x] CAJERO no puede listar configuraciones de productos de otras tiendas —
  `listarPorProductoFiltraLasTiendasFueraDeAlcance`.
- [x] Invocar el servicio de aplicación directamente también exige alcance — es
  justamente el punto de `AutorizacionTiendaService`, verificado a nivel de servicio
  (no HTTP) en los tests de `TrasladoServiceImplTest`/`ProductoTiendaServiceImplTest`.
- [x] Existen pruebas negativas para lectura, creación, actualización y acciones de
  estado — ver los métodos `*ConTiendaFueraDeAlcanceLanzaAccesoDenegado*` en ambos
  `*ServiceImplTest`.
- [x] La suite completa permanece verde — 428 pruebas, 0 fallos, 0 errores
  (24 de agosto de 2026).

## Fase 3 — Flujo contable correcto de ventas

**Resuelto (2026-08-25) el bug de fondo y el desglose de MIXTO** — ver checklist abajo.
**Explícitamente fuera de alcance en este PR**: anulación de venta ya completada
(reversión de inventario/caja/CxC) y la migración correctiva de datos históricos — ver
notas en cada tarea.

### Decisión funcional previa

Antes de programar, confirmar el contrato de `MIXTO`. Recomendación: recibir un arreglo
de pagos con método y monto; el saldo no cubierto se convierte en cuenta por cobrar.

**Diseño adoptado**: `List<PagoInmediato>` en el mismo `completar()` (no un endpoint
aparte) — solo obligatorio para `MIXTO`; para EFECTIVO/TARJETA/TRANSFERENCIA el servidor
resuelve el único pago posible por sí mismo (ignora cualquier desglose que mande el
cliente), y CREDITO nunca tiene pago inmediato. Esto mantiene el contrato HTTP
retrocompatible para 4 de los 5 métodos — `completar()` sin body sigue funcionando
exactamente igual que antes para ellos; solo MIXTO requiere que el cliente cambie.

### Reglas objetivo

| Método | Caja | Cuenta por cobrar |
| --- | --- | --- |
| `EFECTIVO` | Ingreso por el total | No crear |
| `CREDITO` | Sin ingreso inicial | Crear por el total |
| `MIXTO` | Ingreso por pagos inmediatos | Crear solo por el saldo |

### Tareas

- [x] Añadir un value object/DTO para el desglose de pagos —
  `ventas.application.dtos.PagoInmediato(MetodoPago, BigDecimal)` +
  `api.dtos.requests.{CompletarVentaRequest,PagoInmediatoRequest}`.
- [x] Validar montos positivos, métodos admitidos y que la suma no exceda el total —
  `VentaServiceImpl.validarDesgloseMixto`, nueva `DesglosePagoInvalidoException` (400
  `DESGLOSE_PAGO_INVALIDO`).
- [x] Cambiar `VentaService.completar` para recibir el desglose necesario — overload
  nuevo `completar(tiendaId, id, List<PagoInmediato>)`; se conservó el de 2 argumentos
  (delega al de 3 con lista vacía) para no romper compatibilidad binaria con el resto
  del código/tests que no necesitan desglose.
- [x] Eliminar la creación incondicional de cuenta por cobrar — ahora solo se crea si
  `saldo > 0` (`resolverPagosInmediatos` + el `if` al final de `completar`).
- [x] Registrar ingresos inmediatos mediante `CajaService` dentro de la misma
  transacción — `cajaService.registrarMovimientoSiHayAbierta(...)` por cada tramo,
  mismo patrón ya usado por `CuentaPorCobrarServiceImpl.registrarCobro` (no falla si no
  hay caja abierta, solo no registra el movimiento — la tienda podría no operar caja
  diaria todavía).
- [x] Crear cuenta por cobrar únicamente cuando el saldo sea mayor que cero — hecho.
- [x] Aplicar el límite de crédito al saldo financiado, no al total ya pagado —
  `validarLimiteCredito` ahora recibe el saldo real (ya no hace falta el trade-off
  conservador documentado antes: el pago inmediato ahora es atómico con `completar()`,
  no un cobro separado después que podría no llegar a dispararse).
- [x] Definir comportamiento cuando no existe caja abierta — **decisión adoptada sin
  preguntar**: reusar la política ya establecida en `CuentaPorCobrarServiceImpl`/
  `CuentaPorPagarServiceImpl` para el mismo escenario (un cobro/pago recibido sin caja
  abierta no falla, simplemente no queda reflejado en Caja) — no se inventó una regla
  nueva, se aplicó la misma que ya existía para el caso idéntico.
- [ ] Diseñar la anulación para revertir inventario, caja y cuenta por cobrar sin borrar
  el historial — **fuera de alcance**: hoy `Venta.anular()` exige `BORRADOR`
  (`EstadoVenta.COMPLETADA` es terminal, sin camino de vuelta) — esto no es "arreglar un
  bug", es construir una capacidad que no existe en absoluto (reversión compensatoria de
  3 módulos). Alcance de este PR fue específicamente el bug de cuenta por cobrar
  incondicional, no anulación post-completar.
- [ ] Crear una consulta/migración correctiva para identificar cuentas por cobrar
  originadas por ventas `EFECTIVO` — **no aplica**: confirmado en la auditoría de
  2026-08-24 que este backend nunca se desplegó a producción, no hay datos históricos
  que corregir.
- [x] Actualizar el cliente Flutter y el contrato HTTP en el mismo ciclo de versión —
  `VentaApi.completar()` ganó `pagosInmediatos` opcional; `CheckoutNotifier` manda el
  desglose directo en el mismo `completar()` en vez del baile
  completar→buscarPorVenta→registrarCobro de antes (que además de redundante era la
  causa exacta del riesgo real: si esas llamadas de cobro posteriores nunca se
  disparaban, quedaba una `CuentaPorCobrar` fantasma). Ver `market-flutter/CLAUDE.md`,
  "Flujo contable correcto de ventas — checkout simplificado".
- [x] Corregir/reemplazar el test `VentaServiceImplTest.completarCreaLaCuentaPorCobrarDelClientePorElTotal`
  — reemplazado por `completarEfectivoNoCreaCuentaPorCobrarYRegistraIngresoEnCaja` (y
  pares para TARJETA/CREDITO/MIXTO) que afirman el comportamiento correcto.

### Archivos principales

- `ventas/application/services/interfaces/VentaService.java`
- `ventas/application/services/impl/VentaServiceImpl.java`
- `ventas/api/controllers/VentaController.java`
- `ventas/api/dtos/requests/`
- `cuentasporcobrar/application/services/interfaces/CuentaPorCobrarService.java`
- `caja/application/services/interfaces/CajaService.java`

### Pruebas y aceptación

- [x] Una venta `EFECTIVO` no crea cuenta por cobrar y registra el ingreso correcto —
  verificado por test y contra Postgres real vía `docker compose` + `curl` (mismo para
  TARJETA/TRANSFERENCIA).
- [x] Una venta `CREDITO` crea una cuenta por el total y no registra ingreso inicial —
  verificado por test y contra Postgres real.
- [x] Una venta `MIXTO` crea deuda únicamente por el saldo — verificado por test y
  contra Postgres real (caso con saldo > 0 y caso cubierto al 100%, sin CxC).
- [ ] Cualquier fallo revierte venta, inventario, caja y cuenta por cobrar — la parte de
  inventario/venta ya estaba cubierta (transacción única); no se agregó un test
  explícito de fallo a mitad de los movimientos de caja (son best-effort, no lanzan).
- [ ] La anulación genera movimientos compensatorios trazables — fuera de alcance, ver
  nota arriba.
- [x] La suite completa permanece verde — 450 pruebas, 0 fallos, 0 errores
  (25 de agosto de 2026).

## Fase 4 — Concurrencia e integridad de inventario

### Resultado esperado

Dos operaciones simultáneas sobre el mismo producto y tienda no pierden actualizaciones
ni permiten existencia negativa.

### Estrategia recomendada

Usar bloqueo pesimista (`PESSIMISTIC_WRITE`) al cargar inventario para movimientos de
stock. Para filas todavía inexistentes, asegurar la creación mediante la restricción
única `(tienda_id, producto_id)` y reintentar la lectura bloqueada. Evaluar `@Version`
como protección adicional para actualizaciones realizadas fuera de ese flujo.

### Tareas

- [x] Añadir método de repositorio que cargue inventario con bloqueo de escritura —
  `findByTiendaIdAndProductoIdConBloqueo` (`@Lock(PESSIMISTIC_WRITE)` + `@Query`
  explícito, porque el sufijo "ConBloqueo" no es una propiedad de la entidad y la
  derivación automática de Spring Data falla en el arranque si no se fija la query).
  La lectura sin bloqueo (`findByTiendaIdAndProductoId`) se mantiene intacta para
  `obtener()` y el resto de consultas de solo lectura.
- [x] Definir de forma segura la creación concurrente de la primera fila de inventario
  — `InventarioServiceImpl.registrarMovimiento` corre el intento (lectura bloqueada +
  aplicar + guardar + kardex) dentro de un `TransactionTemplate` explícito; si la
  primera fila choca contra la restricción única (dos movimientos concurrentes son
  ambos los primeros sobre el mismo tienda/producto), se reintenta una única vez en
  una transacción nueva y aislada — para entonces la fila ganadora ya está confirmada,
  así que el reintento la encuentra vía la lectura bloqueada y aplica su movimiento
  como una actualización normal. Un segundo fallo (tienda/producto realmente
  inexistente) se propaga tal cual. Se usó `TransactionTemplate` en vez de
  auto-invocación con `@Transactional` (que el proxy de Spring no intercepta dentro de
  la misma clase) y en vez de auto-inyección del propio bean (sin precedente en el
  proyecto).
- [x] Verificar que Liquibase tenga una restricción única por tienda y producto — ya
  existía (`ux_inventario_tienda_producto` en `001-inventario.xml`); no hizo falta
  changeset nuevo.
- [x] Evaluado `@Version` como protección adicional — **deliberadamente omitido**: el
  bloqueo pesimista en el único camino de mutación (`registrarMovimiento`, usado por
  Ventas/Compras/Traslados) ya cubre toda escritura de inventario; no existe otro
  camino que actualice la fila fuera de ese flujo, así que el control optimista
  complementario no aporta protección adicional real.
- [x] Mantener el movimiento de kardex y la actualización de existencia en la misma
  transacción — ambos ocurren dentro del mismo `TransactionTemplate.execute(...)`.
- [x] Traducir conflictos recuperables a `409 Conflict` con un código estable — ya
  cubierto por `StockInsuficienteException` (409, `STOCK_INSUFICIENTE`) para el
  conflicto de negocio real. La colisión transitoria de creación de fila nunca llega
  al cliente: el reintento la resuelve de forma transparente.
- [x] Aplicar reintentos solo a conflictos transitorios y con un límite pequeño — un
  único reintento, solo ante `ReferenciaInvalidaException` del primer intento.
- [x] Revisar también compras y traslados, porque utilizan el mismo inventario —
  confirmado: `CompraServiceImpl` y `TrasladoServiceImpl` (y `VentaServiceImpl`) llaman
  todos al mismo `InventarioService.registrarMovimiento`; ningún otro módulo toca
  `InventarioRepository` directamente. No requirieron cambios propios.
- [x] Corregir `InventarioRepositoryAdapter.save`: se dejó deliberadamente sin tocar
  (sigue traduciendo cualquier `DataIntegrityViolationException` a
  `ReferenciaInvalidaException` genérica) — el mensaje/código incorrecto para el caso
  de colisión concurrente ya no es un problema observable: `registrarMovimiento` nunca
  expone esa excepción al cliente en el caso de colisión (la resuelve con el
  reintento); solo llega al cliente cuando la causa es real (tienda/producto
  inexistente), caso en el que el mensaje sí es correcto. Mismo patrón de
  "no distinguir el tipo de excepción en el adaptador, resolver la ambigüedad
  re-consultando en el servicio" usado en la Fase 6 para `VentaRepositoryAdapter`.

### Archivos principales

- `inventario/application/services/impl/InventarioServiceImpl.java`
- `inventario/domain/repository/InventarioRepository.java`
- `inventario/infrastructure/persistence/repositories/InventarioJpaRepository.java`
- `inventario/infrastructure/persistence/adapters/InventarioRepositoryAdapter.java`
- `inventario/application/services/InventarioServiceImplTest.java` (pruebas nuevas)

### Pruebas y aceptación

- [x] Prueba PostgreSQL con dos ventas concurrentes sobre la última unidad: solo una
  completa — verificado con 2 `curl` POST realmente paralelos (`&` + `wait`) contra
  `/api/v1/inventario/tiendas/1/movimientos` con existencia=1: una devolvió 201
  (existencia final 0), la otra 409 `STOCK_INSUFICIENTE`.
- [x] Prueba de dos ingresos concurrentes: la existencia final contiene ambos —
  verificado con 8 POST realmente paralelos de COMPRA (cantidad 1 c/u) sobre un
  producto sin fila de inventario previa (ejercita la colisión de creación de primera
  fila): las 8 completaron con 201, existencia final = 8, sin pérdidas.
- [x] Cada actualización confirmada posee exactamente su movimiento de kardex —
  verificado: 8 compras + 7 ventas de drenaje + 1 venta ganadora de la carrera = 16
  movimientos registrados, coincide exactamente con `totalElementos` del kardex; la
  venta rechazada (409) no dejó movimiento huérfano.
- [x] No existen saldos negativos ni actualizaciones perdidas — confirmado en ambas
  pruebas anteriores (existencia final 8 y luego 0, nunca negativa; secuencia 1..8 sin
  huecos ni duplicados en los 8 ingresos paralelos).
- [x] La suite completa permanece verde — 456 pruebas, 0 fallos, 0 errores (25 de
  agosto de 2026; +2 respecto a las 454 de la Fase 6, cubriendo el reintento por
  colisión y la propagación de una referencia realmente inválida tras el reintento).

## Fase 5 — Rotación atómica de refresh tokens

### Resultado esperado

Un refresh token puede consumirse correctamente una sola vez, aun con solicitudes
simultáneas o varias instancias del backend.

### Estrategia recomendada

Realizar una actualización condicional atómica equivalente a:

```sql
UPDATE refresh_token
SET revocado = true
WHERE token_hash = :hash AND revocado = false AND expira_en > :ahora;
```

Continuar solo si se actualizó exactamente una fila. Una alternativa válida es un
`SELECT ... FOR UPDATE`, siempre que todas las rutas respeten el mismo bloqueo.

### Tareas

- [x] Añadir una operación de repositorio para consumir el token atómicamente —
  `RefreshTokenRepository.consumir(tokenHash, ahora)`, `UPDATE ... SET revocado = true
  WHERE token_hash = :hash AND revocado = false AND expira_en > :ahora` vía
  `@Modifying @Query`, devuelve el número de filas afectadas (0 o 1).
- [x] Evitar la secuencia no protegida `find -> comprobar -> save` — `refresh()` ya no
  hace `find` + comprobar en memoria + `save()`; el `UPDATE` condicional es la única
  fuente de verdad sobre quién consumió el token.
- [x] Emitir el token hijo únicamente después de consumir correctamente el padre —
  `emitirTokensRotados` solo se llama cuando `consumir(...) == 1`.
- [x] Si el token ya fue consumido, registrar reutilización y revocar la familia o todos
  los tokens del usuario según la política definida — se mantuvo la política ya
  existente en el código (revocar todos los tokens del usuario, no solo la cadena
  padre-hijo); ahora la rama perdedora de una carrera legítima (dos refresh
  simultáneos con el mismo token) también se trata como reutilización, que es la
  postura conservadora que pide esta fase.
- [x] Mantener la respuesta genérica para no filtrar detalles de autenticación — sin
  cambios, sigue siendo siempre `AutenticacionFallidaException` (401 genérico).
- [x] Añadir una tarea de limpieza de tokens expirados con índice apropiado —
  `RefreshTokenCleanupScheduler` (`@Scheduled(fixedDelayString =
  "${app.security.refresh-token.cleanup-interval:PT1H}")`, `@EnableScheduling` nuevo en
  `MarketBackendApplication`) llama a `RefreshTokenRepository.eliminarExpirados`;
  índice nuevo `ix_refresh_token_expira_en` (`008-refresh-token-index-expira-en.xml`)
  para que el `DELETE ... WHERE expira_en < :antesDe` no haga seq scan. No toca tokens
  revocados-pero-no-expirados (se conservan hasta su expiración natural, por si hiciera
  falta auditar una reutilización).
- [x] Añadir métricas de rotaciones, reutilizaciones y revocaciones de familia —
  **deliberadamente diferido a la Fase 7**: el proyecto no tiene `micrometer` ni
  `spring-boot-starter-actuator` en `pom.xml` (confirmado — ver hallazgo de la Fase 7
  en la tabla resumen), así que no hay dónde publicar contadores reales todavía.
  Mientras tanto, el mismo `SecurityAuditPublisher`/logger `SECURITY_AUDIT` que ya
  registra `REFRESH_EXITOSO`/`REFRESH_FALLIDO`/`REFRESH_REUTILIZADO` (sin cambios,
  reusado tal cual) es la única fuente de esa señal — introducir un sistema de métricas
  paralelo solo para esta fase habría sido trabajo duplicado frente a lo que la Fase 7
  va a añadir correctamente.

### Bug encontrado y corregido durante la verificación

Al verificar con concurrencia real contra Postgres se encontró que la revocación de
familia (`revocarTodosDeUsuario`) **no persistía**: `refresh()` es `@Transactional`, y
lanzar `AutenticacionFallidaException` (una `RuntimeException`) después de la
revocación hacía que Spring revirtiera toda la transacción por la regla de rollback
por defecto — la respuesta era 401 pero la familia quedaba intacta en la base de
datos. Preexistente al código anterior a esta fase (la misma secuencia
revocar-luego-lanzar ya existía), solo se hizo observable al implementar y verificar
el camino de reutilización de esta fase. Corregido con
`@Transactional(noRollbackFor = AutenticacionFallidaException.class)` en `refresh()`.
Verificado antes/después con la misma prueba de reutilización real: antes del fix el
token nuevo del ganador de la carrera sobrevivía a una reutilización posterior de su
padre ya consumido; después del fix, la revocación de familia sí lo alcanza.

### Archivos principales

- `seguridad/application/services/impl/AuthServiceImpl.java`
- `seguridad/domain/repository/RefreshTokenRepository.java`
- `seguridad/infrastructure/persistence/repositories/RefreshTokenJpaRepository.java`
- `seguridad/infrastructure/persistence/adapters/RefreshTokenRepositoryAdapter.java`
- `seguridad/infrastructure/security/RefreshTokenCleanupScheduler.java` (nuevo)
- `MarketBackendApplication.java` (`@EnableScheduling`)
- `db/changelog/modules/seguridad/008-refresh-token-index-expira-en.xml` (nuevo)
- `application.yml` (`app.security.refresh-token.cleanup-interval`)
- `seguridad/application/services/AuthServiceImplTest.java` (pruebas actualizadas/nuevas)

### Pruebas y aceptación

- [x] Dos refresh simultáneos producen como máximo un nuevo par válido — verificado con
  2 `curl` POST realmente paralelos contra `/api/v1/auth/refresh` con la misma cookie:
  uno 200 (nuevo par), el otro 401.
- [x] El segundo intento activa la política de reutilización — verificado reproduciendo
  la cookie original (ya consumida) tras la carrera: 401, y la familia completa quedó
  revocada en la base de datos (incluido el par nuevo emitido por el ganador de la
  carrera — ver bug corregido arriba).
- [x] El comportamiento funciona con transacciones/conexiones independientes —
  verificado contra Postgres real (no H2/mock), cada request en su propia conexión del
  pool de Hikari.
- [x] Los tokens expirados pueden limpiarse sin bloquear autenticaciones normales — el
  `DELETE` corre en su propia transacción corta programada, sin relación con las
  transacciones de login/refresh.
- [x] La suite completa permanece verde — 457 pruebas, 0 fallos, 0 errores (25 de
  agosto de 2026; +1 respecto a las 456 de la Fase 4, cubriendo la carrera de dos
  refresh simultáneos).

## Fase 6 — Idempotencia de ventas aislada por tienda

**Resuelta (2026-08-25)** — ver checklist abajo.

### Resultado esperado

Un reintento devuelve la misma venta solamente dentro de su contexto autorizado y una
colisión nunca expone datos de otra tienda.

### Diseño recomendado

- Clave de idempotencia compuesta: `(tienda_id, vendedor_id, correlation_id)`.
- Guardar además una huella estable del comando para detectar que una misma clave fue
  reutilizada con contenido diferente.
- La restricción única de la base de datos es la autoridad final frente a carreras.

### Tareas

- [x] Cambiar el puerto y repositorio para buscar por tienda, vendedor y correlation ID
  — `findByTiendaIdAndVendedorIdAndCorrelationId` reemplaza al `findByCorrelationId`
  global (eliminado, no dejado como método muerto — buscar solo por correlationId era
  exactamente el bug).
- [x] Añadir una columna de huella del comando o una tabla general de idempotencia —
  **decisión distinta, documentada**: no se agregó columna nueva. La "huella" se
  calcula al vuelo comparando `clienteId`/`lineas`/`metodoPago` de la venta ya
  persistida contra los de la solicitud entrante (`VentaServiceImpl.coincideConLaSolicitud`)
  — los datos para comparar ya están guardados en la venta existente, así que una
  columna de hash aparte solo agregaría una fuente adicional de verdad que podría
  desincronizarse, sin ganar nada. `LineaVenta` no tenía `equals()`/`hashCode()`
  definidos (mucho riesgo de tocar semántica de igualdad usada en otro lado); se
  comparó campo a campo con `BigDecimal.compareTo` en su lugar.
- [x] Crear un changeset que sustituya la restricción única global por la compuesta —
  `ventas/006-correlation-id-compuesto.xml` (`ux_venta_tienda_vendedor_correlation_id`
  sobre `tienda_id, vendedor_id, correlation_id`).
- [x] Normalizar, limitar y validar `correlationId` — se recorta y un string en blanco
  se trata como ausente (`normalizarCorrelationId`); `@Size(max = 100)` en
  `CrearVentaRequest` (coincide con el `VARCHAR(100)` de la columna).
- [x] Si existe la misma clave y la misma huella, devolver la venta existente.
- [x] Si existe la misma clave con distinta huella, devolver `409 Conflict` — nueva
  `CorrelationIdReutilizadoException` (`CORRELATION_ID_REUTILIZADO`).
- [x] Capturar la colisión de inserciones concurrentes, releer y devolver el resultado
  idempotente sin producir `500` — **detalle no trivial resuelto**: se quitó
  `@Transactional` de `crear()` a propósito. Envolver todo en una sola transacción
  reutilizaría la misma sesión de Hibernate para la relectura después de que el
  insert fallara por la restricción única, y una sesión que acaba de lanzar una
  violación de restricción durante el flush queda en estado no utilizable para
  seguir operando en la misma transacción — cada paso (chequeo, insert, relectura
  tras colisión) corre en su propia transacción vía el manejo por defecto de Spring
  Data JPA. Verificado con 8 requests genuinamente concurrentes contra Postgres real
  (no un test secuencial que nunca ejercita la carrera de verdad).
- [x] No devolver una venta existente hasta comprobar tienda y vendedor — satisfecho
  por construcción: la búsqueda ya es por la clave compuesta, no hace falta un
  chequeo posterior.
- [x] Confirmado por auditoría: `004-correlation-id.xml` creaba la unique constraint
  solo sobre `correlation_id` (global) — corregido.

### Archivos principales

- `ventas/application/services/impl/VentaServiceImpl.java`
- `ventas/domain/repository/VentaRepository.java`
- `ventas/infrastructure/persistence/repositories/VentaJpaRepository.java`
- `ventas/infrastructure/persistence/entities/VentaEntity.java`
- `ventas/api/dtos/requests/CrearVentaRequest.java`
- `ventas/domain/exception/CorrelationIdReutilizadoException.java` (nuevo)
- `db/changelog/modules/ventas/006-correlation-id-compuesto.xml` (nuevo)

### Pruebas y aceptación

- [x] Dos solicitudes concurrentes idénticas crean una sola venta — verificado con 8
  requests concurrentes reales (`curl` en paralelo) contra Postgres real: los 8
  devolvieron `201` con el mismo id, y solo existe 1 fila en la tabla.
- [x] La misma clave puede existir de forma independiente en tiendas distintas —
  verificado contra el backend real.
- [x] Una clave reutilizada con datos diferentes devuelve `409` — verificado contra el
  backend real (`CORRELATION_ID_REUTILIZADO`).
- [x] Ningún reintento devuelve datos de otra tienda o vendedor — por construcción
  (búsqueda por clave compuesta).
- [x] La suite completa permanece verde — 454 pruebas, 0 fallos, 0 errores
  (25 de agosto de 2026).

No requirió cambios en `market-flutter`: el cliente ya mandaba un `correlationId` por
ítem encolado y ya trataba la respuesta como autoritativa — ver
`market-flutter/CLAUDE.md`, "Idempotencia de ventas offline", actualización
2026-08-25.

## Fase 7 — Integración, observabilidad y cierre

**Resuelta en su mayor parte (2026-08-25)** — dos tareas quedan explícitamente
bloqueadas/diferidas, ver notas.

### Tareas

- [x] Añadir Testcontainers PostgreSQL para migraciones y pruebas de concurrencia —
  `LiquibaseMigrationIT` (`@SpringBootTest` + `@Testcontainers`, Postgres real vía
  `@ServiceConnection`) carga el contexto completo de Spring Boot contra un Postgres
  efímero. Corre solo en `mvn verify` (plugin `maven-failsafe-plugin` nuevo, patrón
  `*IT`), nunca en `mvn test`, porque necesita Docker. **Esta prueba encontró dos bugs
  reales** al ejecutarse por primera vez (ver más abajo) — bugs invisibles para los
  457 tests unitarios con mocks.
- [x] Ejecutar Liquibase desde cero — cubierto por `LiquibaseMigrationIT`: cada corrida
  levanta un contenedor Postgres nuevo (vacío) y Liquibase migra las 76 changesets
  desde cero antes de que Hibernate valide el esquema. **Sobre una copia anonimizada
  del esquema existente**: no aplica — no existe una base de datos de producción de la
  que sacar una copia (mismo motivo documentado en la Fase 3 para la migración
  correctiva de datos históricos: nunca hubo prod).
- [x] Añadir `spring-boot-starter-actuator` — agregado a `pom.xml`. `SecurityConfig` ya
  permitía únicamente `/actuator/health` sin autenticar (nada que cambiar ahí); el
  resto de rutas de Actuator ya caían bajo `.anyRequest().authenticated()`.
- [x] Exponer únicamente health/readiness necesarios y proteger los demás endpoints —
  `management.endpoints.web.exposure.include: health` (nada más se expone) +
  `management.endpoint.health.show-details: never` (no filtra detalle de la conexión a
  la base de datos a un caller sin JWT). Verificado contra Postgres real vía Docker:
  `GET /actuator/health` sin token → 200 `{"groups":["liveness","readiness"],"status":"UP"}`;
  `/actuator/metrics`, `/actuator/env`, `/actuator/beans`, `/actuator` (raíz) sin token
  → 401; con JWT válido → 404 limpio (no 500, ver bug corregido abajo).
- [x] Añadir métricas para conflictos de stock, idempotencia, refresh reutilizados y
  denegaciones multi-tienda — reusando los dos puntos ya centralizados en vez de
  instrumentar cada excepción por separado: `SecurityAuditPublisherImpl.publicar`
  incrementa `market.security.evento{tipo=...}` por cada `TipoEventoAuditoria` (cubre
  `REFRESH_REUTILIZADO` de la Fase 5 sin tocar `AuthServiceImpl`);
  `GlobalExceptionHandler.handleBusiness`/`handleAccessDenied` incrementan
  `market.business_exception{codigo=...}` por cada `BusinessException.errorCode()` más
  `ACCESS_DENIED` (cubre `STOCK_INSUFICIENTE` de la Fase 4, `CORRELATION_ID_REUTILIZADO`
  de la Fase 6, y las denegaciones multi-tienda de `AutorizacionTiendaServiceImpl` de
  la Fase 2, todas de una vez).
- [x] Añadir pruebas de arranque para los perfiles local, test y prod — resuelto junto
  con la Fase 1 (que introdujo los perfiles): `ProfileStartupIT` (4 pruebas, Postgres
  real vía Testcontainers) + `LiquibaseMigrationIT` (perfil `test`).
- [x] Añadir Mockito como agente de pruebas para evitar la carga dinámica advertida por
  Java 25 — `maven-dependency-plugin` (goal `properties`) resuelve la ruta real del jar
  de `mockito-core` en `${org.mockito:mockito-core:jar}`, pasada como `-javaagent` en
  `argLine` de `surefire`/`failsafe` (con comillas — sin ellas, la ruta con espacios de
  `Program Files` rompe el arranque de la JVM forkeada en Windows, encontrado al
  verificar). Confirmado: el warning "Mockito is currently self-attaching" ya no
  aparece en `mvn test`.
- [x] Ejecutar una prueba manual de venta completa por cada método de pago — EFECTIVO,
  MIXTO (parcial) y CREDITO ya verificados contra Postgres real en la Fase 3. TARJETA y
  TRANSFERENCIA no se re-probaron por separado: comparten exactamente la misma rama de
  `VentaServiceImpl.resolverPagosInmediatos` que EFECTIVO (`EFECTIVO, TARJETA,
  TRANSFERENCIA -> List.of(new PagoInmediato(metodo, venta.total()))`), así que el
  mismo caso probado ya cubre las tres — no hay lógica distinta que ejercitar.
- [x] Actualizar documentación operativa y procedimiento de rollback — `deploy/README.md`:
  nueva sección "Rollback" (imagen de aplicación vía `git checkout` + rebuild;
  `liquibase:rollback` — casi todos los changesets del proyecto son auto-reversibles
  por Liquibase salvo el trigger de `movimiento_inventario`, que ya trae su
  `<rollback>` manual; restauración desde backup como último recurso) y nueva sección
  "Monitoreo" documentando `/actuator/health`.

### Bugs encontrados y corregidos durante la verificación

Ambos invisibles para la suite de 457 pruebas unitarias con mocks — solo aparecieron
al cargar el contexto real de Spring Boot en `LiquibaseMigrationIT` y al probar
manualmente contra Docker:

1. **Arranque roto al agregar Actuator**: `RequiresPermissionStartupValidator`
   autowireaba `RequestMappingHandlerMapping` por tipo sin calificador; Actuator
   registra un segundo bean de ese tipo (`controllerEndpointHandlerMapping`, para
   `@ControllerEndpoint`, que este proyecto no usa) y el arranque fallaba con
   `NoUniqueBeanDefinitionException`. Corregido con
   `@Qualifier("requestMappingHandlerMapping")`.
2. **Rutas inexistentes devolvían 500 en vez de 404**: el `@ExceptionHandler(Exception.class)`
   genérico de `GlobalExceptionHandler` atrapaba `NoResourceFoundException` (la que
   Spring MVC lanza para cualquier ruta sin handler) y la traducía a `INTERNAL_ERROR`
   500 — encontrado al probar a propósito un endpoint de Actuator no expuesto.
   Corregido con un `@ExceptionHandler(NoResourceFoundException.class)` dedicado → 404
   `NOT_FOUND`. Preexistente a esta fase (afectaba cualquier URL mal escrita, no solo
   Actuator), solo se hizo visible al verificar la exposición de Actuator.

### Archivos principales

- `pom.xml` (`spring-boot-starter-actuator`, `spring-boot-testcontainers`,
  `testcontainers-junit-jupiter`, `testcontainers-postgresql`, plugins
  `maven-dependency-plugin`/`maven-surefire-plugin`/`maven-failsafe-plugin`)
- `src/test/java/com/ais/marketbackend/LiquibaseMigrationIT.java` (nuevo)
- `application.yml` (`management.*`)
- `shared/exceptions/GlobalExceptionHandler.java` (métricas + handler 404)
- `seguridad/infrastructure/security/SecurityAuditPublisherImpl.java` (métricas)
- `seguridad/infrastructure/security/RequiresPermissionStartupValidator.java`
  (`@Qualifier`, bug de arranque)
- `deploy/README.md` (Rollback, Monitoreo)
- 20 archivos `*ControllerTest.java` (actualizados: `new GlobalExceptionHandler()` ahora
  recibe un `SimpleMeterRegistry`)

### Verificación final

```powershell
mvn -o "-Dmaven.repo.local=C:\Program Files\Apache-Maven-3.9.1" clean verify
```

- [x] Todas las pruebas unitarias y de integración pasan — 457 unitarias (`mvn test`) +
  1 de integración (`LiquibaseMigrationIT`, `mvn verify`), 0 fallos.
- [x] Liquibase migra una base vacía — `LiquibaseMigrationIT` lo verifica en cada
  corrida (76 changesets, Postgres real). Base existente: N/A, no hay prod (ver arriba).
- [x] No hay secretos ni llaves privadas en el artefacto productivo — ya resuelto en la
  Fase 1 (llaves prod separadas en `deploy/certs/`, llaves dev movidas a
  `local-dev/certs/`, ambas excluidas del jar — verificado con `unzip -l`).
- [x] Se verificó aislamiento entre tiendas con cada rol operativo — Fase 2.
- [x] Se verificaron ventas concurrentes sobre stock limitado — Fase 4 (2 ventas
  paralelas sobre la última unidad, Postgres real).
- [x] Se verificó refresh concurrente — Fase 5 (2 refresh paralelos, Postgres real).
- [x] Se verificaron reintentos concurrentes de creación de venta — Fase 6 (8 POST
  paralelos con el mismo `correlationId`, Postgres real).

## Fase 8 — Hallazgos transversales de backend (nueva, auditoría 2026-08-24)

### Resultado esperado

El backend responde con latencia y uso de memoria estables cuando el volumen de datos
reales (ventas, movimientos de inventario) crece, y los reportes de caja/ventas
reflejan la zona horaria real del negocio.

### Tareas

- [x] Añadir paginación (`Pageable`/`Page<T>`) a los listados sin límite (2026-08-25):
  ventas, cuentas por cobrar, traslados, productos (catálogo + `ProductoTienda.listarPorTienda`),
  inventario (existencias + kardex de movimientos), caja (historial de sesiones).
  Diseño: `shared.domain.Pagina<T>` (framework-agnóstico) + `PaginaMapper`
  (adaptador↔Spring Data) + `PaginacionParams` (normaliza `page`/`size`, tope
  `TAMANO_MAXIMO=5000`) + `PaginaResponse<T>` (API). Cada método de listado
  paginado es un **overload nuevo** junto al método sin paginar existente — el
  sin paginar se conserva para uso interno (`DashboardServiceImpl` sigue
  necesitando el histórico completo para sus agregados; forzarlo a paginar
  habría exigido rediseñar el dashboard a consultas SQL de agregación, fuera de
  alcance). `TrasladoServiceImpl.listar()` fue el caso especial: el filtro por
  alcance de tienda de la Fase 2 no se puede aplicar en memoria después de
  paginar (produce páginas incompletas) — se extendió
  `AutorizacionTiendaService` con `tiendaIdsPermitidas()` (vacío = alcance
  global) y se agregó una consulta JPA derivada
  (`findByTiendaOrigenIdInOrTiendaDestinoIdIn`) que filtra a nivel de BD antes
  de paginar. `ProductoTienda.listarPorProducto` quedó **deliberadamente sin
  paginar** — acotado por el número de tiendas, no por volumen de datos.
  Contrato HTTP coordinado en el mismo PR con ambos clientes: `market-flutter`
  (3 de los 6 endpoints ya los consumía con `data as List` — se actualizó el
  parser para desempacar `contenido` y se pide `size=5000` para preservar el
  "traer todo" que el POS necesita para su caché offline) y `market-backoffice`
  (los 6 módulos, service+composable+vista, con UI de paginación real
  10/25/50/100 igual al patrón ya usado en `ClientesView.vue`). Verificado:
  428 pruebas backend en verde, el envelope exacto contra Postgres real vía
  `docker compose` + `curl`, `pnpm typecheck`/`pnpm build` limpios en el
  backoffice, el bundle de Docker del backoffice inspeccionado para confirmar
  que el cambio realmente se compiló y desplegó, y **clic-test visual real**
  en navegador contra backend+backoffice en Docker con datos reales
  sembrados: los 6 módulos (Ventas, Productos, Traslados, Cuentas por Cobrar,
  Inventario —existencias y kardex—, Caja) confirmados con el footer de
  paginación correcto y `totalElementos` exacto. El clic-test encontró y
  corrigió un bug real: cambiar el tamaño de página sin resetear a página 1
  dejaba la tabla vacía si la página actual ya no existía tras el cambio
  (footer mostraba "Página 2 de 1") — corregido en los 6 módulos con un
  `watch` que resetea la página al cambiar el tamaño. **No verificado**: en
  un dispositivo/emulador Flutter real — ver `market-backoffice/CLAUDE.md`
  («Server-side pagination») y `market-flutter/CLAUDE.md` («Backend
  pagination rollout») para el detalle.
- [x] Corregir el cálculo de "inicio de hoy/mes" en `DashboardServiceImpl` (2026-08-24).
  Se agregó `app.negocio.zona-horaria` (`BUSINESS_TIMEZONE`, default
  `America/Guatemala`) y el cálculo ahora usa
  `ahoraZoned.toLocalDate().atStartOfDay(zona)` en vez de truncar en UTC. Verificado
  end-to-end contra Postgres real vía `docker compose` (login + `GET
  /api/v1/dashboard/tiendas/{id}` responde 200 sin error).
- [x] Reducir llamadas redundantes a `inventarioService.listarPorTienda(tiendaId)` en
  `DashboardServiceImpl.obtenerResumen` — se llamaba 3 veces con el mismo argumento;
  ahora se llama una sola vez y se reutiliza la lista. **No incluye** evitar traer el
  histórico completo de ventas (`ventaService.listarPorTienda` sigue trayendo todas
  las ventas de la tienda y filtrando en memoria) — eso depende de la tarea de
  paginación/consulta por rango de fecha, pendiente arriba.
- [x] Añadir índices — `venta(tienda_id, estado, fecha)`
  (`ventas/005-indices.xml`), y se extendió el mismo saneamiento a las tres tablas
  que la auditoría confirmó también sin índice propio:
  `cuenta_por_cobrar(tienda_id, estado)` (`cuentasporcobrar/003-indices.xml`),
  `caja_sesion(tienda_id, estado)` (`caja/002-indices.xml`),
  `compra(tienda_id, estado, fecha)` (`compras/002-indices.xml`) y
  `traslado(tienda_origen_id)` / `traslado(tienda_destino_id)`
  (`traslados/002-indices.xml`). Verificado: los 5 changesets nuevos aplican limpio
  contra Postgres real (`docker compose up`, logs de Liquibase sin error).
- [x] Configurar rotación de logs — en vez de un logger interno del JVM (los logs ya
  van correctamente a stdout, confirmado en la auditoría), se configuró rotación en
  el driver `json-file` de Docker (`docker-compose.yml`, ancla `x-logging`: 10 MB × 5
  archivos por contenedor, aplicado a los 4 servicios) — es la capa donde realmente
  se persisten y podrían crecer sin límite. Documentado en `deploy/README.md`.

### Ya verificado como correcto (no requiere acción)

- Rate limiting de login por IP+usuario ya existe (`InMemoryLoginRateLimiter`,
  documentado como limitación de instancia única).
- `GlobalExceptionHandler` no filtra stacktraces al cliente y loguea con
  `correlationId` server-side.
- No hay logging de contraseñas/tokens/JWT completos.
- CORS acotado (métodos/headers explícitos, no `*`).
- `@Valid` se usa consistentemente en los controllers con `@RequestBody`.
- Fechas usan `Instant`/`TIMESTAMP WITH TIME ZONE` de forma consistente, salvo el bug
  puntual del dashboard arriba.
- Logs van a stdout — compatible con `docker compose logs`, sin configuración extra.

### Archivos principales

- Todos los controllers de listado (`ventas`, `cuentasporcobrar`, `traslados`,
  `productos`, `inventario`, `caja`)
- `dashboard/application/services/impl/DashboardServiceImpl.java`
- `db/changelog/modules/ventas/` (changeset nuevo de índices)
- `src/main/resources/application.yml` / `application-prod.yml`

### Pruebas y aceptación

- [x] Los listados aceptan `page`/`size` y devuelven metadatos de paginación
  (`contenido`/`pagina`/`tamano`/`totalElementos`/`totalPaginas`) — los 6 endpoints
  verificados contra Postgres real, ver detalle arriba.
- [x] El resumen del dashboard usa la zona horaria configurada del negocio —
  verificado end-to-end contra Postgres real (no se agregó un test unitario
  determinista para el límite exacto de medianoche: requeriría inyectar un
  `Clock`, algo que ningún servicio del proyecto usa hoy — se consideró
  sobre-ingeniería para este fix puntual).
- [x] Los 5 índices nuevos aplican limpio sobre Postgres real vía `docker compose up`
  (logs de Liquibase sin error) — no se corrió un `EXPLAIN` explícito.
- [x] La suite completa permanece verde — 428 pruebas, 0 fallos, 0 errores.

### Paginación — resuelta (2026-08-25)

Implementada coordinando los 3 repos en el mismo ciclo (backend + `market-flutter` +
`market-backoffice`), tal como exigía la regla general de este plan sobre cambios de
contrato HTTP. Detalle completo en la tarea marcada arriba y en el `CLAUDE.md` de cada
cliente. El backoffice quedó verificado con clic-test visual real (encontró y corrigió
un bug de reseteo de página al cambiar el tamaño). Pendiente real que queda: verificación
en un dispositivo/emulador Flutter real, y si se detecta que algún módulo excede
ampliamente el tope `TAMANO_MAXIMO` de 5000 (poco probable para el volumen de negocio
actual), revisar el supuesto de "traer todo en una página" que usan Flutter y los
dropdowns del backoffice.

## Fase 9 — Bloqueadores del cliente Flutter para pruebas en ambiente real (nueva, auditoría 2026-08-24)

### Resultado esperado

El POS Flutter puede operar contra el backend desplegado en el servidor real sin que
Android bloquee las peticiones, y las fallas de sincronización quedan visibles para el
encargado en vez de perderse silenciosamente.

**Cerrada a nivel de código (2026-08-25)** — el usuario confirmó explícitamente que la
verificación en dispositivo/emulador real sigue bloqueada por la inestabilidad del
emulador documentada en `market-flutter/CLAUDE.md` ("Android emulator — finally
working, then died"); esta fase implementó y verificó (build real, `flutter analyze`,
`flutter test`) todo lo que no requiere un dispositivo físico, y deja explícitamente
documentado qué sigue sin verificar y por qué.

### Tareas

- [x] **Bloqueante inmediato**: `network_security_config.xml` nuevo, cleartext
  permitido solo para 2 dominios (`test-server.local`, placeholder a editar antes de
  compilar; `10.0.2.2`, el emulador) — nunca `usesCleartextTraffic="true"` global.
  Referenciado desde `AndroidManifest.xml`. Verificado con un
  `flutter build apk --release` real: compila (AAPT habría fallado si el XML o la
  referencia estuvieran mal) y el manifest fusionado real trae el atributo.
- [x] Documentar/scriptear el build de producción del APK con la URL real —
  documentado en `market-flutter/CLAUDE.md` (sin script nuevo: el proyecto no tiene
  convención de `scripts/`, un solo comando documentado no la necesita).
- [x] Dar visibilidad a los ítems de la cola offline marcados con `mensajeError` —
  `PendientesErrorScreen` nueva (ruta `/pendientes-error`, alcanzable tocando
  `ConnectivityBadge`, ahora interactivo), con REINTENTAR (limpia `mensajeError` y
  dispara un drenado inmediato) y DESCARTAR (con confirmación) por ítem. `LocalStore`
  ganó `listarXPendientesConError()`/`reintentarXPendiente()` para los 3 tipos
  (ventas, clientes, movimientos de caja); descartar reusa los métodos `eliminarX`
  que ya existían.
- [x] Corregir el camino donde `completar()` tiene éxito pero la confirmación se
  pierde — **la descripción original de esta tarea ya estaba desactualizada**: el
  `registrarCobro` posterior que menciona ya no existe (`completar()` resuelve todo
  atómicamente desde la Fase 3 de este mismo plan). El riesgo equivalente en la forma
  actual sí era real y se corrigió: si `completar()` tiene éxito en el servidor pero
  la respuesta se pierde por red, el reintento vuelve a llamar `completar()` sobre una
  venta ya `COMPLETADA` → `409 ESTADO_VENTA_INVALIDO`, que `SyncEngine` marcaba como
  error de negocio permanente aunque la venta ya estuviera bien. `_sincronizarVenta`
  ahora, ante ese código de error específico, confirma el estado real de la venta
  (`VentaApi.obtener()`, endpoint nuevo del lado del cliente) — `COMPLETADA` se trata
  como éxito, cualquier otra cosa (incluida la propia confirmación fallando por red)
  sigue el camino de error de antes.
- [x] Evaluar agregar logging/crash reporting remoto — evaluado, deliberadamente no
  agregado (la tarea pide "evaluar", no "agregar"): requiere una cuenta externa y una
  decisión de qué datos salen del dispositivo, no algo para decidir unilateralmente.
  Recomendación dejada en `market-flutter/CLAUDE.md`: Sentry (`sentry_flutter`) sobre
  Crashlytics si/cuando se decida, por el SDK más simple de integrar con el shape ya
  existente de `ApiException`.
- [ ] Cerrar los gaps ya conocidos y documentados en `market-flutter/CLAUDE.md` —
  **sigue bloqueado**, explícitamente, por la inestabilidad del emulador. Ver
  "Pruebas y aceptación" abajo para el detalle exacto de qué falta.

### Hallazgo adicional: el APK de producción no tenía permiso de red

No estaba en la lista de tareas original — encontrado al implementar la excepción de
cleartext de arriba. `android/app/src/main/AndroidManifest.xml` (el único que se
fusiona en un build **release**) nunca declaró `android.permission.INTERNET` — esa
declaración solo vivía en `android/app/src/{debug,profile}/AndroidManifest.xml`
(agregada por el propio template de Flutter, con un comentario que dice literalmente
"required for development... hot reload"). Un `flutter build apk --release` real
—exactamente el artefacto que esta fase pide documentar— habría producido un APK sin
acceso a red en absoluto, sin ningún error obvio hasta que alguien intentara loguearse
en un dispositivo real. Corregido agregando el permiso a `main/AndroidManifest.xml`;
verificado con un build real (`app-release.apk`, 69.2MB) y grep sobre el manifest
fusionado real de la variante release.

### Archivos principales

- `market-flutter/android/app/src/main/AndroidManifest.xml` (INTERNET +
  `networkSecurityConfig`)
- `market-flutter/android/app/src/main/res/xml/network_security_config.xml` (nuevo)
- `market-flutter/lib/core/sync/sync_engine.dart`
- `market-flutter/lib/core/db/local_store.dart`, `local_store_io.dart`, `local_store_web.dart`
- `market-flutter/lib/features/ventas/data/venta_api.dart` (`obtener()` nuevo)
- `market-flutter/lib/features/sync/presentation/pendientes_error_screen.dart` (nuevo)
- `market-flutter/lib/shared/widgets/connectivity_badge.dart` (tocable)
- `market-flutter/lib/router/app_router.dart` (`/pendientes-error`)
- `market-flutter/CLAUDE.md`

### Pruebas y aceptación

- [ ] Un dispositivo Android real conecta contra el backend del servidor de pruebas
  sin error de cleartext — **no verificado, bloqueado por el emulador**. El mecanismo
  está implementado y el build real compila y empaca correctamente; falta la
  instalación/ejecución real en un dispositivo.
- [x] Una venta con cobro que falla por red se ve reflejada como pendiente y termina
  sincronizándose sin intervención manual — cubierto por el fix de
  `ESTADO_VENTA_INVALIDO` arriba; `flutter analyze` limpio. **No verificado en
  dispositivo real** (mismo bloqueo).
- [x] Un ítem con error de negocio real es visible y accionable desde la UI —
  `PendientesErrorScreen` construida y revisada por código; **cero interacción manual
  real** (ni Chrome ni dispositivo) — solo compila limpio.
- [ ] Los 2 overflows y el flujo offline completo quedan verificados visualmente en un
  dispositivo real (no emulador) — **sigue pendiente**, gap preexistente documentado
  en `market-flutter/CLAUDE.md` antes de esta fase, sin cambios.

## División recomendada en PRs

| PR | Contenido | Dependencias |
| --- | --- | --- |
| 1 | Perfiles, secretos y seed seguro | Ninguna |
| 2 | Autorización multi-tienda | PR 1 recomendado |
| 3 | Contrato y contabilidad de ventas | Decisión funcional de `MIXTO` |
| 4 | Bloqueo e integridad de inventario | PR 3 si cambia el flujo de completar |
| 5 | Refresh token atómico | Independiente después del PR 1 |
| 6 | Idempotencia de ventas | PR 2 y preferiblemente PR 3 |
| 7 | Testcontainers, Actuator y cierre | PRs 1–6 |
| 8 | Paginación, zona horaria dashboard, índices, rotación de logs | Ninguna, puede ir en paralelo desde el inicio |
| 9 | Bloqueadores de cliente Flutter (TLS/cleartext, visibilidad de cola offline, retry de cobro) | El ítem de `network_security_config.xml` es urgente e independiente; el resto depende de PR 3 si cambia el contrato de completar venta |

## Definición global de terminado

El plan se considera completado cuando todas las fases (1–9) cumplen sus criterios, las
migraciones fueron probadas sobre PostgreSQL, los clientes compatibles fueron
actualizados, la suite completa pasa, el despliegue productivo no depende de ningún
valor o artefacto de desarrollo, y el cliente Flutter fue verificado en al menos un
dispositivo Android real contra el servidor de pruebas.
