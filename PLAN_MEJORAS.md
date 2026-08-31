# Plan de mejoras — Market ERP

## 1. Propósito

Este documento convierte la auditoría técnica del 28 de agosto de 2026 en un plan
ejecutable para los tres componentes del sistema:

- `market-backend`: API y lógica de negocio en Spring Boot.
- `market-backoffice`: administración en Vue 3 y TypeScript.
- `market-flutter`: punto de venta (POS) Flutter con operación offline.

El sistema tiene una buena base para un piloto controlado, pero no debe considerarse
listo para producción fiscal o para operar dinero sin conciliación manual hasta cerrar,
como mínimo, las fases P0 y P1 de este documento.

## 2. Estado base verificado

Auditoría realizada sobre el repositorio limpio el 28 de agosto de 2026:

| Componente | Verificación | Resultado |
| --- | --- | --- |
| Backend | `mvn test` | 516 pruebas, 0 fallos |
| Backoffice | `vue-tsc -b --noEmit` | Correcto |
| Backoffice | `vitest run` | 21 pruebas, 0 fallos |
| Flutter | `flutter analyze` | Repetido 2026-08-28: "No issues found!" |
| Flutter | `flutter test` | Repetido 2026-08-28: 1 test, "All tests passed!" (cobertura mínima, ver Fase 9) |
| Integración PostgreSQL | `mvn verify` / Testcontainers | No ejecutado; Docker no estaba activo |

Estos resultados son una línea base, no una garantía de los flujos concurrentes,
offline, fiscales o de recuperación descritos más adelante.

**Verificación de código contra este plan realizada el 2026-08-28** (lectura directa
del repositorio, sin ejecutar los flujos de negocio). Cada fase abajo indica, con
archivo:línea, qué sigue pendiente y qué ya está resuelto. Los ítems ya resueltos se
retiraron de las listas de tareas.

## 3. Reglas de ejecución

1. Trabajar una fase por PR o por conjunto pequeño de PR relacionados.
2. No editar changesets de Liquibase ya aplicados; agregar uno nuevo por cambio.
3. Toda operación que mueva dinero, inventario o documentos fiscales debe tener:
   - una invariante de dominio;
   - protección de concurrencia;
   - idempotencia cuando pueda reintentarse;
   - restricción equivalente en base de datos cuando sea posible;
   - prueba unitaria y prueba de integración con PostgreSQL real.
4. No desplegar cambios de contrato sin actualizar backend, backoffice y POS en el
   mismo ciclo o mantener compatibilidad temporal.
5. Todo PR debe registrar los comandos ejecutados y sus resultados.
6. Antes de producción, realizar pruebas en tablet Android real, no solo en web o
   emulador.
7. No marcar una fase como terminada solo porque compila: deben cumplirse sus
   criterios de aceptación.

## 4. Resumen de prioridades

| Prioridad | Fase | Objetivo | Bloquea producción |
| --- | --- | --- | --- |
| P0 | 1 | FEL real y seguro por ambiente | Sí, si se emitirán facturas |
| P0 | 2 | Idempotencia integral del POS | Sí |
| P0 | 3 | Concurrencia e integridad contable | Sí |
| P1 | 4 | Sesiones, seguridad y alcance multiinstancia | Sí |
| P1 | 5 | Pruebas críticas y CI/CD | Sí |
| P1 | 6 | Backups, restauración y continuidad | Sí |
| P1 | 7 | Auditoría y observabilidad | Recomendado antes de producción |
| P2 | 8 | Calidad y mantenibilidad del backoffice | No |
| P2 | 9 | Robustez y mantenibilidad de Flutter | No |
| P2 | 10 | Funciones comerciales faltantes | Según alcance del negocio |
| P3 | 11 | Rendimiento, escalado y operación avanzada | No para el piloto |

---

## Fase 1 — FEL real y seguro por ambiente (P0)

### Problema

`DevCertificadorFelAdapter` está registrado como `@Component` sin restricción de
perfil. Genera un UUID aleatorio y puede simular una certificación incluso en
producción. Además, el correlativo usa `MAX(numero) + 1`, vulnerable a concurrencia.

**Confirmado en código (2026-08-28):**
`DevCertificadorFelAdapter.java:17-23` era `@Component` sin `@Profile`, único
implementador de `CertificadorFelPort` (se activaría en `prod`). No existe adaptador
FEL real (solo el simulado). `DocumentoFelJpaRepository.findMaxNumero` +
`DocumentoFelRepositoryAdapter.siguienteNumero()` hacían `MAX+1` sin lock; sí existe
`UNIQUE(tienda_id, serie, numero)` en BD que evita duplicado silencioso, pero sin
reintento automático ante el conflicto. `FelServiceImpl.anular` solo cambia estado
local, sin llamar a ningún puerto externo. `ProdSafetyGuard` no validaba nada de FEL.
No hay idempotencia propia del módulo FEL (sí existe `correlationId` en `ventas`, pero
no cubre la certificación).

**Resuelto (2026-08-28):** el adaptador simulado ya está restringido a
`@Profile("!prod")`; nuevo `FelProdSafetyGuard` rechaza el arranque en `prod` si no hay
un `CertificadorFelPort` real registrado (con test de integración
`perfilProdSinCertificadorFelRealNoArranca` en `ProfileStartupIT`); el correlativo ya
no usa `MAX+1` a ciegas, sino una tabla `fel_correlativo` bloqueada con
`PESSIMISTIC_WRITE` por `(tienda_id, serie)` en una transacción `REQUIRES_NEW` propia
(con reintento ante colisión de creación concurrente, igual que el patrón de
`InventarioServiceImpl`), con dos IT de concurrencia real contra Postgres
(`FelCorrelativoConcurrenciaIT`). Sigue pendiente todo lo demás: no hay adaptador FEL
real (parte B, fuera de este alcance — requiere elegir proveedor y credenciales
reales), ni anulación fiscal real, ni idempotencia propia del módulo FEL.

### Tareas

- [x] Marcar el adaptador simulado con `@Profile("!prod")`.
- [x] Crear una validación de arranque que rechace `prod` si no existe un adaptador FEL
  real configurado (`FelProdSafetyGuard`).
- [ ] Seleccionar certificador autorizado (por ejemplo Infile, Digifact u otro).
- [ ] Implementar el adaptador HTTP real con:
  - autenticación y secretos externos al repositorio;
  - timeouts diferenciados de conexión y respuesta;
  - manejo de errores normalizado;
  - identificador idempotente por documento;
  - trazabilidad sin registrar XML, credenciales o datos sensibles completos.
- [x] Reemplazar `MAX + 1` por una secuencia o tabla de correlativos bloqueada por
  `(tienda_id, serie)` (tabla `fel_correlativo` + `PESSIMISTIC_WRITE`).
- [ ] Definir el flujo de reintento ante respuesta incierta del certificador: primero
  consultar por identificador idempotente, nunca emitir nuevamente a ciegas.
- [ ] Implementar anulación fiscal real, no solo cambio de estado local.
- [ ] Persistir solicitud/respuesta fiscal sanitizada o referencias verificables según
  las exigencias del proveedor y SAT.
- [ ] Agregar certificados/credenciales mediante secret manager o archivos montados de
  solo lectura.

### Pruebas requeridas

- [x] Producción no arranca con el adaptador simulado
  (`ProfileStartupIT.perfilProdSinCertificadorFelRealNoArranca`).
- [x] Dos (y diez) emisiones concurrentes reciben correlativos distintos
  (`FelCorrelativoConcurrenciaIT`, contra Postgres real vía Testcontainers).
- [ ] Un timeout posterior a una certificación no crea un segundo DTE.
- [ ] Reintentar un documento fallido conserva el mismo identificador de negocio.
- [ ] Pruebas contractuales contra sandbox del certificador.
- [ ] Prueba manual de emisión, consulta, descarga y anulación en ambiente de pruebas.

### Criterio de aceptación

Una factura mostrada como `CERTIFICADA` puede comprobarse en el proveedor autorizado,
su correlativo es único y un reintento nunca genera un documento fiscal duplicado.

---

## Fase 2 — Idempotencia integral y operación offline del POS (P0)

### Problema

Las ventas online no envían `correlationId`. Los movimientos de caja y altas de
clientes offline tampoco tienen clave idempotente. `connectivity_plus` detecta una
interfaz de red, no la disponibilidad real del backend.

**Confirmado en código (2026-08-28), toda la fase sigue pendiente:**
`checkout_notifier.dart` confirma que la venta online (`_confirmarOnline`) no genera
`correlationId` (solo offline lo hace, y como `'${DateTime.now().microsecondsSinceEpoch}-$tiendaId'`,
no UUID). Caja (`AbrirCajaRequest`/`RegistrarMovimientoCajaRequest`/`CerrarCajaRequest`)
no tiene clave idempotente. Clientes sin NIT no tienen deduplicación
(`ClienteServiceImpl` solo valida NIT no vacío). No existe endpoint de consulta por
clave idempotente. `connectivity_provider.dart` solo detecta interfaz de red, el
propio comentario del código lo admite. El fallback de "Consumidor Final" con ID `1`
solo se usa en modo offline (`checkout_notifier.dart:25,143`); online sí resuelve por
nombre. El logout ya advierte si hay pendientes sin sincronizar (`logout_confirm.dart`),
pero no bloquea ni protege contra desinstalación.

### Diseño objetivo

Cada intención del usuario debe tener un UUID estable generado antes del primer
request. El UUID se conserva localmente hasta conocer el resultado definitivo.

Estados sugeridos para una operación local:

```text
CREADA_LOCAL -> ENVIANDO -> CONFIRMADA
                    |-> PENDIENTE_REINTENTO
                    |-> REQUIERE_REVISION
```

### Tareas de backend

**Resuelto (2026-08-28) — parte A (backend) de la fase, sin tocar Flutter todavía:**
apertura, movimiento y cierre de caja ya aceptan `correlationId` opcional
(`AbrirCajaRequest`/`RegistrarMovimientoCajaRequest`/`CerrarCajaRequest`), con
idempotencia real: un reintento con el mismo contenido bajo la misma clave devuelve el
estado existente (incluso el cierre, reintentado después de que la caja ya quedó
`CERRADA`); el mismo `correlationId` con datos distintos lanza
`CorrelationIdReutilizadoException` (409). Igual para alta de clientes
(`CrearClienteRequest.correlationId`) — cubre el caso sin NIT que antes no tenía
ninguna protección. Todo con restricción única en BD
(`caja_sesion.correlation_id_apertura/cierre` por tienda,
`movimiento_caja.correlation_id` por sesión, `cliente.correlation_id` global) y
recuperación ante colisión de creación concurrente (mismo patrón ya usado por
`VentaServiceImpl.crear`: catch + releer, nunca reintentar a ciegas dentro de la misma
transacción). Se agregó `GET /api/v1/ventas/tiendas/{tiendaId}/correlation/{id}` para
resolver una respuesta incierta sin reintentar el `POST` a ciegas; para caja y
clientes se decidió NO agregar un `GET` equivalente — reintentar el mismo `POST` con
el mismo `correlationId` ya es seguro y devuelve el estado existente, así que un `GET`
adicional sería redundante ahí. `correlationId` sigue opcional en los tres flujos: no
se hizo obligatorio en venta online porque el POS Flutter (parte B, aún no tocada)
todavía no lo envía en ese flujo — obligarlo ahora habría roto la venta online real.

- [ ] Convertir la idempotencia de venta en requisito para todos los clientes, no solo
  para sincronización offline — bloqueado hasta que Flutter (parte B) genere y envíe
  `correlationId` también en la venta online.
- [x] Agregar idempotencia a movimientos de caja (y apertura/cierre).
- [x] Agregar idempotencia a creación de clientes (`correlationId`, cubre clientes sin
  NIT).
- [x] Considerar idempotencia para apertura/cierre de caja y movimientos — hecho. CxC/CxP
  (cobros/pagos) sigue pendiente, es Fase 3 (concurrencia), no Fase 2.
