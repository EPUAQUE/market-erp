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

### Tareas

- [ ] Crear una matriz de agregados y estrategia de concurrencia:

| Agregado | Estrategia inicial recomendada |
| --- | --- |
| Inventario | Mantener `PESSIMISTIC_WRITE` existente |
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
- [ ] Añadir `CHECK` de base de datos para montos positivos, saldos no negativos y
  combinaciones de estado críticas.
- [ ] Revisar todos los flujos `find -> validar -> save` monetarios.
- [ ] Traducir conflictos de concurrencia a códigos HTTP y mensajes consistentes.

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
- [x] Transiciones de estado concurrentes en compra/traslado/FEL: exactamente una
  tiene éxito, sin duplicar movimientos de inventario
  (`CompraConcurrenciaIT`/`TrasladoConcurrenciaIT`/`FelConcurrenciaIT`, 2026-08-28).
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

### Tareas

- [ ] Conservar o volver a solicitar la tienda activa de forma segura tras restaurar
  (no verificado en esta pasada).
- [ ] Implementar cambio de contraseña y flujo administrativo de restablecimiento.
- [ ] Permitir revocar sesiones/dispositivos activos de un usuario.
- [ ] Evaluar MFA para administradores y auditores.
- [ ] Sustituir o complementar el rate limiter en memoria con un almacén compartido si
  habrá múltiples instancias. Confirmado: `InMemoryLoginRateLimiter` usa
  `ConcurrentHashMap` local sin expiración de buckets (crecimiento indefinido).
- [ ] Agregar expiración/limpieza a los buckets del rate limiter para evitar crecimiento
  indefinido.
- [ ] Retirar la llave privada de desarrollo del control de versiones y generar llaves
  locales mediante script/documentación. Confirmado: `local-dev/certs/dev-private.pem`
  y `src/test/resources/certs/test-private.pem` siguen commiteadas (la de producción,
  `deploy/certs/prod-private.pem`, no está trackeada).
- [ ] Probar rotación de llaves JWT manteniendo temporalmente validación de la anterior.
- [ ] Revisar cabeceras CSP, `X-Content-Type-Options`, `Referrer-Policy` y permisos del
  navegador en Caddy/Nginx.
- [ ] Definir política de contraseña, bloqueo, recuperación y baja de empleados.

### Criterio de aceptación

Una sesión válida sobrevive un reinicio normal del cliente sin exponer tokens; una
sesión revocada no puede renovarse y los controles funcionan en todas las instancias.

---

## Fase 5 — Pruebas críticas y CI/CD (P1)

**Confirmado en código (2026-08-28):** no existe `.github/workflows/` ni pipeline
equivalente (toda la fase sigue pendiente). El Dockerfile del backend sí usa
`-DskipTests`. La versión de Java está fijada (`pom.xml` + Dockerfile en Java 25), pero
falta Maven Wrapper, `packageManager`/Corepack en el backoffice y FVM en Flutter.

### Pipeline mínimo

- [ ] Crear `.github/workflows/ci.yml` o equivalente.
- [ ] Backend:
  - `mvn test`;
  - `mvn verify` con PostgreSQL/Testcontainers;
  - compilación del jar;
  - validación Liquibase desde una base vacía y desde una versión anterior.
- [ ] Backoffice:
  - instalación con lockfile;
  - typecheck;
  - tests;
  - build de producción.
- [ ] Flutter:
  - `flutter analyze`;
  - `flutter test`;
  - build APK release al menos en ramas de entrega.
- [ ] Construcción de imágenes Docker solo después de pasar pruebas.
- [ ] Eliminar `-DskipTests` del camino de release o garantizar que la imagen dependa de
  un job de pruebas exitoso.
- [ ] Agregar escaneo de secretos, dependencias e imágenes.
- [ ] Fijar versiones de herramientas:
  - Maven Wrapper;
  - versión Java;
  - `packageManager`/Corepack para pnpm;
  - FVM o versión Flutter documentada.

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

### Tareas

- [ ] Mantener el dump PostgreSQL actual, pero copiarlo cifrado a almacenamiento
  externo y versionado.
- [ ] Respaldar también:
  - imágenes de productos;
  - configuración necesaria para reconstruir el entorno;
  - certificados y secretos mediante su mecanismo seguro, no dentro del dump;
  - volúmenes relevantes de Caddy cuando corresponda.