- [x] Guardar hash/canonicalización del contenido asociado a la clave y devolver `409`
  si se reutiliza con datos diferentes — implementado como comparación de campos (no
  hash), equivalente y ya el patrón existente en `VentaServiceImpl`.
- [x] Definir endpoint de consulta por clave idempotente para resolver respuestas
  inciertas — hecho para ventas; para caja/clientes se optó por reintento seguro del
  mismo `POST` en vez de un `GET` adicional (ver nota arriba).

### Tareas de Flutter

**Resuelto (2026-08-28) — parte B (Flutter) de la fase, alcance acotado a UUID +
conectividad real (parte C — cola offline profunda — se aborda por separado más abajo):**
`checkout_notifier.dart` ya no usa `DateTime.now().microsecondsSinceEpoch` para nada
— toda venta (online y offline) genera un UUID v4 real (paquete `uuid`,
`nuevoCorrelationId()`). La venta online ahora también manda `correlationId` al
backend (antes solo lo hacía la sincronización offline) — generado **una sola vez por
intento de cobro** en `CobroSheet` (ligado al ciclo de vida de esa hoja abierta) y
reutilizado si el vendedor reintenta manualmente tras un error, nunca regenerado —
regenerarlo en cada reintento habría anulado la protección de idempotencia que esto
existe para dar. Nuevo `backendAlcanzableProvider`
(`core/connectivity/backend_reachability_provider.dart`) distingue "sin interfaz de
red" de "hay red pero el backend no responde": sondea `GET /actuator/health` (público,
sin auth) al reconectar y cada 15s mientras la interfaz siga arriba, con timeout corto
propio (4s) para no bloquear la UI. `CheckoutNotifier`, `CajaActionsNotifier`,
`ClienteSelectorSheet` y `SyncEngineNotifier` ya no leen `redDisponibleProvider`
(solo interfaz) para decidir si hay red — todos leen este nuevo provider, así que
"Wi-Fi activo con backend caído" ahora sí encola/reintenta en vez de tratarse como
conectado. `redDisponibleProvider` se mantiene tal cual (interfaz cruda) porque
`backendAlcanzableProvider` lo usa como base. `flutter analyze` y `flutter test`
limpios.

**Verificado en Chrome (2026-08-28) contra backend y Postgres reales:** login,
catálogo, venta en efectivo completa — el UUID generado por el navegador
(`6f3fc2f9-09e3-4628-af62-054a372016b1`) quedó persistido en `venta.correlation_id`
(confirmado por consulta directa a la base). El badge de conectividad pasó de
"Conectado" a "Sin conexión" al matar el proceso del backend (con la interfaz de red
seguía arriba) y volvió solo a "Conectado" al reiniciarlo, sin recargar la página —
exactamente el escenario "Wi-Fi activo, backend caído" que antes no se distinguía. No
se alcanzó a probar puntualmente "reintento manual sobre la misma hoja reutiliza la
misma clave" (quedó solo revisado por código, es un `final` de instancia de
`_CobroSheetState`, no debería poder regenerarse).

**Bug preexistente encontrado y arreglado durante esta verificación (no introducido
por este trabajo):** `ClientesApi.listar()` (`clientes_api.dart`) parseaba
`GET /api/v1/clientes` como un array plano (`data as List<dynamic>`), pero ese
endpoint ya devuelve el mismo envelope paginado que otros listados
(`{contenido, pagina, ...}`) desde el rollout de paginación documentado en el propio
`CLAUDE.md` del proyecto — ese rollout actualizó `ProductosApi` y
`CuentaPorCobrarApi`, pero se saltó `ClientesApi`. Efecto real: `TypeError: ... is not
a subtype of type 'List<dynamic>'` en cuanto había algún cliente en la tienda
(incluido el "Consumidor Final" seedeado por defecto) — **toda venta online sin
cliente explícito fallaba silenciosamente** con el mensaje genérico "No se pudo
completar la venta.", y lo mismo el selector de cliente para crédito. Arreglado con el
mismo patrón ya usado en los otros dos (`contenidoDePagina()` +
`size: tamanoPaginaCompleta`).

- [x] Generar UUID criptográficamente aleatorio para toda venta online y offline.
- [ ] Persistir la intención antes de llamar al backend; eliminarla solo después de
  confirmar el resultado — ya cierto para offline (sin cambios); la venta online no
  persiste la intención localmente antes de llamar (fuera de alcance de esta parte,
  requeriría una cola también para el camino online).
- [x] No usar `DateTime.microsecondsSinceEpoch` como identificador distribuido.
- [ ] Si falla una operación por timeout, DNS, TLS o conexión, consultar su estado por
  clave antes de repetirla — el backend ya expone el `GET` de consulta (Fase 2 parte
  A); Flutter todavía no lo consulta antes de reintentar, solo reutiliza la misma
  clave al reintentar el `POST` (ver nota arriba).
- [x] Encolar automáticamente cuando la API sea inalcanzable, aunque el dispositivo
  tenga Wi-Fi o datos — vía `backendAlcanzableProvider`.
- [x] Distinguir sin interfaz de red / red disponible pero API inalcanzable (las otras
  tres — error de autenticación, error de negocio permanente, respuesta incierta — ya
  las distinguía `ApiException`/`SyncEngine` antes de esta fase, sin cambios aquí).
- [x] Procesar dependencias de cola explícitamente; por ejemplo, un cliente local debe
  sincronizarse antes de una venta que lo referencia — resuelto (2026-08-28, parte C):
  `ClienteSeleccionado` (real o `pendienteLocal`), `VentaPendienteIsar.clientePendienteLocalId`,
  `ClientePendienteIsar.clienteServidorId` (fila conservada tras sincronizar, no
  borrada, para que la venta pueda resolver el id real después). Una venta que
  referencia un cliente offline SIEMPRE se encola (nunca intenta ir online), y su
  sincronización espera a que ese cliente sincronice primero (mismo orden ya
  existente clientes→ventas). Sin verificar en dispositivo real (requiere
  `LocalStore.disponible == true`, no probable en Chrome — ver
  `market-flutter/CLAUDE.md`, "Dependencias de cola offline").
- [ ] Evitar el ID fijo `1` para “Consumidor Final”; resolverlo por código estable
  expuesto por la API o configuración de tienda.