- [ ] Generar checksum por backup y verificarlo después de subirlo.
- [ ] Alertar cuando falle un backup o no exista uno reciente.
- [ ] Definir RPO y RTO del negocio.
- [ ] Documentar y automatizar restauración en un ambiente aislado.
- [ ] Ejecutar una restauración programada al menos mensualmente.
- [ ] Probar recuperación cuando el volumen Docker completo se pierde.
- [ ] Definir rollback de aplicación y compatibilidad de migraciones.

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

### Auditoría

- [ ] Corregir `docs/auditoria.md` para separar claramente diseño futuro de funciones
  existentes mientras se implementa.
- [ ] Implementar el outbox operativo descrito o reducir el diseño a una primera
  versión realista y durable.
- [ ] Auditar, como mínimo:
  - login, logout, refresh reutilizado y bloqueo;
  - cambios de usuarios, roles y asignaciones;
  - precios y configuración por tienda;
  - ajustes de inventario;
  - apertura, movimientos y cierre de caja;
  - ventas, anulaciones, devoluciones y cobros;
  - compras, pagos y traslados;
  - emisión/anulación FEL;
  - exportaciones de reportes.
- [ ] Registrar actor, tienda, fecha, acción, entidad, resultado y correlation ID sin
  almacenar contraseñas, tokens ni cuerpos sensibles.
- [ ] Proteger la auditoría contra modificación y aplicar retención definida.

### Observabilidad

- [ ] Agregar registry Prometheus u otro backend real de métricas.
- [ ] Exponer métricas en una red administrativa protegida.
- [ ] Crear dashboards para:
  - latencia y errores HTTP;
  - conflictos de inventario/contabilidad;
  - errores y tiempo de certificación FEL;
  - refresh reutilizados y rate limiting;
  - edad/cantidad de pendientes offline reportados;
  - backups y espacio en disco.
- [ ] Agregar correlation ID extremo a extremo en backend y clientes.
- [ ] Configurar alertas accionables y un runbook por alerta.

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

## 9. Registro de avance

| Fase | Estado | PR/commit | Resultado de pruebas | Observaciones |
| --- | --- | --- | --- | --- |
| 1 — FEL | Parte A resuelta, parte B pendiente | Sin commitear aún | `mvn verify` (con Docker): 533 unitarios + 8 IT, `BUILD SUCCESS` | Blindaje del simulado + correlativo con lock. Adaptador real necesita proveedor/credenciales. |
| 2 — Idempotencia POS | Completa (partes A, B y C) | Sin commitear aún | Backend: `mvn verify` (533+8, `BUILD SUCCESS`). Flutter: `flutter analyze`/`flutter test` limpios; parte B verificada en Chrome contra backend/Postgres reales; parte C solo revisada por código, sin dispositivo real | Backend (caja/clientes/consulta ventas) + Flutter (UUID real, correlationId en venta online, conectividad real, cliente offline usable en la misma sesión, versionado de esquema Isar, minimización de PII local, logout bloqueado con pendientes) listos. Desinstalación marcada como no implementable vía app. De paso se encontraron y arreglaron dos bugs preexistentes: `AdminUserSeeder` y `ClientesApi.listar()` (pagination). |
| 3 — Concurrencia | Completa salvo matriz de agregados formal, `CHECK` de BD y revisión general de flujos monetarios | Sin commitear aún | `mvn verify` (con Docker): 536 unitarios + 17 IT, `BUILD SUCCESS` | `PESSIMISTIC_WRITE` (`findByIdConBloqueo`/`findAbiertaByTiendaIdConBloqueo`) en cliente (límite de crédito), caja (abrir/registrar movimiento/cerrar), CxC/CxP (cobro/pago/anular), gasto programado (generarPago), compra (recibir/anular), traslado (completar/anular) y FEL (reintentar/anular). Caja además tiene índice único parcial para una sola sesión ABIERTA por tienda. Queda: `CHECK` de BD para montos/saldos/estados y una revisión sistemática de flujos `find -> validar -> save` monetarios fuera de los ya cubiertos. |
| 4 — Sesiones/seguridad | Pendiente | | | |
| 5 — CI/pruebas | Pendiente | | | |
| 6 — Backups | Pendiente | | | |
| 7 — Auditoría/observabilidad | Pendiente | | | |
| 8 — Backoffice | Pendiente | | | |
| 9 — Flutter | Pendiente | | | |
| 10 — Funciones comerciales | Pendiente | | | |
| 11 — Rendimiento | Pendiente | | | |