- [x] Impedir o advertir logout/desinstalación cuando existan operaciones pendientes —
  resuelto (2026-08-28, parte C) para logout: **decisión del usuario, bloqueo duro sin
  bypass**. Antes advertía con opción de "cerrar sesión de todos modos"; ahora, con
  algo pendiente, `logout()` simplemente no se llama — el único camino es sincronizar
  (conectarse) o descartar explícitamente un ítem atascado desde
  `PendientesErrorScreen` (que ya exige su propia confirmación de "no se puede
  deshacer"). Desinstalación: **decisión del usuario, marcado como no implementable
  vía código de la app** — un app normal en Android/iOS no puede interceptar ni
  bloquear su propia desinstalación sin Device Admin/MDM (gestión de dispositivos),
  fuera de alcance de un cambio de código; no se investigó más allá de confirmar esto.
- [x] Definir migración y versión del esquema Isar — resuelto (2026-08-28, parte C):
  `esquemaLocalVersionActual` + colección `MetadatoLocalIsar` (una fila) registran con
  qué versión se escribió la base local. Un cambio de versión detectado sin nada
  pendiente real limpia el mirror y reinicia limpio (cero riesgo, es solo caché); con
  algo pendiente, nunca se borra solo — cualquier migración real que un cambio no
  aditivo necesite se agrega a mano en `_aplicarMigracionSiHaceFalta` para esa versión
  específica antes de publicarla, no hay forma de derivarla genéricamente. Sin probar
  contra un escenario real de actualización (requiere dispositivo real con una versión
  anterior instalada, no solo tests unitarios) — ver `market-flutter/CLAUDE.md`,
  "Versión de esquema local Isar".
- [x] Evaluar cifrado de datos locales o minimizar los datos personales persistidos —
  resuelto (2026-08-28, parte C). Evaluado: `isar_community` no tiene cifrado nativo
  (verificado en su código fuente, cero mención de `encrypt`); cifrar de verdad
  requeriría cifrar campos a mano con una librería nueva. Inventariada la PII real en
  Isar: solo `ClientePendienteIsar.nombre/telefono/nit` (clientes dados de alta
  offline) — el resto de las colecciones (ventas, movimientos, catálogo) no guardan
  PII directa. **Decisión del usuario:** minimizar en vez de cifrar — se implementó
  `LocalStore.limpiarClientesPendientesSincronizadosSinReferencia()` (llamada al
  final de cada drenado), que borra un cliente ya sincronizado en cuanto ninguna
  venta pendiente sigue referenciándolo, en vez de conservarlo indefinidamente.
  Cifrado de campo (AES vía `flutter_secure_storage`) quedó evaluado y descartado por
  ahora — el cifrado de disco del sistema operativo (activo de fábrica en Android
  moderno) ya cubre el escenario real de riesgo ("tablet robada/perdida apagada") sin
  agregar una librería de cifrado nueva ni manejo de llaves para un dato de exposición
  ya acotada y ahora de vida corta.

### Pruebas requeridas

- [ ] Respuesta perdida después de crear y después de completar una venta.
- [ ] Reintento tras matar la app durante cada estado.
- [ ] Wi-Fi activo con backend caído.
- [ ] Cambio entre Wi-Fi y datos durante sincronización.
- [ ] Dos dispositivos generan operaciones simultáneamente.
- [x] Movimiento de caja procesado con respuesta perdida no se duplica — cubierto con
  tests unitarios (`CajaServiceImplTest`, incluida la colisión de creación
  concurrente simulada); falta un test de concurrencia real contra Postgres como
  `FelCorrelativoConcurrenciaIT` (Fase 1) — no se hizo aquí por alcance, agregar
  cuando se retome esta fase.
- [ ] Actualización de app conserva y migra pendientes existentes.
- [ ] Pruebas en tablet Android real con modo avión y red inestable.

### Criterio de aceptación

Ante cualquier reintento razonable, cada intención del vendedor produce como máximo
una operación de negocio y ninguna venta confirmada se pierde localmente.

---

## Fase 3 — Concurrencia e integridad contable (P0)

**Confirmado en código (2026-08-28):** Inventario ya tiene `PESSIMISTIC_WRITE`
(`InventarioJpaRepository.java:23`, usado en `InventarioServiceImpl`) — esa parte de
la fase ya está resuelta, mantenerla así. Queda pendiente: estados concurrentes de
compras/traslados/FEL y `CHECK` de BD para montos/saldos/estados.

**Resuelto (2026-08-28) — Crédito de cliente:** `VentaServiceImpl.validarLimiteCredito`
leía el cliente con `ClienteService.obtener` (sin bloqueo) — dos ventas a crédito casi
simultáneas del mismo cliente podían leer el mismo saldo pendiente y juntas exceder el
límite aunque cada una, evaluada sola, no lo hiciera. Se agregó
`ClienteRepository.findByIdConBloqueo` (`@Lock(PESSIMISTIC_WRITE)`) y
`ClienteService.obtenerParaActualizarCredito`, usado exclusivamente por
`VentaServiceImpl.completar()` para serializar la validación entre ventas concurrentes
del mismo cliente — la segunda venta espera a que la primera termine de commitear y ve
el saldo ya actualizado. Cubierto por `ClienteServiceImplTest` (unitario) y por
`VentaCreditoConcurrenciaIT` (Testcontainers/Postgres real): dos ventas a crédito
concurrentes cercanas al límite — exactamente una tiene éxito, el saldo nunca lo supera.

**Resuelto (2026-08-28) — Concurrencia de caja:** `CajaServiceImpl.abrir` solo
comprobaba en memoria si ya había una caja abierta (sin bloqueo ni restricción en BD)
— dos aperturas concurrentes para la misma tienda podían ambas pasar el chequeo y
crear dos sesiones ABIERTA a la vez. `registrarMovimiento`/`cerrar` leían la sesión sin
bloqueo — dos movimientos concurrentes podían perderse entre sí (la colección JPA de
movimientos usa `orphanRemoval`, que en un merge concurrente sin lock puede borrar como
"huérfano" un movimiento insertado por la otra transacción), y dos cierres concurrentes
podían pisarse el monto contado sin ningún error. Se agregó el índice único parcial
`ux_caja_sesion_abierta_por_tienda` (`caja/004-una-abierta-por-tienda.xml`, solo sobre
filas con `estado = 'ABIERTA'`) — `abrir` traduce su violación a
`CajaSesionAbiertaException` — y `CajaSesionRepository.findAbiertaByTiendaIdConBloqueo`
(`@Lock(PESSIMISTIC_WRITE)`), usado por `registrarMovimiento`/`cerrar`/
`registrarMovimientoSiHayAbierta` (ahora `@Transactional`, manteniendo el lock hasta el
commit) — lo que además vuelve innecesario el patrón de detectar-colisión-y-releer para
estos métodos, ya que la segunda solicitud concurrente espera el lock y relee el estado
ya actualizado antes de intentar guardar. Cubierto por `CajaServiceImplTest`
(unitario, incluida la colisión de apertura concurrente sin correlationId) y por
`CajaConcurrenciaIT` (Testcontainers/Postgres real): dos aperturas concurrentes —
exactamente una tiene éxito; diez movimientos concurrentes sobre la misma caja — saldo
final exacto, sin movimientos perdidos.

**Resuelto (2026-08-28) — Concurrencia de CxC/CxP:**
`CuentaPorCobrarServiceImpl.registrarCobro`/`CuentaPorPagarServiceImpl.registrarPago`
(y sus respectivos `anular`) leían la cuenta con `findById` (sin bloqueo) — dos
cobros/pagos casi simultáneos sobre la misma cuenta podían leer el mismo saldo
pendiente y juntos superarlo aunque cada uno, evaluado solo, no lo hiciera (mismo
riesgo de colección JPA con `orphanRemoval` que en Caja). Se agregó
`findByIdConBloqueo` (`@Lock(PESSIMISTIC_WRITE)`) en ambos repositorios, usado por
las cuatro operaciones mutadoras. Cubierto por `CuentaPorCobrarServiceImplTest`/
`CuentaPorPagarServiceImplTest` (unitarios) y por `CuentaPorCobrarConcurrenciaIT`/
`CuentaPorPagarConcurrenciaIT` (Testcontainers/Postgres real): dos cobros/pagos
concurrentes cercanos al saldo — exactamente uno tiene éxito, el saldo nunca se
supera ni queda negativo.

**Resuelto (2026-08-28) — Ejecución duplicada de gasto programado:**
`GastoProgramadoServiceImpl.generarPago` leía el gasto con `findById` (sin bloqueo)
— dos ejecuciones casi simultáneas del mismo gasto podían leer la misma
`proximaFecha` vencida y ambas pasar la validación, generando dos pagos para el
mismo período (mismo riesgo de colección JPA con `orphanRemoval`). Se agregó
`findByIdConBloqueo` (`@Lock(PESSIMISTIC_WRITE)`), usado por `generarPago`.
Cubierto por `GastoProgramadoServiceImplTest` (unitario) y por
`GastoProgramadoConcurrenciaIT` (Testcontainers/Postgres real): dos ejecuciones
concurrentes del mismo período — exactamente una tiene éxito, un solo pago
registrado.

**Resuelto (2026-08-28) — Estados concurrentes de compras/traslados/FEL:**
`CompraServiceImpl.recibir`/`anular`, `TrasladoServiceImpl.completar`/`anular` y
`FelServiceImpl.reintentar`/`anular` leían el agregado con `findById` (sin
bloqueo) — dos transiciones casi simultáneas sobre el mismo agregado podían
ambas leer el mismo estado y pasar la validación: en Traslado esto duplicaba
sin ningún control los movimientos de Inventario (salida/entrada) al no haber
restricción de BD que lo impidiera; en Compra la restricción única de
`cuenta_por_pagar.compra_id` abortaba la transacción perdedora completa pero
con un mensaje de error engañoso ("proveedor/tienda no existe"); en FEL dos
`anular`/`reintentar` podían pisarse el resultado sin ningún error. Se agregó
`findByIdConBloqueo` (`@Lock(PESSIMISTIC_WRITE)`) en los tres repositorios,
usado por las seis operaciones de transición de estado — la segunda solicitud
ahora espera, relee el estado ya actualizado y falla con el error de negocio
correcto (`EstadoCompraInvalidoException`/`EstadoTrasladoInvalidoException`/
`EstadoDocumentoFelInvalidoException`) en vez de duplicar efectos o fallar con
un mensaje engañoso. Cubierto por los unitarios existentes (mocks
actualizados) y por `CompraConcurrenciaIT`/`TrasladoConcurrenciaIT`/
`FelConcurrenciaIT` (Testcontainers/Postgres real): dos transiciones
concurrentes sobre el mismo agregado — exactamente una tiene éxito, sin
duplicar movimientos de inventario.

**Resuelto (2026-08-29) — `CHECK` de base de datos:** el dominio ya exigía montos
positivos, saldos no negativos y algunas combinaciones de estado, pero nada lo
respaldaba en BD — un `INSERT`/`UPDATE` directo (script, migración de datos, bug
futuro que use SQL crudo) podía violar esas invariantes sin que Postgres protestara.
Se agregaron `CHECK` en los agregados monetarios/de cantidad tocados en esta fase:
`caja_sesion`/`movimiento_caja`, `cuenta_por_cobrar`/`cobro`, `cuenta_por_pagar`/
`pago`, `gasto_programado`/`pago_gasto_programado`, `linea_compra`, `linea_traslado`,
`linea_venta`, `inventario`/`movimiento_inventario`, `producto_tienda` y
`documento_fel`/`fel_correlativo` — montos/cantidades positivos o no negativos según
el caso, y dos invariantes de estado adicionales antes solo garantizadas por
construcción en el dominio: `saldo_pendiente <= monto_original` en CxC y CxP, y
`stock_minimo <= stock_maximo` en `producto_tienda`. Un primer intento con `<=` sin
escapar rompió el parseo XML de Liquibase (`&lt;=` es obligatorio dentro de
`<sql>`), detectado por `LiquibaseMigrationIT`. Cubierto por `CheckConstraintsIT`
(Testcontainers/Postgres real): inserta/actualiza filas inválidas por SQL directo
(sin pasar por el dominio, que las rechazaría antes) y confirma que Postgres las
rechaza — y que una fila válida sí se acepta — para una muestra representativa de
las restricciones agregadas.

**Resuelto (2026-08-29) — Revisión general de flujos `find -> validar -> save`:**
auditoría de todos los `*ServiceImpl` del backend en busca de este mismo patrón en
módulos aún no tocados por la fase. Encontrado y corregido:
`VentaServiceImpl.completar`/`anular` leían la venta con `findById` (sin bloqueo)
— dos `completar` casi simultáneos sobre la misma venta en BORRADOR (doble clic, o
un reintento de red solapado) podían ambos pasar la validación y duplicar el
movimiento de Inventario/CxC/Caja antes de que cualquiera commiteara; mismo patrón
ya corregido en Compra/Traslado/FEL, aquí en la propia Venta. Se agregó
`findByIdConBloqueo` (`@Lock(PESSIMISTIC_WRITE)`), usado por `completar`/`anular`.
De paso se encontró y corrigió un bug de orden independiente de la concurrencia:
`completar` validaba el límite de crédito *antes* de comprobar que la venta seguía
en BORRADOR — un segundo intento sobre una venta ya completada (con o sin carrera)
podía fallar con `LimiteCreditoExcedidoException` en vez del `EstadoVentaInvalidoException`
correcto, porque la CxC ya creada por el primer intento inflaba el saldo proyectado
del segundo. Se movió `venta.completar()` (la comprobación de estado) al inicio del
método. Cubierto por `VentaServiceImplTest` (unitario, mocks actualizados, 4
aserciones ajustadas para verificar "nunca se guardó" en vez de mutación en memoria
del objeto de dominio) y por `VentaConcurrenciaIT` (Testcontainers/Postgres real):
dos `completar` concurrentes sobre la misma venta — exactamente uno tiene éxito,
el inventario refleja la salida una sola vez.

**Resuelto (2026-08-29) — Asignación mixta tienda/grupo concurrente:**
`UsuarioServiceImpl.asignarTienda`/`asignarGrupo` permitían una carrera entre asignar
una tienda individual (`usuario_tienda`) y asignar el grupo de esa tienda
(`usuario_grupo_tienda`) al mismo usuario — cada tabla tiene su propia restricción
única, pero ninguna restricción en BD abarca ambas tablas a la vez, así que dos
solicitudes concurrentes podían violar la regla de negocio "no permitir asignación
mixta" sin que ninguna fallara. A diferencia del resto de esta fase, aquí no hay una
sola fila que ambas operaciones toquen — se resolvió bloqueando la fila del propio
`usuario` (`UsuarioRepository.findByIdConBloqueo`, `PESSIMISTIC_WRITE`) como punto de
serialización compartido entre las dos tablas: cualquier `asignarTienda`/
`asignarGrupo` concurrente para el mismo usuario se serializa sin importar en cuál de
las dos tablas escriba cada uno. Cubierto por `UsuarioServiceImplTest` (mocks
actualizados) y por `AsignacionMixtaConcurrenciaIT` (Testcontainers/Postgres real):
un `asignarTienda` y un `asignarGrupo` concurrentes para el mismo usuario, con la
tienda perteneciendo al grupo — exactamente uno tiene éxito.

**Resuelto (2026-08-29) — Traducir conflictos de concurrencia a HTTP consistente:**
todas las excepciones de negocio lanzadas por los locks agregados en esta fase
(`EstadoVentaInvalidoException`, `CajaSesionAbiertaException`,
`CobroExcedeSaldoException`, etc.) ya extendían `BusinessException` y por lo tanto
ya se traducían de forma consistente vía `GlobalExceptionHandler.handleBusiness` —
nada que hacer ahí. El hueco real estaba un nivel más abajo: si Postgres mismo
aborta una transacción por contención (deadlock detectado, SQLState `40P01`, o una
espera de `PESSIMISTIC_WRITE` que agota el tiempo), Spring traduce eso a
`ConcurrencyFailureException` (o una subclase: `CannotAcquireLockException`,
`DeadlockLoserDataAccessException`) — una excepción de infraestructura, no de
negocio, que antes caía en el `@ExceptionHandler(Exception.class)` genérico y
respondía 500 "Ocurrió un error inesperado", indistinguible de un error real para
el cliente (que en este caso solo necesita reintentar). Se agregó un handler para
`ConcurrencyFailureException` en `GlobalExceptionHandler` que responde 409 con
`errorCode: CONFLICTO_CONCURRENCIA` y un mensaje que invita a reintentar. Cubierto
por `GlobalExceptionHandlerTest` (nuevo): un controlador mínimo de prueba que lanza
`CannotAcquireLockException`/`DeadlockLoserDataAccessException` directamente,
confirmando que ambas responden 409 con el código consistente.

### Tareas

- [x] Crear una matriz de agregados y estrategia de concurrencia:

| Agregado | Estrategia inicial recomendada |
| --- | --- |
| Inventario | [x] Mantener `PESSIMISTIC_WRITE` existente |
| Caja | [x] Bloqueo de sesión abierta + restricción única parcial |
| CxC/CxP | [x] `PESSIMISTIC_WRITE` (`findByIdConBloqueo`) |
| Crédito cliente | [x] Serializar por cliente (`PESSIMISTIC_WRITE` vía `findByIdConBloqueo`) |
| Gasto programado | [x] `PESSIMISTIC_WRITE` (`findByIdConBloqueo`) |
| FEL | [x] Correlativo atómico (Fase 1) + `PESSIMISTIC_WRITE` en reintentar/anular |

- [x] Agregar una restricción PostgreSQL que permita una sola caja abierta por tienda
  (2026-08-28, índice único parcial `ux_caja_sesion_abierta_por_tienda`).
- [x] Bloquear la caja durante registrar movimiento y cerrar (2026-08-28,
  `findAbiertaByTiendaIdConBloqueo` + `@Transactional`).
- [x] Hacer atómico "validar saldo + registrar cobro/pago + actualizar saldo"
  (2026-08-28, `findByIdConBloqueo` en CxC y CxP).
- [x] Hacer atómico "validar límite + crear exposición crediticia" (2026-08-28,
  `ClienteService.obtenerParaActualizarCredito` + `VentaServiceImpl.completar`).
- [x] Añadir `CHECK` de base de datos para montos positivos, saldos no negativos y
  combinaciones de estado críticas (2026-08-29 — ver detalle abajo).
- [x] Revisar todos los flujos `find -> validar -> save` monetarios (2026-08-29 —
  ver detalle arriba; encontró y corrigió el mismo hueco en `Venta.completar`/
  `anular`; dejó documentado un hallazgo no monetario en `UsuarioServiceImpl`).
- [x] Traducir conflictos de concurrencia a códigos HTTP y mensajes consistentes
  (2026-08-29 — ver detalle abajo).

### Pruebas requeridas

- [x] Dos aperturas simultáneas: exactamente una tiene éxito (`CajaConcurrenciaIT`,
  2026-08-28).
- [x] Diez movimientos paralelos: saldo final exacto (`CajaConcurrenciaIT`, 2026-08-28).
- [ ] Cierre concurrente con venta: resultado serializable y auditable (el lock ya
  serializa `cerrar` contra `registrarMovimiento`/`registrarMovimientoSiHayAbierta`
  sobre la misma sesión; falta un test específico que ejercite esta combinación).
- [x] Dos cobros/pagos sobre el último saldo: nunca hay saldo negativo
  (`CuentaPorCobrarConcurrenciaIT`/`CuentaPorPagarConcurrenciaIT`, 2026-08-28).
- [x] Dos ventas de crédito cercanas al límite: nunca se supera el límite
  (`VentaCreditoConcurrenciaIT`, 2026-08-28).
- [x] Dos ejecuciones del mismo gasto/período: un solo pago
  (`GastoProgramadoConcurrenciaIT`, 2026-08-28).
- [x] Transiciones de estado concurrentes en compra/traslado/FEL/venta: exactamente
  una tiene éxito, sin duplicar movimientos de inventario
  (`CompraConcurrenciaIT`/`TrasladoConcurrenciaIT`/`FelConcurrenciaIT`, 2026-08-28;
  `VentaConcurrenciaIT`, 2026-08-29).
- [x] Ejecutar contra PostgreSQL real, no H2 ni únicamente mocks (todos los IT de
  esta fase usan Testcontainers con Postgres real).

### Criterio de aceptación

Las invariantes contables se mantienen bajo carga concurrente y están protegidas tanto
por código como por la base de datos.

---

## Fase 4 — Sesiones y seguridad operativa (P1)

**Confirmado en código (2026-08-28) — ya resuelto, retirado de tareas:** el backoffice
(`ApiClient.ts` + `guards.ts`) ya hace refresh silencioso antes de redirigir a login, y
Flutter ya restaura sesión con cookie `Secure/HttpOnly` + `/auth/refresh`
(`api_client.dart`, `secure_cookie_storage.dart`).

**Resuelto (2026-08-29) — Expiración de buckets del rate limiter:**
`InMemoryLoginRateLimiter` guardaba un `Bucket` por cada IP y por cada hash de
username visto, sin eliminarlo nunca — el mapa crecía sin límite con el tráfico
normal (o más rápido si alguien rota IPs/usuarios a propósito). Se agregó
`limpiarBucketsLlenos()` (`@Scheduled`, cada 10 min por defecto —
`app.security.rate-limit.login.cleanup-interval`) que recarga cada bucket contra el
reloj actual y elimina los que ya volvieron a su capacidad completa — un bucket
lleno es indistinguible de uno recién creado, así que borrarlo es seguro; la
siguiente solicitud simplemente crea uno idéntico. La sustitución por un almacén
compartido (Redis) sigue pendiente pero solo aplica si el backend llega a correr en
múltiples instancias — no es el caso actual, se mantiene como tarea separada.
Cubierto por `InMemoryLoginRateLimiterTest` (nuevo): un bucket que aún no se
recargó no se elimina; uno que se recargó por completo sí, y una solicitud
posterior ve capacidad nueva.

**Resuelto (2026-08-29) — Cambio de contraseña y restablecimiento administrativo:**
`POST /api/v1/auth/password` (autoservicio, requiere la contraseña actual, valida la
nueva contra `PasswordPolicy`) y `POST /api/v1/usuarios/{usuarioId}/password/restablecer`
(admin, permiso nuevo `USUARIOS_RESTABLECER_PASSWORD`). El restablecimiento
administrativo genera una contraseña temporal aleatoria (`TemporaryPasswordGenerator`,
20 caracteres sin ambiguos) y la devuelve una sola vez en la respuesta — nunca se
persiste en claro ni se audita. Ambos caminos revocan todas las sesiones (refresh
tokens) del usuario. Se agregó `usuario.debe_cambiar_password` (columna nueva,
`Usuario.restablecerConPasswordTemporal` la marca, `cambiarPassword` la limpia); el
login (`/auth/login`, `/auth/refresh`) la incluye como `debeCambiarPassword` en la
respuesta para que el frontend fuerce la pantalla de cambio.

**Decisión de alcance tomada en esta pasada:** originalmente se buscaba que el backend
bloqueara *todo* el resto de la API mientras `debe_cambiar_password` esté activo. Al
investigar cómo hacerlo se encontró que el mecanismo del que dependía — revalidar por
peteción contra la BD (versión de seguridad/`sver`, documentado en
seguridad-desarrolladores.md §5 como si ya existiera) — **nunca se implementó**; un
access token vigente sigue siendo válido hasta su `exp` sin importar qué le pase a
la cuenta después. Construirlo desde cero excedía el alcance de esta tarea y se
solapa con "permitir revocar sesiones activas" (tarea separada de esta misma fase),
así que se bajó el alcance a solo la señal (`debeCambiarPassword` en la respuesta de
login) — el bloqueo real queda pendiente, documentado en el hallazgo de abajo.
Cubierto por `UsuarioTest`, `AuthServiceImplTest`, `UsuarioServiceImplTest`,
`AuthControllerTest`, `UsuarioControllerTest` y `TemporaryPasswordGeneratorTest`.

**Resuelto (2026-08-29) — Revalidación por petición (`sver`) y revocar sesiones:**
implementado el mecanismo que la decisión de alcance de arriba había dejado
pendiente. El access token ahora lleva dos claims nuevos, `sver`
(`Usuario.versionSeguridad` al emitir) y `debeCambiarPassword`
(`AccessTokenIssuerImpl`). `SecurityVersionValidator` (nuevo
`OAuth2TokenValidator<Jwt>`, registrado en `JwtConfig.defaultValidators`) revalida
`sver` contra la BD en **toda petición autenticada** — a diferencia de
`PermissionInterceptor`, que solo corre en endpoints anotados
`@RequiresPermission` — y rechaza el token con `401` si la versión no coincide, el
usuario no existe o no está activo; cualquier cambio de contraseña,
bloqueo/desactivación o revocación de sesiones invalida así de inmediato los access
tokens ya emitidos, sin esperar su `exp`. `DebeCambiarPasswordFilter` (nuevo,
registrado tras `BearerTokenAuthenticationFilter` en `SecurityConfig`) lee el claim
`debeCambiarPassword` y bloquea con `403 DEBE_CAMBIAR_PASSWORD` cualquier ruta que no
sea `/api/v1/auth/password`, `/logout` o `/me` — cierra el bloqueo real que había
quedado pendiente arriba. Nuevo endpoint
`POST /api/v1/usuarios/{usuarioId}/sesiones/revocar` (permiso
`USUARIOS_REVOCAR_SESIONES`, solo ADMIN, migración `013-seed-permiso-revocar-sesiones.xml`)
expone "revocar sesiones activas de otro usuario" sin tocar su contraseña ni su
estado — nuevo método de dominio `Usuario.revocarSesiones()` (solo sube
`versionSeguridad`) + `UsuarioServiceImpl.revocarSesiones` (revoca también todos sus
refresh tokens, mismo patrón que `cambiarMiPassword`/`restablecerPassword`). Cubierto
por `UsuarioTest`, `UsuarioServiceImplTest`, `UsuarioControllerTest` (nuevos casos) y
`SecurityVersionValidatorTest`/`DebeCambiarPasswordFilterTest` (nuevos, unitarios).
Documentado en `seguridad-desarrolladores.md` §5. Verificado con `mvn verify` contra
Postgres real (Testcontainers): 564 unitarios + 24 IT, `BUILD SUCCESS` — el contexto
Spring arranca correctamente con `SecurityVersionValidator` inyectado en `JwtConfig`.

**Resuelto (2026-08-29) — Cabeceras de seguridad:** `deploy/Caddyfile` (borde público)
y `market-backoffice/nginx.conf` (defensa en profundidad si se accede al contenedor
sin pasar por Caddy) ahora agregan `Strict-Transport-Security`,
`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
`Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` (geolocation/
cámara/micrófono/pagos deshabilitados) y `Content-Security-Policy` (`default-src
'self'`, permitiendo `fonts.googleapis.com`/`fonts.gstatic.com` para Plus Jakarta
Sans, que el backoffice carga vía `@import` en `main.css`). Sin probar en un
despliegue real (solo revisado por configuración) — verificar que Google Fonts siga
cargando y que ninguna feature del backoffice dependa de un origen no listado en el
CSP antes de producción.

**Resuelto (2026-08-31):** decisiones acordadas con el usuario — MFA solo evaluado y
documentado (no implementado, es alcance de una fase aparte); llaves de dev/test
retiradas del tracking de git (sin reescribir historial — son solo de dev/test,
riesgo bajo, y `ProdSafetyGuard` ya rechaza cualquier llave `dev-*` en `prod`).

Llaves JWT de dev (`local-dev/certs/dev-*.pem`) y de test
(`src/test/resources/certs/test-*.pem`) sacadas del tracking (`git rm --cached`,
siguen en disco) — nuevo `.gitignore` para ambas rutas, nuevo script
`generar-llaves.sh` en cada carpeta (genera el par si no existe, mismo comando
`openssl` que ya estaba documentado). `.github/workflows/ci.yml` corre el script
de test antes de `mvn verify` — sin esto el primer push habría roto CI (el archivo
ya no estaría en el checkout). Verificado localmente: borré los `.pem` de test,
corrí el script, corrí `mvn test` — 564 tests, limpio.

Rotación de llaves JWT: confirmado que el mecanismo YA existe en `JwtConfig`
(`activeKid` elige qué llave firma tokens nuevos, `keys` es una lista — todas
validan) — solo faltaba una prueba real. Nuevo `JwtRotacionTest` (sin Spring
context, RSA en memoria): confirma que durante una rotación (2 llaves en la
lista) un token firmado con la vieja sigue validando, y que al retirar la vieja
de la lista (rotación completa) sus tokens pasan a rechazarse.

Política de contraseña/bloqueo/recuperación/baja documentada en
`seguridad-desarrolladores.md` §13 — casi todo ya existía (contraseña, bloqueo
temporal por rate limit, restablecimiento admin), pero **"baja de empleados" no
tenía ningún endpoint real**: `Usuario.desactivar()`/`bloquear()`/`activar()`
existían en el dominio desde antes de esta fase pero nunca estuvieron expuestos
por HTTP (confirmado auditando `UsuarioController` — cero rutas). Se agregaron
`POST /api/v1/usuarios/{id}/desactivar`, `/bloquear`, `/activar` (permiso único
`USUARIOS_CAMBIAR_ESTADO`) — ambas transiciones (desactivar/bloquear) revocan
sesiones activas de inmediato, igual que `revocarSesiones` ya hacía. De paso se
encontró y corrigió un bug en `SecurityAuditPublisherImpl` (Fase 7): `entidadId`
quedaba con el id del actor (el admin que ejecuta la acción) en vez del usuario
objetivo — visible recién al probar con actor≠objetivo (bloquear a otro usuario),
antes todos los casos probados tenían actor=objetivo por casualidad.

Verificado: `mvn verify` (574 unitarios + 24 IT, `BUILD SUCCESS`, incluye 10 tests
nuevos: 2 de rotación, 8 de las 3 transiciones de estado). En vivo contra
backend+Postgres reales: creé un usuario de prueba, hizo login, lo bloqueé como
admin, y el MISMO token (todavía sin expirar) pasó a devolver 401 de inmediato
— confirma que `sver` invalida sin esperar el TTL. Reactivé y volvió a poder
loguearse. `GET /api/v1/auditoria` mostró `USUARIO_BLOQUEADO`/`USUARIO_DESACTIVADO`
con `actorId`/`entidadId` correctos tras el fix.

**Pendiente, no resuelto en esta pasada:**
- "Conservar o volver a solicitar la tienda activa de forma segura tras
  restaurar" — es un comportamiento de los clientes (Flutter/backoffice), no del
  backend; requiere probar en esos proyectos, fuera de alcance de esta pasada.
- Rate limiter distribuido — confirmado que sigue sin aplicar hoy (instancia
  única).
- Protección contra que un admin se desactive/bloquee a sí mismo, o contra
  quedarse sin ningún admin activo — documentado como límite conocido en
  `seguridad-desarrolladores.md` §13, no implementado (bajo riesgo hoy, equipos
  chicos).

### Tareas

- [ ] Conservar o volver a solicitar la tienda activa de forma segura tras restaurar
  — comportamiento de cliente (Flutter/backoffice), fuera de alcance de esta pasada.
- [x] Implementar cambio de contraseña y flujo administrativo de restablecimiento
  (2026-08-29 — ver detalle arriba; el bloqueo server-side por `debe_cambiar_password`
  ya no está pendiente, ver el hallazgo de `sver` justo arriba).
- [x] Permitir revocar sesiones/dispositivos activos de un usuario. Incluye la
  revalidación por petición (versión de seguridad/`sver`) que
  seguridad-desarrolladores.md §5 documentaba como ya implementada y no lo estaba —
  resuelto 2026-08-29, ver detalle arriba.
- [x] Evaluar MFA para administradores y auditores — solo evaluación/documentación,
  ver `seguridad-desarrolladores.md` §14; no implementado (decisión del usuario).
- [ ] Sustituir o complementar el rate limiter en memoria con un almacén compartido si
  habrá múltiples instancias (sigue pendiente; no aplica hoy — instancia única).
- [x] Agregar expiración/limpieza a los buckets del rate limiter para evitar crecimiento
  indefinido (2026-08-29 — ver detalle arriba).
- [x] Retirar la llave privada de desarrollo del control de versiones y generar llaves
  locales mediante script/documentación — hecho para dev y test (2026-08-31, ver
  detalle arriba); no se reescribió el historial de git (decisión del usuario, son
  solo llaves de dev/test, riesgo bajo).
- [x] Probar rotación de llaves JWT manteniendo temporalmente validación de la
  anterior — 2026-08-31, `JwtRotacionTest`, ver detalle arriba.
- [x] Revisar cabeceras CSP, `X-Content-Type-Options`, `Referrer-Policy` y permisos del
  navegador en Caddy/Nginx (2026-08-29 — ver detalle arriba).
- [x] Definir política de contraseña, bloqueo, recuperación y baja de empleados —
  2026-08-31, `seguridad-desarrolladores.md` §13; "baja de empleados" además pasó
  de dominio-sin-endpoint a 3 rutas HTTP reales, ver detalle arriba.

### Criterio de aceptación

Una sesión válida sobrevive un reinicio normal del cliente sin exponer tokens; una
sesión revocada no puede renovarse y los controles funcionan en todas las instancias.

---

## Fase 5 — Pruebas críticas y CI/CD (P1)

**Confirmado en código (2026-08-28):** no existe `.github/workflows/` ni pipeline
equivalente (toda la fase sigue pendiente). El Dockerfile del backend sí usa
`-DskipTests`. La versión de Java está fijada (`pom.xml` + Dockerfile en Java 25), pero
falta Maven Wrapper, `packageManager`/Corepack en el backoffice y FVM en Flutter.

**Resuelto (2026-08-30) — "Pipeline mínimo" completo, "Cobertura prioritaria"
sigue pendiente (ver abajo):** nuevo `.github/workflows/ci.yml` con 5 jobs —
`backend` (`./mvnw -B verify`, cubre unitarios + `*IT` con Testcontainers/Postgres
real + empaquetado del jar en un solo comando; `LiquibaseMigrationIT` ya valida
desde base vacía dentro de ese mismo `verify`), `backoffice` (`pnpm install
--frozen-lockfile` → `typecheck` → `test` → `build`), `flutter` (`pub get` →
regenera código de Isar y falla si queda diff sin commitear → `analyze` →
`test` → `build web` de humo; en `main` además `build apk --release`, subido
como artifact — sin keystore propio, firma con la debug key existente,
suficiente para build/smoke, no para publicar), `docker-build` (con
`needs: [backend, backoffice]`, nunca corre si algo falló antes — construye
las dos imágenes reales y corre Trivy en modo informativo sobre ambas) y
`gitleaks` (escaneo de secretos, independiente). `-DskipTests` se mantuvo en
el `Dockerfile` del backend a propósito — la vía elegida por el plan fue
"garantizar que la imagen dependa de un job de pruebas exitoso" (`needs`),
no duplicar los tests dentro del build de Docker.

Fijado en el mismo pase: Maven Wrapper (`market-backend/mvnw`/`mvnw.cmd`/`.mvn/`,
pinneado a Maven 3.9.12; también se agregó `market-backend/.gitattributes`
forzando `eol=lf` en `mvnw` y el bit ejecutable en el índice de git —
`core.fileMode=false` en este repo lo perdía silenciosamente, lo que habría
roto `./mvnw` en el runner Linux con "Permission denied"), `packageManager`/
`engines` en `market-backoffice/package.json` (`pnpm@11.15.1`, Node `>=20`),
y `market-flutter/.fvmrc` (`3.38.8`, coincide con la versión ya instalada y
con el constraint `^3.10.7` de Dart). Escaneo de dependencias vía nuevo
`.github/dependabot.yml` (`maven`, `npm`, `pub`, `docker` × 2, `github-actions`
— `pub` confirmado como ecosistema soportado por Dependabot antes de usarlo).

Verificado localmente (no se puede correr GitHub Actions sin `act`, que no
está instalado — la prueba real es el primer push): `./mvnw -B verify`
(`BUILD SUCCESS`, incluye los 24 IT existentes), `pnpm typecheck && pnpm test
&& pnpm build` (build de producción no necesitó `.env` — confirma que
`VITE_API_BASE_URL` dummy alcanza), `flutter analyze`/`flutter test`/`flutter
build web` (limpios; los warnings de compatibilidad Wasm son informativos,
no bloquean un build JS normal), y `docker build` de ambas imágenes reales
(backend y backoffice) fuera de CI, sanity-check antes de commitear.

### Pipeline mínimo

- [x] Crear `.github/workflows/ci.yml` o equivalente.
- [x] Backend:
  - [x] `mvn test`;
  - [x] `mvn verify` con PostgreSQL/Testcontainers;
  - [x] compilación del jar;
  - [x] validación Liquibase desde una base vacía — cubierta por
    `LiquibaseMigrationIT`, ya corre dentro de `mvn verify`.
  - [ ] validación Liquibase desde una versión anterior — sin mecanismo hoy
    (no hay tags de Liquibase ni snapshot de un release previo); queda
    pendiente, no se inventó nada nuevo en esta pasada.
- [x] Backoffice:
  - [x] instalación con lockfile;
  - [x] typecheck;
  - [x] tests;
  - [x] build de producción.
- [x] Flutter:
  - [x] `flutter analyze`;
  - [x] `flutter test`;
  - [x] build APK release al menos en ramas de entrega — solo en `main`,
    firmado con la debug key (sin keystore propio todavía, ver Fase 9).
- [x] Construcción de imágenes Docker solo después de pasar pruebas (job
  `docker-build` con `needs: [backend, backoffice]`).
- [x] Eliminar `-DskipTests` del camino de release o garantizar que la imagen
  dependa de un job de pruebas exitoso — se optó por la segunda opción
  (`needs`), `-DskipTests` se mantuvo en el `Dockerfile` a propósito para no
  correr los tests dos veces.
- [x] Agregar escaneo de secretos (Gitleaks), dependencias (Dependabot) e
  imágenes (Trivy, informativo — no bloquea por CVEs de imagen base que el
  equipo no puede parchear todavía).
- [x] Fijar versiones de herramientas:
  - [x] Maven Wrapper;
  - [x] versión Java (ya estaba fija, sin cambios);
  - [x] `packageManager`/Corepack para pnpm;
  - [x] FVM (`.fvmrc`) o versión Flutter documentada.

### Cobertura prioritaria

- [ ] E2E: login -> abrir caja -> vender -> verificar inventario/caja.
- [ ] E2E: compra -> recibir -> inventario -> cuenta por pagar.
- [ ] E2E: crédito -> cobro -> caja -> saldo.
- [ ] E2E: traslado entre tiendas.
- [ ] E2E: permisos y aislamiento entre tiendas/grupos.
- [ ] Tests Flutter para carrito, checkout, cola, refresh, caja y parsers.
- [ ] Tests Vue para guards, refresh, permisos y principales composables.
- [ ] Pruebas contractuales de DTOs entre backend y clientes.

### Criterio de aceptación

Ningún artefacto desplegable se produce si falla una prueba, migración, análisis o
build de cualquiera de los tres componentes afectados.

---

## Fase 6 — Backups, restauración y continuidad (P1)

**Confirmado en código (2026-08-28):** existe `deploy/backup/backup.sh` (`pg_dump` +
gzip con retención por días), pero sin cifrado, sin subida a almacenamiento externo, sin
checksum y sin alertas reales (solo `echo` a stdout). La restauración está documentada
en `deploy/README.md`, pero no automatizada ni ensayada. Toda la fase sigue pendiente.

**Resuelto (2026-08-30):** decisiones acordadas con el usuario — storage externo
Google Cloud Storage (el bucket todavía no existe, quedan los comandos `gcloud`
exactos documentados para crearlo), alertas por correo (SMTP genérico, sin
proveedor específico todavía), RPO ≤24h/RTO "unas horas" (el intervalo diario
actual ya alcanza). `deploy/backup/backup.sh` reescrito: el dump local a
`/backups` sigue igual que antes (retención por `BACKUP_RETENTION_DAYS`); si
`GCS_BUCKET`/`BACKUP_ENCRYPTION_PASSPHRASE` están configurados (vacíos por
default, sin ellos sigue funcionando solo-local), arma un bundle con el dump +
imágenes de producto (`productos_imagenes`, mount nuevo `:ro`) + certs
(`deploy/certs/*.pem`, mount nuevo `:ro`) + `.env` (mount nuevo `:ro`), lo cifra
simétrico AES256 (`gpg`) con la passphrase, genera un `.sha256`, y sube ambos a
GCS con `rclone` (paquete Alpine nuevo — preferido sobre hablar la REST API de
GCS a mano porque ya verifica el hash después de subir). `rclone` toma
credenciales de la cuenta de servicio adjunta a la VM vía el metadata server de
GCE (`RCLONE_CONFIG_GCS_ENV_AUTH=true`) — nunca una llave JSON estática en el
servidor. Cualquier fallo en cualquier paso dispara una alerta por correo
(`alert.sh`, vía `msmtp`, con fallback a solo-log si no hay SMTP configurado) y
`exit 1`. Nuevo `check-freshness.sh` (corrido al inicio de cada vuelta de
`loop.sh`) alerta si el dump local más reciente supera `BACKUP_MAX_AGE_HOURS`
(default 30h). Nuevo `restore.sh`: descarga el bundle más reciente (o uno por
nombre), verifica checksum, descifra, y restaura — la base de datos y las
imágenes de producto se aplican directo; certs y `.env` del bundle NO se
sobreescriben solos, quedan guardados en `deploy/backups/` para revisión manual
(demasiado sensibles para aplicar sin supervisión). Nuevo
`.github/workflows/backup-restore-drill.yml` (cron mensual +
`workflow_dispatch`) automatiza el ensayo de restauración contra un Postgres
descartable con sanity check de conteo de filas — requiere 3 secrets nuevos en
GitHub que el usuario debe crear (documentados en `deploy/README.md`, incluidos
los comandos `gcloud` para la cuenta de servicio de solo lectura que usa ese
workflow). `deploy/README.md` documenta RPO/RTO, los comandos `gcloud` exactos
para crear el bucket (con versioning + lifecycle de ejemplo) y la cuenta de
servicio de escritura de la VM, y el flujo completo de restauración.

Verificado de punta a punta contra un Postgres descartable (usando el backend
`local` de `rclone` como reemplazo de GCS real, sin necesitar credenciales de
nube): `backup.sh` armó el bundle cifrado con dump+imágenes+certs+`.env` de
prueba y lo "subió" verificado; `restore.sh` lo descargó, verificó checksum,
descifró, y restauró — los datos, imágenes, certs y `.env` de prueba volvieron
exactamente iguales. `check-freshness.sh` y `alert.sh` probados en sus 3
escenarios (sin backups, backup fresco, backup viejo) — sin SMTP configurado,
caen correctamente a solo-log. `docker compose config` valida limpio con los
mounts/env nuevos.

**Pendiente, requiere que el usuario actúe de su lado (no accesible desde
acá):** crear el bucket GCS + cuenta de servicio real + adjuntarla a la VM
(comandos ya listos); conseguir credenciales SMTP reales; llenar el `.env` real
del servidor con `GCS_BUCKET`/`BACKUP_ENCRYPTION_PASSPHRASE`/`ALERT_SMTP_*`
(y guardar la passphrase en un vault fuera del servidor — crítico, sin eso los
backups en GCS quedan inservibles si se pierde el servidor); agregar los 3
secrets del workflow mensual en GitHub. También pendiente, no ejecutado por mí:
probar recuperación con el volumen Docker completo perdido contra una copia de
prueba real del servidor (el pipeline en sí ya se verificó de punta a punta,
falta el ensayo contra disco/VM real).

### Tareas

- [x] Mantener el dump PostgreSQL actual, pero copiarlo cifrado a almacenamiento
  externo y versionado (bucket con `--versioning`, comandos documentados).
- [x] Respaldar también:
  - [x] imágenes de productos;
  - [x] configuración necesaria para reconstruir el entorno (`.env`);
  - [x] certificados mediante su mecanismo seguro (cifrado dentro del mismo
    bundle GPG, no en texto plano);
  - [ ] volúmenes relevantes de Caddy — no incluido (`caddy_data` solo tiene el
    certificado TLS de Let's Encrypt, se re-emite solo al desplegar de nuevo;
    no es pérdida de datos de negocio, se dejó fuera de alcance).
- [x] Generar checksum por backup y verificarlo después de subirlo (`.sha256`
  + verificación propia de `rclone copyto`).
- [x] Alertar cuando falle un backup o no exista uno reciente (`alert.sh` +
  `check-freshness.sh`).
- [x] Definir RPO y RTO del negocio (RPO ≤24h, RTO unas horas — ver
  `deploy/README.md`).
- [x] Documentar y automatizar restauración en un ambiente aislado (`restore.sh`).
- [x] Ejecutar una restauración programada al menos mensualmente — automatizado
  vía GitHub Actions (`backup-restore-drill.yml`), no solo un recordatorio de
  calendario; falta que el usuario agregue los 3 secrets para que corra en real.
- [ ] Probar recuperación cuando el volumen Docker completo se pierde — pendiente
  contra un servidor/VM real, no automatizable desde acá sin supervisión.
- [x] Definir rollback de aplicación y compatibilidad de migraciones — ya
  cubierto en la sección "Rollback" existente de `deploy/README.md`, solo
  revisado, no reescrito.

### Criterio de aceptación

Se puede reconstruir un ambiente nuevo con datos, imágenes y configuración dentro del
RTO definido, usando una copia que no dependa del servidor original.

---

## Fase 7 — Auditoría y observabilidad (P1)

**Confirmado en código (2026-08-28):** `docs/auditoria.md` describe un subsistema
completo (outbox, `AuditEventProcessor`, tablas `AUDIT_EVENT`/`AUDIT_OUTBOX`,
endpoints `/api/v1/audit/*`) que NO existe en el código — cero coincidencias. Solo hay
un logger simple (`SecurityAuditPublisher`). `micrometer` core está presente (vía
Actuator) pero sin `micrometer-registry-prometheus` ni endpoint expuesto. El
correlation ID solo se usa en respuestas de error, no propagado por MDC/filtro a logs
generales ni a respuestas exitosas. Toda la fase sigue pendiente.

**Resuelto (2026-08-31):** decisiones acordadas con el usuario — observabilidad
acotada a solo exponer `/actuator/prometheus` (sin montar Prometheus/Grafana en la
VM todavía), alertas por el mismo canal de correo ya armado en Fase 6
(`ALERT_SMTP_*`/`ALERT_EMAIL_*`). `docs/auditoria.md` reescrito por completo — el
outbox/BD-separada/poller descrito nunca existió en código; se optó por la salida
que el propio documento permitía ("reducir el diseño a una primera versión
realista"): escritura directa en `audit_event`, misma transacción que la operación
de negocio, sin outbox.

Nuevo módulo `auditoria` (tabla `audit_event`, **append-only** — mismo trigger
`BEFORE UPDATE OR DELETE` que ya protege `movimiento_inventario` — con
`actor_id`/`actor_username`/`tienda_id`/`accion`/`entidad`/`entidad_id`/
`resultado`/`correlation_id`/`detalle`; lectura vía `GET /api/v1/auditoria` y
`GET /api/v1/auditoria/tiendas/{tiendaId}`, permiso `AUDITORIA_VER` sembrado para
`ADMIN` y `AUDITOR`). Nueva anotación `@Auditable` + un solo `AuditoriaAspect`
(Spring AOP — primer uso en el proyecto, requirió `spring-boot-starter-aspectj`,
el reemplazo de `spring-boot-starter-aop` en Spring Boot 4.x, confirmado al
fallar la build con el nombre viejo) aplicada a 19 métodos en 8 servicios
(`ProductoTiendaServiceImpl`, `InventarioServiceImpl`, `CajaServiceImpl`,
`VentaServiceImpl`, `CompraServiceImpl`, `CuentaPorPagarServiceImpl`,
`TrasladoServiceImpl`, `FelServiceImpl`) — cubre 8 de las 9 categorías pedidas;
"exportaciones de reportes" queda fuera porque esa función no existe todavía
(`ReporteController` solo devuelve JSON, ningún endpoint de exportación — alcance
de Fase 10, no inventado acá). `login/logout/refresh/bloqueo` y
`usuarios/roles/asignaciones` se cubrieron extendiendo `SecurityAuditPublisherImpl`
(ya tenía 14 call sites en `AuthServiceImpl`/`UsuarioServiceImpl` — **cero cambios**
en esos call sites) para que, además de logear/contar como siempre, también
persista en `audit_event` y dispare alerta por correo en los dos tipos de
severidad alta. Se cerró el hueco encontrado de `RATE_LIMIT_ALCANZADO` (declarado
en el enum, nunca disparado) agregándolo en `GlobalExceptionHandler.handleRateLimit`.

Nuevo `CorrelationIdFilter` (registrado con `Ordered.HIGHEST_PRECEDENCE`, fuera de
la cadena de Spring Security, para cubrir hasta un 401/429) pone el
`correlationId` en MDC en cada request — aparece en toda línea de log
(`logging.pattern.level`) y como header en toda respuesta, no solo errores.
`GlobalExceptionHandler`/`SecurityAuditPublisherImpl`/`AuditoriaAspect` lo leen de
ahí en vez de generar cada uno el suyo. `micrometer-registry-prometheus` agregado,
`/actuator/prometheus` expuesto con el mismo alcance de red que `/health`
(`127.0.0.1:8080`). Nuevo `AlertaEmailService` (`spring-boot-starter-mail`,
mismas env vars `ALERT_SMTP_*`/`ALERT_EMAIL_*` de Fase 6) — nunca rompe el flujo
que la disparó, cae a solo-log sin SMTP configurado.

**Dos bugs encontrados y corregidos durante la verificación en el backend local
real (no en `mvn verify`, que no los detecta):** (1) `LOGIN_EXITOSO`/eventos de
login-refresh quedaban con `actorUsername="anonymousUser"` en vez del usuario
real — `AnonymousAuthenticationToken` de Spring Security reporta
`isAuthenticated()==true` igual que una sesión real, así que la resolución de
actor tomaba la rama equivocada; corregido excluyéndolo explícitamente en
`SecurityAuditPublisherImpl` y `AuditoriaAspect`. (2) Agregar
`spring-boot-starter-mail` activó un `MailHealthIndicator` automático que
intenta una conexión SMTP real en cada chequeo — `/actuator/health` pasó a
devolver `DOWN` solo porque no había SMTP configurado, sin relación con si el
backend en sí estaba sano; corregido con `management.health.mail.enabled: false`.

Verificado: `mvn verify` (564 unitarios + 24 IT, `BUILD SUCCESS`, con los 21
tests que instanciaban `GlobalExceptionHandler` a mano actualizados para su
constructor nuevo). En vivo contra el backend local + Postgres real: creé un
producto y lo asigné a una tienda (`PRODUCTO_TIENDA_ASIGNADO` con actor/tienda/
entidadId correctos vía `@Auditable`), y reproduje una reutilización real de
refresh token (`REFRESH_REUTILIZADO`, `resultado=FALLO`, alerta cayendo a
solo-log sin romper la respuesta) — ambos aparecieron correctos en
`GET /api/v1/auditoria`, con el mismo `correlationId` de la request en cada uno.
Confirmé el header `X-Correlation-Id` en una respuesta exitosa (antes solo en
errores) y `/actuator/prometheus` devolviendo métricas reales con JWT.

**Pendiente, requiere que el usuario decida infraestructura (no completable
desde acá):** dashboards reales (Grafana u otro) y alertas basadas en umbrales
de métricas (latencia HTTP, tasa de error) — dependen de que se monte
Prometheus/Grafana, decisión explícitamente pospuesta esta pasada.

### Auditoría

- [x] Corregir `docs/auditoria.md` para separar claramente diseño futuro de
  funciones existentes mientras se implementa — reescrito por completo,
  describe el diseño real (sin outbox), no lo aspiracional.
- [x] Implementar el outbox operativo descrito o reducir el diseño a una primera
  versión realista y durable — se optó por la segunda opción, documentado el
  porqué en `docs/auditoria.md` §1.
- [x] Auditar, como mínimo:
  - [x] login, logout, refresh reutilizado y bloqueo;
  - [x] cambios de usuarios, roles y asignaciones;
  - [x] precios y configuración por tienda;
  - [x] ajustes de inventario;
  - [x] apertura, movimientos y cierre de caja;
  - [x] ventas y anulaciones (devoluciones no existen como función todavía —
    Fase 10 — nada que auditar ahí);
  - [x] compras, pagos y traslados;
  - [x] emisión/anulación FEL;
  - [ ] exportaciones de reportes — la función no existe todavía (Fase 10), no
    hay dónde enganchar la auditoría.
- [x] Registrar actor, tienda, fecha, acción, entidad, resultado y correlation ID
  sin almacenar contraseñas, tokens ni cuerpos sensibles.
- [x] Proteger la auditoría contra modificación (trigger append-only, mismo
  patrón que `movimiento_inventario`) y aplicar retención definida — retención
  definida como "indefinida, purga manual de DBA si algún día hace falta" en
  vez de un borrado automático que necesitaría deshabilitar la protección
  recién construida (ver `docs/auditoria.md` §3).

### Observabilidad

- [x] Agregar registry Prometheus u otro backend real de métricas
  (`micrometer-registry-prometheus`).
- [x] Exponer métricas en una red administrativa protegida — mismo alcance que
  `/health` (`127.0.0.1:8080` únicamente).
- [ ] Crear dashboards — pospuesto por decisión del usuario, sin
  Prometheus/Grafana montado todavía.
- [x] Agregar correlation ID extremo a extremo en backend (`CorrelationIdFilter`
  + MDC + header en toda respuesta) — clientes (Flutter/Vue) quedan fuera de
  esta pasada, es trabajo del lado backend únicamente.
- [x] Configurar alertas accionables y un runbook por alerta — para los dos
  tipos implementables sin Prometheus (`REFRESH_REUTILIZADO`,
  `RATE_LIMIT_ALCANZADO`); alertas basadas en umbrales de métricas quedan
  pendientes de la decisión de infraestructura de arriba.

### Criterio de aceptación

Es posible reconstruir quién realizó una operación crítica y detectar fallos antes de
que una tienda los reporte manualmente.

---

## Fase 8 — Calidad del backoffice Vue (P2)

**Confirmado en código (2026-08-28):** no hay ESLint/Prettier configurado (lo declara
el propio `CLAUDE.md` del repo). Vistas grandes reales:
`DashboardView.vue` 521 líneas, `UsuariosView.vue` 439, `ProductosView.vue` 382,
`VentasView.vue` 336, `CajaView.vue` 307. Casi no hay componentes reutilizables (solo
`EstadoBadge.vue`). No hay cancelación de requests real (el mapeo de error para
`ERR_CANCELED` existe pero `ApiClient` nunca pasa `signal`). No hay Playwright. El
refresh de sesión solo reacciona a un 401, no es proactivo al montar la app. Toda la
fase sigue pendiente.

### Tareas

- [ ] Agregar ESLint, Prettier y chequeos en CI.
- [ ] Dividir vistas grandes (`Dashboard`, `Usuarios`, `Productos`, `Ventas`, `Caja`)
  en componentes de responsabilidad única.
- [ ] Crear componentes reutilizables para:
  - tabla paginada;
  - filtros;
  - modal y confirmación;
  - campos de dinero/fecha;
  - estados de carga, vacío y error;
  - formularios y errores de validación.
- [ ] Evaluar una librería de validación de formularios o una convención interna común.
- [ ] Normalizar cancelación de requests y evitar respuestas obsoletas al cambiar
  filtros rápidamente.
- [ ] Agregar accesibilidad: navegación por teclado, foco, etiquetas, contraste y
  anuncios de error.
- [ ] Implementar refresh silencioso al cargar la aplicación.
- [ ] Agregar E2E con Playwright para flujos administrativos críticos.
- [ ] Mantener texto en español, pero centralizarlo si se prevé personalización o i18n.

### Criterio de aceptación

Las vistas críticas son mantenibles, accesibles y tienen pruebas de interacción, no
solo pruebas aisladas de servicios/composables.

---

## Fase 9 — Calidad del POS Flutter (P2)

**Confirmado en código (2026-08-28):** `flutter analyze` y `flutter test` ya corren
sin problema ("No issues found!", 1 test pasa) — el punto de investigar por qué no
terminaban ya no reproduce, retirado. `pos_screen.dart` tiene 826 líneas (confirmado
grande). Solo existe `test/widget_test.dart` (smoke test de login); no hay tests de
carrito, checkout, parsers, refresh de auth ni sincronización. No hay flavors
(`dev`/`staging`/`prod`); el ambiente se maneja vía `--dart-define`.

### Tareas

- [ ] Dividir `pos_screen.dart` (826 líneas) y pantallas grandes en
  widgets/controladores pequeños.
- [ ] Agregar tests unitarios para:
  - carrito y redondeos;
  - checkout por método de pago;
  - serialización y parsers;
  - autenticación y refresh;
  - sincronización y clasificación de errores;
  - caja y cuentas por cobrar.
- [ ] Agregar pruebas de widgets para selector de tienda, cobro y pendientes (login ya
  tiene una prueba básica).
- [ ] Implementar pruebas de integración en Android.
- [ ] Definir versión, firma, flavors (`dev`, `staging`, `prod`) y distribución del APK.
- [ ] Validar impresión de ticket, reimpresión y selección de impresora en hardware
  real.
- [ ] Probar cámara/lector, teclado físico y distintos tamaños de tablet.
- [ ] Medir tiempos de arranque, catálogo grande y consumo de memoria.
- [ ] Definir telemetría de errores respetuosa de datos personales.

### Criterio de aceptación

El POS puede operar un turno completo en una tablet real, incluida pérdida de red,
reinicio de app, sincronización, impresión y cierre de caja.

---

## Fase 10 — Funciones comerciales prioritarias (P2)

Estas funciones deben priorizarse con usuarios reales antes de implementarlas.

**Confirmado en código (2026-08-28):** ninguna de estas funciones está implementada
como flujo completo. Existen únicamente tipos de movimiento de kardex sueltos sin
servicio/entidad propia detrás: `TipoMovimiento.DEVOLUCION_CLIENTE`,
`DEVOLUCION_PROVEEDOR` y `AJUSTE_NEGATIVO` (mermas), sin vínculo a venta/compra
específica, sin motivo/autorización, sin reversión de CxC/FEL. `MovimientoCaja` ya
distingue `INGRESO`/`EGRESO`, pero sin flujo de aprobación. Todo lo demás (notas de
crédito, descuentos, promociones, impuestos, lotes/vencimiento, conteos físicos,
recepción parcial de compras, caja por terminal, arqueo por denominación,
rentabilidad con costo histórico) no existe en el código.

### Ventas y fiscal

- [ ] Devoluciones y cambios parciales/totales (hoy solo existe el tipo de movimiento
  `DEVOLUCION_CLIENTE`, sin flujo).
- [ ] Notas de crédito FEL y reversión coordinada de inventario, caja y CxC.
- [ ] Descuentos por línea y por venta con permisos/límites.
- [ ] Promociones, combos y precios por vigencia.
- [ ] Impuestos y desglose fiscal explícito.
- [ ] Venta suspendida, cotización o apartado.
- [ ] Ticket, PDF, reimpresión y búsqueda por número.

### Inventario y compras

- [ ] Lotes y fechas de vencimiento para alimentos/productos perecederos.
- [ ] Mermas con motivo, autorización y auditoría (hoy solo existe el tipo de
  movimiento `AJUSTE_NEGATIVO`, sin motivo ni autorización).
- [ ] Conteos físicos y conciliación de inventario.
- [ ] Recepción parcial de compras.
- [ ] Devolución a proveedor (hoy solo existe el tipo de movimiento
  `DEVOLUCION_PROVEEDOR`, sin flujo desde una compra).
- [ ] Reposición sugerida considerando venta histórica, plazo y stock de seguridad.

### Caja y finanzas

- [ ] Caja por terminal/cajero, no únicamente por tienda, si el negocio tendrá varias
  cajas simultáneas.
- [ ] Retiros, depósitos y transferencias de efectivo con aprobación (hoy solo hay
  ingreso/egreso simple).
- [ ] Arqueo y explicación de diferencias (hoy solo compara un monto total contado vs.
  esperado, sin desglose por denominación).
- [ ] Conciliación de tarjeta y transferencia.
- [ ] Rentabilidad incluyendo mermas, gastos y costo histórico.
- [ ] Exportación o integración contable.

### Criterio de aceptación

Cada función debe tener reglas acordadas con usuarios, tratamiento contable/fiscal,
permisos, auditoría y escenarios de reversión antes de programarse.

---

## Fase 11 — Rendimiento y escalado (P3)

**Confirmado en código (2026-08-28):** productos, ventas y clientes ya paginan
server-side (`Pageable`/`Page`), ya resuelto para esos tres listados. No se encontró el
patrón O(n) descrito para CxC (tampoco existe un `findByVentaId` directo en
`CuentaPorCobrarRepository`; verificar si algún flujo lo necesita). Argon2id ya está
implementado y es configurable (`PasswordEncoderConfig`), pero no hay configuración
explícita de HikariCP (se usan valores por defecto de Spring Boot) ni medición de costo
real de Argon2.

### Tareas

- [ ] Definir volúmenes esperados: tiendas, productos, tickets/día, líneas/ticket y
  usuarios concurrentes.
- [ ] Probar los listados y dashboards con datos representativos.
- [ ] Revisar si otros listados (no productos/ventas/clientes) siguen sin paginación
  server-side.
- [ ] Verificar si hace falta una consulta directa de CxC por `ventaId` (hoy no existe
  el accesor ni se detectó el antipatrón O(n) descrito).
- [ ] Revisar planes de ejecución e índices con `EXPLAIN ANALYZE`.
- [ ] Medir el costo real de Argon2 (ya configurable) y configurar explícitamente el
  pool de conexiones HikariCP con carga real.
- [ ] Definir estrategia de imágenes: límites, miniaturas, limpieza y almacenamiento
  externo si crece el volumen.
- [ ] Probar múltiples instancias antes de habilitarlas: rate limiting, schedulers,
  outbox, FEL y tareas periódicas deben coordinarse.

### Criterio de aceptación

El sistema cumple objetivos documentados de latencia y throughput con el volumen
esperado y sin romper invariantes al ejecutar varias instancias.

---

## 5. Matriz mínima de pruebas de negocio

| Flujo | Caso normal | Concurrencia | Respuesta perdida | Sin red | Reversión |
| --- | --- | --- | --- | --- | --- |
| Venta contado | [ ] | [ ] | [ ] | [ ] | [ ] |
| Venta crédito | [ ] | [ ] | [ ] | [ ] | [ ] |
| Venta mixta | [ ] | [ ] | [ ] | N/A o [ ] | [ ] |
| Movimiento caja | [ ] | [ ] | [ ] | [ ] | [ ] |
| Cobro CxC | [ ] | [ ] | [ ] | Definir | [ ] |
| Pago CxP | [ ] | [ ] | [ ] | Definir | [ ] |
| Compra/recepción | [ ] | [ ] | [ ] | Definir | [ ] |
| Traslado | [ ] | [ ] | [ ] | Definir | [ ] |
| FEL | [ ] | [ ] | [ ] | [ ] | [ ] |
| Gasto programado | [ ] | [ ] | [ ] | N/A | [ ] |

## 6. Checklist de salida a producción

No autorizar producción hasta completar todos los puntos aplicables:

- [ ] Fases 1, 2 y 3 cerradas.
- [ ] Sesiones restaurables y revocables.
- [ ] CI en verde para los tres proyectos.
- [ ] `mvn verify` exitoso con PostgreSQL real.
- [ ] APK release firmado y probado en tablet real.
- [ ] FEL probado en sandbox y producción controlada.
- [ ] Restauración completa ensayada.
- [ ] Backups externos cifrados y alertados.
- [ ] Métricas, logs y alertas accesibles.
- [ ] Auditoría de operaciones críticas activa.
- [ ] Prueba de permisos entre tiendas y roles.
- [ ] Prueba de carga con volumen esperado.
- [ ] Procedimiento de rollback documentado.
- [ ] Conciliación diaria de inventario, caja, ventas y FEL definida.
- [ ] Manual mínimo para administrador, encargado y vendedor.
- [ ] Responsable y procedimiento de soporte definidos.

## 7. Secuencia sugerida de entregas

1. **Entrega A:** bloquear FEL simulado en producción y diseñar proveedor real.
2. **Entrega B:** UUID/idempotencia para venta online y movimientos de caja.
3. **Entrega C:** locks y restricciones de caja, cuentas y crédito.
4. **Entrega D:** suite concurrente con PostgreSQL y CI obligatorio.
5. **Entrega E:** restauración de sesión y hardening de seguridad.
6. **Entrega F:** backup externo, restore drill, métricas y alertas.
7. **Entrega G:** auditoría durable.
8. **Entrega H:** refactor y pruebas profundas de Vue/Flutter.
9. **Entrega I:** funciones comerciales priorizadas con usuarios.

## 8. Registro de decisiones

Mantener esta tabla durante la ejecución para evitar decisiones implícitas:

| Fecha | Decisión | Motivo | Impacto/compatibilidad | Responsable |
| --- | --- | --- | --- | --- |
| 2026-08-28 | Crear este plan desde la auditoría integral | Centralizar trabajo futuro | Todo el monorepo | Pendiente |
| 2026-08-28 | Fase 1 acotada a parte A (blindar simulado); parte B (adaptador FEL real) queda pendiente de elegir proveedor | Sin certificador contratado ni credenciales de sandbox, no se puede implementar el adaptador HTTP real | `market-backend` (módulo `fel`) | Pendiente |
| 2026-08-28 | Fase 2 acotada a parte A (idempotencia backend: caja, clientes, consulta ventas), sin tocar Flutter en ese momento | Evitar romper la venta online real, que aún no envía `correlationId`; permitir validar backend primero | `market-backend` (caja, clientes, ventas) | Resuelto |
| 2026-08-28 | `AdminUserSeeder` usa `UsuarioService.asignarTiendaSistema` (sin autorización de llamador) en vez de `asignarTienda` | Bug preexistente (commit `79d3df1`): el seeder corre sin usuario autenticado y `asignarTienda` ahora exige `autorizacionTiendaService.exigirAcceso`, rompiendo el arranque en cualquier perfil con `seed.enabled=true` (local/dev/test, y `docker compose up`). Detectado al correr `mvn verify` con Docker por primera vez en esta sesión | `market-backend` (módulo `seguridad`); `asignarTienda` (uso HTTP) no cambió | Resuelto |
| 2026-08-28 | `ClientesApi.listar()` (Flutter) ahora usa `contenidoDePagina()` en vez de tratar la respuesta como array plano | Bug preexistente: `GET /api/v1/clientes` ya devuelve el envelope paginado (rollout documentado en `market-flutter/CLAUDE.md`), pero esta API no se actualizó como sí se hizo con `ProductosApi`/`CuentaPorCobrarApi` — rompía con `TypeError` toda venta online sin cliente explícito (incluida la resolución de "Consumidor Final"). Detectado al probar la Fase 2 parte B en Chrome contra el backend real | `market-flutter` (`clientes_api.dart`) | Resuelto |
| 2026-08-30 | `VentasView.vue` (backoffice) autocompleta precio unitario y agrega método de pago obligatorio a "Nueva venta" | Bug preexistente reportado por el cliente: el precio unitario nunca se autocompletaba al elegir un producto (backend siempre lo devolvió bien; la vista solo cargaba el catálogo genérico sin precio, nunca `GET /productos/tiendas/{tiendaId}`); además `crear()` nunca mandaba `metodoPago`, que el backend ya exige `@NotNull` desde Fase 2 — bloqueaba toda venta con "Datos de entrada inválidos". Ya desplegado en producción | `market-backoffice` (`VentasView.vue`, `productos.service.ts`, `ventas.service.ts`, `useVentas.ts`, `endpoints.ts`, `types/venta.ts`) | Resuelto |
| 2026-08-30 | `CuentasPorPagarView.vue`/`CuentasPorCobrarView.vue` (backoffice) muestran `compraId`/`ventaId` en el detalle de pagos/cobros, no el id propio de la cuenta | Bug preexistente reportado por el cliente: el título del detalle usaba `cuentaEnDetalle.id` (PK de `CuentaPorPagar`/`CuentaPorCobrar`) mientras la fila de la tabla mostraba `compraId`/`ventaId` — ambos contadores divergen porque no toda compra/venta genera una cuenta en el mismo orden (solo las que quedan a crédito, y no siempre reciben/completan en orden de creación). El usuario veía un número de compra distinto al que realmente seleccionó. Ya desplegado en producción | `market-backoffice` (`CuentasPorPagarView.vue`, `CuentasPorCobrarView.vue`) | Resuelto |
| 2026-08-30 | `UsuariosView.vue`/`InventarioView.vue` (backoffice) limpian el formulario de alta al abrirlo, no solo tras crear con éxito | Bug preexistente reportado por el cliente: "Nuevo usuario"/"Registrar movimiento" solo alternaban la visibilidad del formulario sin limpiar sus campos — cancelar sin enviar dejaba los valores de la vez anterior visibles al reabrir (el cliente vio literalmente "ADMIN" pre-escrito en el campo Usuario al crear una cuenta nueva). Auditados los 22 módulos del backoffice: solo estos dos tenían el patrón, el resto ya usaba `abrirCrear()`/`abrirEditar()` correctamente. Ya desplegado en producción | `market-backoffice` (`UsuariosView.vue`, `InventarioView.vue`) | Resuelto |

## 9. Registro de avance

| Fase | Estado | PR/commit | Resultado de pruebas | Observaciones |
| --- | --- | --- | --- | --- |
| 1 — FEL | Parte A resuelta, parte B pendiente | Sin commitear aún | `mvn verify` (con Docker): 533 unitarios + 8 IT, `BUILD SUCCESS` | Blindaje del simulado + correlativo con lock. Adaptador real necesita proveedor/credenciales. |
| 2 — Idempotencia POS | Completa (partes A, B y C) | Sin commitear aún | Backend: `mvn verify` (533+8, `BUILD SUCCESS`). Flutter: `flutter analyze`/`flutter test` limpios; parte B verificada en Chrome contra backend/Postgres reales; parte C solo revisada por código, sin dispositivo real | Backend (caja/clientes/consulta ventas) + Flutter (UUID real, correlationId en venta online, conectividad real, cliente offline usable en la misma sesión, versionado de esquema Isar, minimización de PII local, logout bloqueado con pendientes) listos. Desinstalación marcada como no implementable vía app. De paso se encontraron y arreglaron dos bugs preexistentes: `AdminUserSeeder` y `ClientesApi.listar()` (pagination). |
| 3 — Concurrencia | **Completa** | Sin commitear aún | `mvn verify` (con Docker): 552 unitarios + 24 IT, `BUILD SUCCESS` | `PESSIMISTIC_WRITE` (`findByIdConBloqueo`/`findAbiertaByTiendaIdConBloqueo`) en cliente (límite de crédito), caja (abrir/registrar movimiento/cerrar), CxC/CxP (cobro/pago/anular), gasto programado (generarPago), compra (recibir/anular), traslado (completar/anular), FEL (reintentar/anular), venta (completar/anular) y usuario (asignarTienda/asignarGrupo, serializando la regla "no asignación mixta" entre las dos tablas usuario_tienda/usuario_grupo_tienda). Caja además tiene índice único parcial para una sola sesión ABIERTA por tienda. `CHECK` de BD en los 10 módulos monetarios/de cantidad tocados, verificados con `CheckConstraintsIT`. `GlobalExceptionHandler` traduce `ConcurrencyFailureException` (deadlock/lock no adquirido) a 409 `CONFLICTO_CONCURRENCIA` en vez de 500 genérico. |
| 4 — Sesiones/seguridad | **Completa** salvo "tienda activa tras restaurar" (es de cliente, fuera de alcance) y rate limiter distribuido (no aplica hoy) | `b988a8a`, `110c3d0` | `mvn verify`: 574 unitarios + 24 IT, `BUILD SUCCESS`. En vivo: bloqueo de usuario invalida su token ya emitido de inmediato (sin esperar TTL), confirmado con curl. **CI real en GitHub Actions confirmado verde** (run `33423714537`, los 5 jobs) | `InMemoryLoginRateLimiter.limpiarBucketsLlenos()` purga buckets llenos. Autoservicio (`POST /auth/password`) y restablecimiento admin (`POST /usuarios/{id}/password/restablecer`). `debe_cambiar_password` bloquea el resto de la API. `SecurityVersionValidator` revalida `sver`; `POST /usuarios/{id}/sesiones/revocar` revoca sesiones. Caddy + Nginx con cabeceras de seguridad. **2026-08-31**: llaves dev/test retiradas del tracking de git (script `generar-llaves.sh` + paso nuevo en CI); rotación de llaves JWT probada (`JwtRotacionTest`); MFA evaluado y documentado (no implementado, decisión del usuario); política de contraseña/bloqueo/recuperación/baja documentada (`seguridad-desarrolladores.md` §13-14); "baja de empleados" pasó de dominio-sin-endpoint a 3 rutas reales (`desactivar`/`bloquear`/`activar`, permiso `USUARIOS_CAMBIAR_ESTADO`) — de paso se corrigió un bug de Fase 7 (`entidadId` de auditoría con el id del actor en vez del usuario objetivo). Bug de CI encontrado y corregido tras el push (`110c3d0`): `ProfileStartupIT` arranca perfil `local` real y lee `./local-dev/certs/dev-public.pem`, pero el workflow solo generaba llaves de test — faltaba el paso equivalente para `local-dev/certs`. |
| 5 — CI/pruebas | "Pipeline mínimo" completo, "Cobertura prioritaria" pendiente | Commiteado y en `main` | Verificado local y **CI real en GitHub Actions confirmado verde** en múltiples runs (5 jobs: backend, backoffice, flutter, docker-build, gitleaks) | `.github/workflows/ci.yml` (5 jobs) + `.github/dependabot.yml` + Maven Wrapper + `packageManager` pnpm + `.fvmrc`. E2E de negocio y tests nuevos de Flutter/Vue quedan pendientes (sección "Cobertura prioritaria"). |
| 6 — Backups | Código completo y verificado local; falta que el usuario cree el bucket/SMTP/secrets reales | Sin commitear aún | Verificado contra Postgres descartable con `rclone` local: backup.sh (dump+bundle cifrado+checksum) y restore.sh (descarga+verifica+descifra+restaura) de punta a punta con datos/imágenes/certs/.env de prueba, más `check-freshness.sh`/`alert.sh` en sus 3 escenarios. `docker compose config` limpio | `deploy/backup/*.sh` (backup/restore/check-freshness/alert), `docker-compose.yml`, `.env.example`, `.github/workflows/backup-restore-drill.yml`, `deploy/README.md`. Falta: bucket/cuenta de servicio/SMTP reales, 3 secrets de GitHub, y el ensayo de recuperación con volumen Docker perdido contra un servidor real. |
| 7 — Auditoría/observabilidad | Completa salvo dashboards (pospuestos, requieren Prometheus/Grafana) | Sin commitear aún | `mvn verify` (564 unitarios + 24 IT, `BUILD SUCCESS`); verificado en vivo contra backend local + Postgres real (`@Auditable` y `SecurityAuditPublisher` escribiendo en `audit_event`, `REFRESH_REUTILIZADO` reproducido, correlationId end-to-end, `/actuator/prometheus` con JWT) | Nuevo módulo `auditoria` (tabla append-only + AOP `@Auditable`), `docs/auditoria.md` reescrito, `CorrelationIdFilter`, `AlertaEmailService`, `micrometer-registry-prometheus`. 2 bugs encontrados y corregidos en la verificación local (actor "anonymousUser" en login, `/actuator/health` DOWN por mail health indicator). |
| 8 — Backoffice | Pendiente | | | |
| 9 — Flutter | Pendiente | | | |
| 10 — Funciones comerciales | Pendiente | | | |
| 11 — Rendimiento | Pendiente | | | |
