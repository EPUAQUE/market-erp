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

**Bandera temporal agregada y desplegada (2026-09-02):** `FelProdSafetyGuard` dejó el
backend de GCP en crash-loop desde el 2026-08-28 — nadie podía entrar al backoffice
porque el backend nunca terminaba de arrancar en `prod` sin un `CertificadorFelPort`
real, y el cliente sigue en fase de pruebas sin proveedor FEL contratado. Se agregó
`app.fel.requerido-real` (env `FEL_REQUERIDO_REAL`, default `true`) — con `false`,
`DevCertificadorFelAdapter` se registra también en `prod` (vía la nueva condición
`FelSimuladoEnProdCondition`, no ya un simple `@Profile("!prod")`) y el guard solo
loguea un `WARN` en vez de rechazar el arranque. Nuevo test
`perfilProdConFelRequeridoRealEnFalseArrancaConAdaptadorSimulado` en
`ProfileStartupIT`. Desplegado en GCP con `FEL_REQUERIDO_REAL=false` — login real
confirmado funcionando de nuevo. Volver a `true` (o quitar la variable del `.env` del
servidor) en cuanto haya un adaptador FEL real — mientras tanto, cualquier documento
FEL emitido en producción usa el simulado (UUID aleatorio, no un DTE fiscalmente
válido).

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

- [x] Convertir la idempotencia de venta en requisito para todos los clientes, no solo
  para sincronización offline — resuelto (2026-08-31). El texto original de este ítem
  quedó desactualizado: Flutter ya enviaba `correlationId` en toda venta online desde
  la parte B de esta fase (2026-08-28); el bloqueador real era que
  `market-backoffice/src/services/ventas.service.ts` nunca lo mandaba. Cerrado:
  `CrearVentaRequest.correlationId` ahora es `@NotBlank` (obligatorio en el límite
  HTTP), `ventas.service.ts`/`useVentas.ts` generan uno con `crypto.randomUUID()` por
  intento de creación, y `VentaControllerTest`/los IT de `e2e/*E2EIT.java` que crean
  ventas se actualizaron para enviarlo. Sigue opcional/nulo para llamadores internos
  directos del service layer (tests, seeders) que nunca pasan por el controller.
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
- [x] Evitar el ID fijo `1` para “Consumidor Final”; resolverlo por código estable
  expuesto por la API o configuración de tienda — resuelto (2026-08-31).
  `ClienteRepository.findByNombre` (mismo patrón ya usado por `RolRepository.findByNombre`)
  + `ClienteService.obtenerConsumidorFinal()` + `GET /api/v1/clientes/consumidor-final`
  resuelven por nombre, no por PK. `ClientesApi.obtenerConsumidorFinal()` (Flutter)
  consume el endpoint nuevo; `CheckoutNotifier._clienteConsumidorFinal()` ya no hace
  una búsqueda O(n) sobre la lista completa de clientes. El id `1` sigue existiendo
  solo como `_clienteConsumidorFinalIdCacheado` — el fallback de la cola offline
  (sin red, no puede golpear el endpoint), que ahora se actualiza en cuanto se
  resuelve una vez online en la sesión, en vez de ser un valor fijo para siempre.
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

- [x] Respuesta perdida después de crear y después de completar una venta —
  cubierto (2026-09-04). **Bug real encontrado y corregido, no solo un
  test**: `CheckoutNotifier._confirmarOnline` (venta online directa, sin
  pasar por la cola offline) nunca tenía la reconciliación que
  `SyncEngine._sincronizarVenta` ya tenía desde una fase anterior ("Sync
  retry bug" en `market-flutter/CLAUDE.md") — si `completar()` tenía éxito
  real en el servidor pero la respuesta se perdía por la red, un reintento
  manual con el mismo `correlationId` volvía a llamar `completar()` sobre
  una venta ya `COMPLETADA`, el backend respondía `409
  ESTADO_VENTA_INVALIDO`, y el vendedor veía "venta fallida" aunque ya
  estuviera cobrada. Se extrajo la lógica de reconciliación a una función
  compartida `ventaYaQuedoCompletada` (`venta_api.dart`, antes duplicada
  como método privado en `SyncEngine`) y ahora `_confirmarOnline` también la
  usa: ante `ESTADO_VENTA_INVALIDO`, consulta `GET
  /ventas/tiendas/{id}/{id}` y, si ya está `COMPLETADA`, trata la operación
  como éxito en vez de mostrar error. Cubierto con dos tests unitarios en
  `checkout_notifier_test.dart`: respuesta perdida justo después de
  `crear()` (el reintento no crea una segunda venta, gracias a la
  idempotencia por `correlationId` ya existente en el backend) y respuesta
  perdida justo después de `completar()` (el reintento ya no muestra error).
  Solo unitario sobre `ProviderContainer` con un `VentaApi` fake que simula
  el estado del servidor — no contra el backend real ni en tablet Android.
- [x] Reintento tras matar la app durante cada estado — cubierto (2026-09-04)
  con `sync_engine_test.dart`, la primera prueba que existe de
  `SyncEngineNotifier` (antes sin cobertura, ver `market-flutter/CLAUDE.md`).
  No hay forma de persistir un estado intermedio real del lado del
  cliente — `VentaPendienteLocal` es un único registro con el payload
  completo, nunca "ya se creó, falta completar" — así que la resiliencia
  ante un kill depende enteramente de que un `SyncEngineNotifier` nuevo
  (equivalente a relanzar la app) reconstruya el resultado correcto sin
  memoria de qué llamada alcanzó a salir antes del kill. Tres escenarios,
  variando únicamente en qué encontró el backend al reintentar desde cero
  para la misma clave: (1) nunca vio la venta — la crea y completa normal;
  (2) el kill fue justo después de `crear()` — el reintento no duplica
  (`crear()` es idempotente por `correlationId`); (3) el kill fue justo
  después de `completar()` — el reintento la reconoce como ya `COMPLETADA`
  en vez de marcarla con error (mismo mecanismo de `ventaYaQuedoCompletada`
  agregado arriba). Los tres terminan con exactamente una venta `COMPLETADA`
  y la cola local vacía. Solo unitario contra un `VentaApi`/`LocalStore`
  fake — no es matar el proceso real de una app instalada en una tablet.
- [x] Wi-Fi activo con backend caído — cubierto (2026-09-04) con tests unitarios
  nuevos en `checkout_notifier_test.dart` (`market-flutter`): con
  `backendAlcanzableProvider` forzado a `false` (interfaz arriba, sonda de
  `/actuator/health` fallando), `CheckoutNotifier.confirmar` nunca llama a
  `VentaApi` (verificado con un `VentaApi` fake que lanza si se le llama),
  encola la venta localmente con el mismo `correlationId` recibido, y un
  reintento manual con la misma clave sigue sin tocar la red y conserva la
  clave (nunca genera una identidad nueva). Un tercer caso cubre el límite:
  sin almacenamiento local disponible (web) falla explícito en vez de
  fingir éxito o perder la venta en silencio. Solo unitario, sobre
  `ProviderContainer` — no es la prueba en tablet Android real con Wi-Fi
  físicamente activo y backend caído de verdad (esa sigue pendiente, ver
  "Pruebas en tablet Android real" más abajo).
- [ ] Cambio entre Wi-Fi y datos durante sincronización.
- [ ] Dos dispositivos generan operaciones simultáneamente.
- [x] Movimiento de caja procesado con respuesta perdida no se duplica — cubierto con
  tests unitarios (`CajaServiceImplTest`, incluida la colisión de creación
  concurrente simulada) y, desde 2026-08-31, un test de concurrencia real contra
  Postgres: `CajaConcurrenciaIT.cierreConcurrenteConRegistroDeMovimientoProduceUnResultadoConsistente`
  cierra la caja y registra un movimiento de ingreso al mismo tiempo (dos hilos
  reales) y confirma que el resultado es consistente en ambos órdenes posibles
  (movimiento aplicado antes del cierre, o rechazado porque ya cerró) — nunca un
  estado parcial o un saldo esperado que no cuadra.
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
- [x] Cierre concurrente con venta: resultado serializable y auditable — resuelto
  (2026-08-31), mismo test que cierra el ítem equivalente de Fase 2:
  `CajaConcurrenciaIT.cierreConcurrenteConRegistroDeMovimientoProduceUnResultadoConsistente`
  cierra la caja y registra un movimiento de ingreso en dos hilos reales al mismo
  tiempo y confirma que el resultado es consistente en ambos órdenes posibles (el
  lock ya serializaba `cerrar` contra `registrarMovimiento`/`registrarMovimientoSiHayAbierta`
  sobre la misma sesión; faltaba solo el test que lo ejerciera).
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

**2026-08-31**: alcance decidido con el usuario — los 5 flujos E2E como IT de
backend (`*IT`, MockMvc + Testcontainers/Postgres real, mismo patrón ya usado
en el proyecto) en vez de Playwright (no instalado en ningún proyecto todavía,
instalarlo es un esfuerzo propio) + tests Vue de guards/refresh/permisos/
composables. Tests Flutter de checkout/cola/refresh siguen bloqueados por la
misma razón documentada en Fase 9 (acoplados a `ApiClient`/Isar, sin librería
de mocking en ese proyecto). Pruebas contractuales de DTOs quedan pendientes.

- [x] E2E: login -> abrir caja -> vender -> verificar inventario/caja.
  `VentaContadoE2EIT` — primer test del repo que hace **login real por HTTP**
  y reutiliza el JWT devuelto para llamar endpoints protegidos (antes, todos
  los `*IT` autenticaban con un `SecurityContextHolder` simulado, sin pasar
  por HTTP en absoluto).
- [x] E2E: compra -> recibir -> inventario -> cuenta por pagar.
  `CompraRecepcionE2EIT`.
- [x] E2E: crédito -> cobro -> caja -> saldo. `VentaCreditoCobroE2EIT` — de
  paso ejercita contra el backend real el endpoint nuevo de Fase 11
  (`GET .../por-venta/{ventaId}`).
- [x] E2E: traslado entre tiendas. `TrasladoE2EIT`.
- [x] E2E: permisos y aislamiento entre tiendas/grupos. `AislamientoTiendaE2EIT`
  — mismo endpoint, mismo usuario, la única variable es la tienda del path:
  404 en la tienda de su alcance (el interceptor lo dejó pasar, el servicio
  simplemente no encontró una caja abierta) vs. 403 en una tienda fuera de su
  alcance (el interceptor lo rechazó antes de llegar al servicio) — confirma
  que es específicamente un límite de autorización, no un efecto colateral
  de "no hay caja".
- [ ] Tests Flutter para carrito, checkout, cola, refresh, caja y parsers.
  Carrito y parsers ya cubiertos en Fase 9; checkout/cola/refresh siguen
  bloqueados (ver nota de Fase 9: `CheckoutNotifier`/`SyncEngineNotifier`
  acoplados a `ApiClient.instance`/Isar, sin librería de mocking en ese
  proyecto).
- [x] Tests Vue para guards, refresh, permisos y principales composables.
  `guards.spec.ts` (7 casos: `requiresAuth: false`, refresh silencioso
  exitoso/fallido, `loadAuthorization` fallido, permiso faltante/presente, no
  repetir `loadAuthorization` si ya estaba cargada), `auth.store.spec.ts` (5
  casos: `login`, `trySilentLogin` exitoso/fallido, `logout` con y sin fallo
  del backend), `permissions.store.spec.ts` (5 casos), más `useVentas.spec.ts`
  y `useCaja.spec.ts` (11 casos) para dos composables "principales" del
  negocio sin cobertura hasta ahora.
- [x] Pruebas contractuales de DTOs entre backend y clientes — resuelto
  (2026-09-01), enfoque decidido con el usuario: snapshot tests livianos, sin
  dependencia nueva (no Pact — la infraestructura de broker que necesita no
  se justifica hoy). `DtoContractSnapshotTest` (top-level, junto a
  `CheckConstraintsIT`/`LiquibaseMigrationIT`) construye 5 DTOs de respuesta
  de alto tráfico compartido entre `market-flutter` y `market-backoffice`
  (`VentaResponse` con `LineaVentaResponse` anidado, `ClienteResponse`,
  `CuentaPorCobrarResponse` con `CobroResponse` anidado, `ProductoResponse`,
  `CajaSesionResponse` con `MovimientoCajaResponse` anidado) con datos fijos,
  los serializa con el mismo `ObjectMapper` de Jackson 3 que usa el backend
  real, y compara el árbol JSON resultante (no el string — insensible a
  orden de campos) contra un snapshot congelado en
  `src/test/resources/contracts/*.json`. Si alguien cambia un nombre/tipo de
  campo sin querer, el test revienta con un diff claro; actualizar el
  snapshot es la señal para ir a revisar ambos clientes antes de mergear, no
  solo silenciar el test. Los snapshots no se escribieron a mano — se
  generaron corriendo el test una vez contra un archivo vacío y capturando
  la salida real de Jackson (confirma, de paso, que `Instant` sale como
  ISO-8601 y los enums como `name()`, sin necesitar adivinar el formato).

**Hallazgo de infraestructura, no de negocio**: Spring Boot 4 dividió
`spring-boot-starter-test` — ya no arrastra soporte de MockMvc
(`@AutoConfigureMockMvc` se movió a `org.springframework.boot.webmvc.test.autoconfigure`,
en un módulo nuevo `spring-boot-starter-webmvc-test`) ni Jackson bajo su
paquete clásico (Jackson 3 renombró `com.fasterxml.jackson.databind` a
`tools.jackson.databind` para `core`/`databind`, dejando `com.fasterxml.jackson.annotations`
solo para anotaciones). Ninguno de los `*IT` existentes lo había necesitado
antes porque ninguno hacía HTTP real — los 5 IT de esta fase son los
primeros. Corregido: nueva dependencia `spring-boot-starter-webmvc-test`
(scope test) en `pom.xml`, imports actualizados a los paquetes nuevos.

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

**2026-09-01 — bucket/service accounts/secrets reales creados por el usuario,
bug real encontrado y corregido al primer intento contra GCS real:** el
usuario creó el bucket `inven365-backups` (`--uniform-bucket-level-access`),
las dos cuentas de servicio (`market-backup-writer` adjunta a la VM,
`market-backup-ci-reader` con su llave para el workflow), y cargó los 3
secrets de GitHub. El primer `docker compose exec backup /backup.sh` real
contra GCS falló al subir: `googleapi: Error 400: Cannot insert legacy ACL
for an object when uniform bucket-level access is enabled` — la verificación
de esta fase (arriba) había usado el backend `local` de `rclone` como
reemplazo de GCS real, que no tiene esta restricción, así que el gap nunca se
había ejercitado. Causa: `rclone` intenta poner una ACL legacy por objeto al
subir a menos que se le diga explícitamente que el bucket usa políticas
uniformes. Corregido: `RCLONE_CONFIG_GCS_BUCKET_POLICY_ONLY: "true"` agregado
al servicio `backup` en `docker-compose.yml`, y `bucket_policy_only = true`
agregado al `rclone.conf` que genera `backup-restore-drill.yml` en CI (mismo
riesgo, aunque ahí solo lee). **Confirmado end-to-end contra infraestructura
real el mismo día**: tras el fix, `docker compose exec backup /backup.sh`
subió y verificó el bundle contra `gs://inven365-backups/` sin error, y el
`workflow_dispatch` de `backup-restore-drill.yml` (run `33541563596`) pasó
completo — descargó el bundle real, verificó checksum, descifró, restauró
contra un Postgres descartable, y pasó el sanity check de conteo de filas.
Primera vez que este pipeline corre de punta a punta contra GCS/GitHub Actions
reales, no solo contra el backend `local` de `rclone`.

**2026-09-01 — SMTP real configurado y probado end-to-end:** el usuario
configuró `ALERT_SMTP_*`/`ALERT_EMAIL_*` en el `.env` del servidor con una
cuenta de Gmail (contraseña de aplicación, `smtp.gmail.com:587`) y recreó
`backup`/`backend`. `docker compose exec backup /alert.sh "Prueba" "..."`
mandó el correo real y llegó a `serviciotutiendagt@gmail.com` — confirmado
por el usuario. Como `deploy/backup/alert.sh` y el `AlertaEmailService` de
Fase 7 leen las mismas 6 variables (`application.yml`, sección `mail`), esto
valida las credenciales SMTP para ambos caminos; el envío específico desde
el lado Java (`REFRESH_REUTILIZADO`/`RATE_LIMIT_ALCANZADO`) no se disparó de
nuevo en esta sesión — sigue siendo el mismo canal ya verificado en Fase 7,
solo que antes con SMTP vacío (caía a solo-log).

**Pendiente, requiere que el usuario actúe de su lado (no accesible desde
acá):** probar recuperación con el volumen Docker completo perdido contra
una copia de prueba real del servidor (el pipeline en sí ya se verificó de
punta a punta, falta el ensayo contra disco/VM real) — el único ítem que
queda de esta fase.

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
refresh de sesión solo reacciona a un 401, no es proactivo al montar la app.

**2026-08-31**: primera pasada ("base + quick wins", decisión del usuario) completa —
ESLint 9 (flat config) + Prettier agregados y en CI, cancelación de requests real en
los 8 composables de listados paginados server-side, y refresh silencioso al recargar
la app. División de vistas grandes, componentes reutilizables, validación de
formularios, accesibilidad y Playwright quedan pendientes para una siguiente pasada.

### Tareas

- [x] Agregar ESLint, Prettier y chequeos en CI. `eslint.config.js` (flat config,
  ESLint 9 + typescript-eslint + eslint-plugin-vue) y `.prettierrc.json`; `pnpm lint`/
  `pnpm format`/`pnpm format:check` nuevos, corridos en CI antes de typecheck. Un error
  real encontrado y corregido (`no-useless-assignment` en `ProductosView.vue`); el resto
  del codebase ya cumplía (solo se reformateó con Prettier, sin cambios semánticos).
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
- [x] Normalizar cancelación de requests y evitar respuestas obsoletas al cambiar
  filtros rápidamente. `ApiClient` (`get`/`post`/`put`/`delete`) acepta `signal` ahora;
  los 8 composables detrás de listados paginados server-side (`useCaja`/`useClientes`/
  `useCompras`/`useCuentasPorCobrar`/`useInventario`/`useProductos`/`useTraslados`/
  `useVentas`) abortan su propia llamada anterior antes de lanzar una nueva. El resto de
  composables (listados client-side, sin refetch por cada cambio de página) no lo
  necesitaba — no se tocaron.
- [ ] Agregar accesibilidad: navegación por teclado, foco, etiquetas, contraste y
  anuncios de error.
- [x] Implementar refresh silencioso al cargar la aplicación. `authGuard` intenta
  `authStore.trySilentLogin()` (reusa el mismo refresh con dedupe que ya usaba el
  interceptor de 401) antes de redirigir a `/login` cuando no hay access token en
  memoria — antes una recarga de página siempre mandaba a login aunque la cookie
  HttpOnly de refresh token siguiera viva. Verificado en Chrome contra el backend real:
  login, recarga completa de página, sesión se mantiene sin pasar por `/login`.
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

**2026-08-31**: primera pasada ("tests + dividir pos_screen", decisión del usuario)
completa. Varias tareas quedan fuera de esta pasada por necesitar hardware real
(impresión, cámara, tablets, integración en Android) o una decisión del usuario
(firma/keystore, proveedor de telemetría) — ninguna de las dos es algo que se pueda
resolver sin esos insumos.

### Tareas

- [x] Dividir `pos_screen.dart` (826 líneas) y pantallas grandes en
  widgets/controladores pequeños. Nuevo directorio
  `lib/features/ventas/presentation/pos/` (`pos_colors.dart`,
  `pos_columna_accesos.dart`, `pos_columna_productos.dart`,
  `pos_columna_carrito.dart`, `pos_body_telefono.dart`) — `pos_screen.dart` quedó en
  ~130 líneas, solo el `Scaffold`/`AppBar` y la decisión tablet-vs-teléfono. Ningún
  widget cambió de comportamiento, solo de archivo (privados `_Foo` pasaron a
  públicos `Foo` para cruzar límites de librería en Dart).
- [x] Agregar tests unitarios para (parcial — ver detalle): carrito y redondeos
  (`carrito_test.dart`, `carrito_notifier_test.dart` — dominio puro +
  wiring del `Notifier`); serialización y parsers (`venta_api_test.dart`:
  `metodoPagoToJson/FromJson`, `Venta.fromJson`, `CuentaPorCobrar.fromJson/pendiente/
  vencida`; `producto_catalogo_test.dart`: `vendible/coincideBusqueda/
  coincideCodigoExacto`); clasificación de errores (`api_exception_test.dart`:
  `ApiException.fromDioException`, red vs. respuesta del backend). **No cubierto**:
  checkout por método de pago como test unitario puro (`CheckoutNotifier` depende de
  `ApiClient.instance`, un singleton con Dio real y constructor privado — mockearlo
  necesitaría una librería de mocking que este proyecto no tiene hoy, ej. `mocktail`;
  se cubrió a nivel de widget en su lugar, ver abajo), autenticación/refresh
  (mismo obstáculo — `ApiClient`), y el motor de sincronización en sí
  (`SyncEngineNotifier`, acoplado a Isar/Riverpod, sin infraestructura de test
  existente — mismo gap que ya documentaba `market-flutter/CLAUDE.md` antes de esta
  pasada). Caja no tiene test unitario propio nuevo (su lógica de dominio es mínima,
  el service la delega casi entera al backend).
- [x] Agregar pruebas de widgets para selector de tienda, cobro y pendientes (login ya
  tenía una prueba básica). `tienda_picker_screen_test.dart` (sin tiendas/varias
  tiendas/habilitar Continuar), `cobro_sheet_test.dart` (Efectivo con cambio,
  Tarjeta/Transferencia sin monto, Crédito exige cliente, Mixto exige que la suma
  iguale el total exacto — sin tocar red, nunca se llega a tocar CONFIRMAR),
  `pendientes_error_screen_test.dart` (lista vacía, tarjetas con mensaje de error,
  confirmación de DESCARTAR). 62 tests en total (antes 1).
- [ ] Agregar pruebas de integración en Android — necesita un dispositivo/emulador
  real; `market-flutter/CLAUDE.md` ya documenta inestabilidad crónica del emulador en
  esta máquina.
- [ ] Definir versión, firma, flavors (`dev`, `staging`, `prod`) y distribución del
  APK — la firma real necesita un keystore del usuario, no algo que se pueda generar
  unilateralmente.
- [ ] Validar impresión de ticket, reimpresión y selección de impresora en hardware
  real.
- [ ] Probar cámara/lector, teclado físico y distintos tamaños de tablet.
- [ ] Medir tiempos de arranque, catálogo grande y consumo de memoria.
- [ ] Definir telemetría de errores respetuosa de datos personales — ya evaluado
  informalmente (`market-flutter/CLAUDE.md`, sección Sentry vs. Crashlytics): decisión
  pendiente del usuario, no tomada unilateralmente por implicar una cuenta externa y
  una clave de API committeada.

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

**2026-08-31**: auditoría completa hecha (agente de exploración, file:line por punto) +
implementación de los hallazgos accionables sin necesitar datos de carga real. Detalle
por tarea abajo. Quedan pendientes, por necesitar decisiones/datos que no puedo generar
solo: volúmenes esperados (número de negocio, debe darlo el usuario), `EXPLAIN ANALYZE`
con datos representativos (necesita esos volúmenes primero) y prueba real de múltiples
instancias (necesita la infraestructura desplegada, no solo el código).

### Tareas

- [ ] Definir volúmenes esperados: tiendas, productos, tickets/día, líneas/ticket y
  usuarios concurrentes. **Pendiente — es una decisión de negocio del usuario, no algo
  que se pueda inferir del código.**
- [ ] Probar los listados y dashboards con datos representativos. **Pendiente** —
  depende de la tarea anterior (sin volúmenes definidos, no hay "representativo" que
  generar).
- [x] Revisar si otros listados (no productos/ventas/clientes) siguen sin paginación
  server-side. Auditados los 19 controllers de listado del backend. La mayoría son
  catálogos pequeños acotados por naturaleza (categorías, marcas, unidades, proveedores,
  grupos de tienda, tiendas, roles, usuarios, producto×tienda) — correctamente sin
  paginar. Tres crecían sin límite natural, igual que ventas/CxC: **cuentas por pagar,
  FEL y notificaciones** — los tres migrados a `PaginaResponse` (backend + backoffice,
  mismo patrón que ventas/CxC/traslados/productos/inventario/caja/compras/clientes).
- [x] Verificar si hace falta una consulta directa de CxC por `ventaId`. Confirmado: no
  existía (`CuentaPorCobrarRepository` solo tenía `findByTiendaId`), y el único call
  site del patrón O(n) (`market-flutter`'s `CuentaPorCobrarApi.buscarPorVenta`) estaba
  sin usar (código muerto — el flujo que lo necesitaba se simplificó en una fase
  anterior). Agregado `findByVentaId` (mismo patrón que `DocumentoFelRepository`) +
  nuevo endpoint `GET /cuentas-por-cobrar/tiendas/{tiendaId}/por-venta/{ventaId}` (404
  si la venta no tiene cuenta — caso normal, ej. venta al contado), verificado contra
  el backend real con `curl`.
- [ ] Revisar planes de ejecución e índices con `EXPLAIN ANALYZE`. **Pendiente** —
  depende de volúmenes representativos (tarea de arriba); sin datos reales de carga,
  un `EXPLAIN ANALYZE` contra una BD casi vacía no dice nada útil sobre el plan real.
- [x] Medir el costo real de Argon2. Microbenchmark en esta máquina de desarrollo (no
  "hardware de producción" — ese paso sigue pendiente del usuario, ver
  `seguridad-desarrolladores.md` §4): **~56.6ms promedio** por hash con los parámetros
  actuales (m=19456 KiB, t=2, p=1), muy por debajo del objetivo OWASP de ~250-500ms —
  hay margen real para subir el costo, pero no se subió unilateralmente (la doc ya
  dice explícitamente que la decisión final necesita medirse en el hardware real de
  producción, no en una laptop de desarrollo).
- [x] Configurar explícitamente el pool de conexiones HikariCP. Antes dependía en
  silencio de los defaults de Spring Boot (`maximum-pool-size=10`, `minimum-idle`
  igual al máximo). Ahora explícito en `application.yml`, configurable por env var,
  con los mismos valores de tope (no subidos sin datos de carga real) pero
  `minimum-idle` bajado de 10 a 2 (deja que el pool se achique sin tráfico).
- [x] Definir estrategia de imágenes de producto. Auditado: ya había whitelist de
  tipo de contenido (JPG/PNG/WEBP) pero ningún límite de tamaño propio (dependía
  solo del límite genérico de multipart, 5MB para toda la app) — agregado un límite
  específico de 2MB (`app.storage.productos-imagenes-max-bytes`, configurable).
  Miniaturas: no se generan (confirmado, cero referencias a librerías de imagen en
  el proyecto) — documentado como no implementado, no urgente al día de hoy sin
  volumen real de catálogo. Almacenamiento: filesystem en un volumen Docker nombrado,
  ya incluido en el backup a GCS (Fase 6) — migrar a un object storage dedicado
  (GCS directo) queda como paso futuro si el volumen de imágenes crece, no antes.
- [x] Probar múltiples instancias antes de habilitarlas — evaluado (no se puede
  "probar" de verdad sin desplegar 2+ réplicas reales). Dos componentes de estado en
  memoria local rompen con 2+ instancias: `InMemoryLoginRateLimiter` (ya documentado
  en Fase 4 — rate limiting por IP se resetea/duplica por instancia, necesita Redis
  u otro almacén compartido) y, hallazgo nuevo de esta auditoría,
  `PermisosEfectivosResolverImpl` (caché de permisos por usuario, TTL 30s) — un
  cambio de rol/tienda en la instancia A no invalida la caché de la instancia B. De
  paso se encontró un bug real (no solo de multi-instancia): el método `invalidar()`
  de ese caché existía pero **nunca se llamaba desde ningún lado**, ni siquiera en
  una sola instancia — un cambio de tienda/grupo/estado de usuario tardaba hasta 30s
  en reflejarse en sus permisos efectivos aunque solo hubiera una instancia corriendo.
  Corregido: `UsuarioServiceImpl` ahora invalida la caché del usuario afectado tras
  `asignarTienda`/`asignarGrupo`/`desactivar`/`bloquear`/`activar`. El resto ya está
  listo para multi-instancia sin cambios: el correlativo FEL usa lock de base de
  datos (no memoria local), el scheduler de limpieza de refresh tokens es una
  operación idempotente contra la BD, y no hay ningún `@Cacheable`/caché adicional en
  el proyecto.

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
- [x] Restauración completa ensayada — 2026-09-01, `workflow_dispatch` de
  `backup-restore-drill.yml` (run `33541563596`) contra GCS real, ver Fase 6.
- [x] Backups externos cifrados y alertados — 2026-09-01, bundle cifrado
  AES256 en `gs://inven365-backups/`, alerta SMTP real confirmada recibida.
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
| 2026-08-31 | El texto del ítem "correlationId obligatorio" en Fase 2 culpaba a Flutter (parte B) del bloqueo | Estaba desactualizado: Flutter ya enviaba `correlationId` en toda venta online desde 2026-08-28; el bloqueador real, encontrado al revisar `ventas.service.ts`, era que el backoffice Vue nunca lo mandaba | `market-backend` (`CrearVentaRequest`), `market-backoffice` (`ventas.service.ts`, `useVentas.ts`) | Resuelto |
| 2026-09-01 | `docker-compose.yml`/`backup-restore-drill.yml` agregan `RCLONE_CONFIG_GCS_BUCKET_POLICY_ONLY`/`bucket_policy_only` para `rclone` | Bug real encontrado al subir el primer backup contra el bucket GCS real del usuario (`inven365-backups`, creado con `--uniform-bucket-level-access`): `rclone` intentaba poner una ACL legacy por objeto y GCS lo rechazaba con `Error 400`. La verificación previa de Fase 6 solo había usado el backend `local` de `rclone`, que no tiene esta restricción — nunca se había ejercitado contra GCS real hasta hoy | `market-backend/docker-compose.yml`, `.github/workflows/backup-restore-drill.yml`, `deploy/README.md` | Resuelto |
| 2026-09-02 | Bandera `app.fel.requerido-real`/`FEL_REQUERIDO_REAL` (default `true`) para permitir el adaptador FEL simulado también en `prod` | Incidente real: el backend de GCP llevaba desde el 2026-08-28 en crash-loop por `FelProdSafetyGuard` (sin `CertificadorFelPort` real configurado) — nadie podía entrar al backoffice. Cliente sigue en fase de pruebas, sin proveedor FEL contratado todavía; decisión del usuario de desbloquear con una bandera explícita en vez de relajar el guard permanentemente | `market-backend` (módulo `fel`: `FelSimuladoEnProdCondition` nuevo, `DevCertificadorFelAdapter`, `FelProdSafetyGuard`, `application.yml`, `docker-compose.yml`, `.env.example`) | Resuelto y desplegado en GCP (commit `85fd280`) |
| 2026-09-02 | `AdminLayout.vue` (backoffice) redirige a `/login` en un `finally` alrededor de `authStore.logout()` | Bug real reportado por el cliente: "cerrar sesión no funciona, no redirecciona al login". `authStore.logout()` relanza el error si el `POST /auth/logout` falla (comportamiento cubierto por `auth.store.spec.ts`, limpia el estado local igual), pero `onLogout()` no capturaba esa excepción — un fallo de red/CORS/backend dejaba al usuario varado en la pantalla admin con la sesión ya limpia localmente pero sin redirigir | `market-backoffice` (`AdminLayout.vue`) | Resuelto y desplegado en GCP (commit `7d7cff6`) |
| 2026-09-02 | Combo de tienda en Ventas/Caja/Inventario (backoffice) se limita a las tiendas del usuario; `GET /tiendas` ya no exige `TIENDAS_VER` | Pedido del cliente: un usuario no-administrador debe quedar en su tienda por defecto, no elegir de un catálogo completo. Encontrado en la investigación: el combo mostraba TODAS las tiendas a cualquier perfil (sin filtrar) y además `GET /tiendas` exigía `TIENDAS_VER` (permiso solo de ADMIN) para poblar ese combo — ningún rol operativo (CAJERO, ENCARGADO_TIENDA, AUDITOR) podía siquiera cargar la pantalla (403). `TiendaServiceImpl.listar()` ya filtraba por `AutorizacionTiendaService.tiendaIdsPermitidas()`, así que solo hizo falta quitar la anotación del controller | `market-backend` (`TiendaController`), `market-backoffice` (`VentasView.vue`, `CajaView.vue`, `InventarioView.vue`) | Resuelto y desplegado en GCP (commit `34599a2`) |
| 2026-09-02 | Cantidades de producto pasan a enteras en todo el sistema (Ventas, Compras, Traslados, Inventario) | Pedido del cliente: los productos se venden por unidades enteras, nunca fraccionadas — no hay productos por peso/volumen en este negocio. Antes `cantidad` era `NUMERIC(12,3)` en las 4 tablas (y `existencia_actual`), con un comentario explícito en `market-flutter` justificando decimales para "productos por peso/volumen". Cambio de fondo, no solo cosmético: columnas de BD a `NUMERIC(12,0)` (migraciones con precondición `HALT` si alguna vez hay una cantidad fraccionaria real, para no redondear en silencio), `@Digits(fraction=0)` + chequeo de dominio en las 4 líneas/movimiento, inputs `step="1"` en el backoffice, y `LineaCarrito.cantidad` de `Decimal` a `int` en Flutter (sin migración de Isar — ese campo ya se persistía como `String`) | `market-backend` (`ventas`/`compras`/`traslados`/`inventario`: DTOs, entidades, dominio, changelogs), `market-backoffice` (4 vistas), `market-flutter` (`carrito.dart`, `carrito_notifier.dart`, `sync_engine.dart`, `local_store_io.dart`) | Resuelto |
| 2026-09-02 | Subtotal por línea en Ventas + todos los montos a 2 decimales en todo el sistema | Pedido del cliente: dinero físico se maneja a 2 decimales. Hallazgo real en la investigación: `market-backoffice` nunca tuvo la capa `money.ts`/`format.ts` que su propio CLAUDE.md documentaba — cero commits de esos archivos en todo el historial, cada monto se mostraba crudo tal cual la API. En el backend, casi todo campo monetario (precio/costo unitario, límite de crédito, cuentas por cobrar/pagar, caja, gastos programados) era `NUMERIC(12,4)`, salvo `producto_tienda.precio_venta` (ya en 2); los totales de venta/compra también redondeaban a 4 decimales, no 2. 16 columnas migradas a `NUMERIC(12,2)` (8 changelogs nuevos, mismo patrón de precondición `HALT`), `@Digits(fraction=2)` en 13 DTOs, nuevo `money.ts` (sin dependencias nuevas) aplicado en 10 vistas | `market-backend` (12 entidades, 13 DTOs, `Venta`/`Compra`/`Inventario`, 8 changelogs), `market-backoffice` (`utils/money.ts` nuevo, 10 vistas) | Resuelto (commit `e3ffab9`) |
| 2026-09-02 | Incidente en GCP: migración de montos a 2 decimales detenida por 2 filas reales con costo a 4 decimales | La precondición `HALT` de `inventario/004-monto-dos-decimales.xml` hizo exactamente lo que debía: 2 movimientos VENTA en `movimiento_inventario` tenían `costo_unitario=7.7833` — no un error de captura, es el costo promedio ponderado que `VentaServiceImpl` graba en el kardex al completar una venta (calculado con precisión de 4 decimales antes de esta fase). Redondear un valor calculado es seguro; se agregó un changeset previo (`inventario-004a-redondear-costos-existentes`) que redondea explícitamente antes del `ALTER`. Segundo obstáculo encontrado en el mismo incidente: `movimiento_inventario` tiene un trigger append-only (Fase de kardex) que bloqueaba hasta el propio `UPDATE` de corrección — se desactiva puntualmente solo para esa sentencia y se reactiva de inmediato, dentro del mismo changeset | `market-backend` (`inventario/004-monto-dos-decimales.xml`) | Resuelto y desplegado en GCP (commits `6ebd367`, `efee6ec`) |
| 2026-09-02 | Subtotal editable (cantidad+precio → subtotal, o subtotal+cantidad → precio unitario) en Ventas y Compras, todo en una sola línea | Pedido del cliente. El campo subtotal solo se re-sincroniza en `@blur`, nunca en cada `@input` — reescribir el mismo campo que el usuario está tecleando en cada tecla peleaba con la escritura (bug real encontrado y corregido en la primera versión: tipear "10" quedaba cortado a medio camino). Layout a `grid-cols-12` para que producto/cantidad/precio/subtotal/Quitar queden en una sola fila en vez de que el subtotal cayera a la línea de abajo | `market-backoffice` (`VentasView.vue`, `ComprasView.vue`) | Resuelto (commits `dd18d98`, `e85bc0e`, `2dbedcc`) |
| 2026-09-02 | Descripción corta en Producto, para factura/recibo y POS de Flutter | Pedido del cliente. Ya existía una `descripcion` larga (VARCHAR 1000) sin uso real fuera del formulario de alta/edición — se agregó un campo nuevo y separado (`descripcionCorta`, VARCHAR 100), no se reutilizó la existente, decisión del usuario. Hallazgo real en la investigación: no existe ningún generador de factura/recibo (ni PDF ni impresora térmica) en todo el proyecto — FEL hoy solo certifica totales, sin detalle de líneas. Por decisión del usuario, esta fase solo agrega el dato (backend + backoffice + Flutter, incluido el mirror local Isar del catálogo); imprimirlo en un recibo real queda pendiente como tarea aparte | `market-backend` (`Producto`, DTOs, changelog), `market-backoffice` (`ProductosView.vue`), `market-flutter` (`ProductoCatalogo`, `ProductoCatalogoIsar`, tarjeta de producto del POS) | Resuelto (commit `3367846`) |
| 2026-09-02 | Listas del backoffice ordenadas de más reciente a más antigua (Ventas, Compras, Traslados, Cuentas por Cobrar/Pagar, Caja, FEL, Gastos Programados, Usuarios, Productos, Clientes) | Pedido del cliente. Investigación previa confirmó que el frontend nunca ordena — cada vista renderiza el array tal como lo devuelve el backend, y casi ningún repositorio tenía `ORDER BY` explícito (Postgres devolvía orden de inserción, es decir el más antiguo primero). Se replicó el patrón ya existente en `AuditEventRepositoryAdapter` (`Sort.by(DESC, "fecha")`) en los demás adapters, usando la columna de fecha real de cada entidad (`fecha`, `fechaEmision` o `fechaApertura` según el módulo) y `id` DESC solo donde la entidad no tiene columna de fecha (Producto, Cliente, Usuario, Gasto Programado). Explícitamente NO tocado: el kardex de `movimiento_inventario` (ya viene `OrderByFechaDesc` y el saldo no depende de reproducir el orden), `NotificacionesView` (ya reordena en frontend con no-leídas primero), y el orden por vencimiento de `CuentasPorCobrarScreen` en Flutter (lógica de cobranza intencional, no cronológica) — quedan fuera de alcance por decisión explícita del cliente los catálogos de configuración (Categorías, Marcas, Unidades de Medida, Tiendas, Proveedores) | `market-backend` (11 adapters/repositorios: Venta, Compra, Producto, Cliente, CuentaPorCobrar, CuentaPorPagar, Traslado, CajaSesion, DocumentoFel, Usuario, GastoProgramado) | Resuelto |
| 2026-09-02 | Búsqueda de cliente por NIT en Ventas (backoffice) | Pedido del cliente: el cajero normalmente recibe el NIT, no el nombre. Se agregó un campo de búsqueda que filtra el combo de cliente por NIT o nombre — client-side, sin cambios de backend, ya que el catálogo completo de clientes ya se carga en el formulario. Verificado en Chrome: creado un cliente de prueba con NIT, filtrado por NIT dejó solo ese cliente en el combo. Nota: Flutter (`ClienteSelectorSheet`) ya soportaba esto desde antes — no requirió cambios | `market-backoffice` (`VentasView.vue`) | Resuelto (commit `44791a4`) |
| 2026-09-02 | Cliente no podía registrar Gastos Programados | Investigación descartó el cambio de orden DESC del mismo día (commit `c941d06`) como causa — compila limpio y no toca el path de creación. Causa real: `GASTOS_PROGRAMADOS_CREAR`/`_VER`/`_EDITAR`/`_GENERAR_PAGO` solo estaban seedeados para ADMIN (y `_VER` para AUDITOR) — el módulo nunca pasó por el catch-up de permisos operativos que Ventas/Caja/Inventario/Traslados sí recibieron en `seguridad-006`. Decisión del cliente: ENCARGADO_TIENDA gana el set completo (ver/crear/editar/generar pago), mismo alcance que ya tiene en Caja/Inventario/Traslados | `market-backend` (`seguridad/015-seed-gastos-programados-encargado.xml`) | Resuelto |
| 2026-09-02 | Kardex de Inventario muestra el proveedor cuando el movimiento viene de una compra | Pedido del cliente. Hallazgo real en la investigación: `movimiento_inventario` nunca guardó ninguna referencia al documento de origen — ni siquiera se pasaba el parámetro al registrar el movimiento, para ningún módulo (Compras/Ventas/Traslados). Se agregó `origen_id` (nullable, sin FK — puede apuntar a compra/venta/traslado según `tipo_movimiento`) y se pasó a través solo desde `CompraServiceImpl.recibir` (único caso real hoy: `DEVOLUCION_PROVEEDOR` existe en el enum pero ningún flujo lo genera todavía). El nombre del proveedor se resuelve en `InventarioApiMapper` (capa API), no en `InventarioServiceImpl` — Compras ya depende de Inventario, así que resolver ahí crearía un ciclo de beans. Movimientos ya registrados antes de este cambio quedan sin proveedor (no hay forma de reconstruir el origen retroactivamente) | `market-backend` (`MovimientoInventario`, `InventarioApiMapper`, `ProveedorService.obtener` nuevo, changelog `inventario/005-origen-id.xml`), `market-backoffice` (columna Proveedor en kardex de `InventarioView.vue`) | Resuelto |
| 2026-09-02 | Filtro por columna + búsqueda global en tablas del backoffice | Pedido del cliente. Investigación previa: ninguno de los 20 módulos con tabla tenía filtro por columna — ni siquiera Categorías/Marcas, la "referencia" que documenta CLAUDE.md, lo tenía; era terreno nuevo, no un parche a rezagados. Decisión del cliente: empezar por los módulos de uso diario (Ventas, Compras, Productos, Clientes, Inventario) en vez de las 20 vistas de una vez, y aceptar que en los módulos con paginación server-side (los 5 elegidos lo son) el filtro solo busca en la página cargada, no en todo el listado — agregar búsqueda real al backend queda para una fase aparte si hace falta. Se creó `useFiltrosTabla` (composable reusable): cada columna filtrable declara un único `valor(item)` que sirve tanto para su filtro de columna como para la búsqueda global, así "ese mismo filtro se puede hacer de forma global" queda garantizado por construcción. Verificado en Chrome: Clientes (NIT/Nombre/Estado), Ventas (Cliente/Vendedor/Estado) y Productos (Código/Nombre/Categoría/Marca/Unidad/Estado) — búsqueda global y filtro por columna probados en vivo, incluyendo limpiar filtros | `market-backoffice` (`useFiltrosTabla.ts` nuevo, `VentasView.vue`, `ComprasView.vue`, `ProductosView.vue`, `ClientesView.vue`, `InventarioView.vue` — existencias y kardex) | Resuelto |
| 2026-09-02 | Filtro por columna + búsqueda global extendido a los 15 módulos restantes | Continuación de la fila anterior — el cliente pidió extender el mismo patrón (`useFiltrosTabla`) al resto del sistema. Cubre: Categorías, Marcas, Unidades de Medida, Tiendas, Proveedores, Grupos de Tiendas, Usuarios (ya tenían búsqueda global de un campo, quedó reemplazada por el composable con filtro por columna agregado); y Facturación Electrónica, Traslados, Caja (historial de sesiones), Cuentas por Pagar, Cuentas por Cobrar, Gastos Programados, Configuración por tienda de Producto, Notificaciones (ninguno tenía ningún buscador antes). En Notificaciones el filtro se aplica antes del ordenamiento no-leídas-primero que ya existía, sin tocar esa lógica. Verificado en Chrome: Categorías, Cuentas por Pagar y Notificaciones renderizan correctamente con filtro por columna + búsqueda global | `market-backoffice` (15 vistas: `CategoriasView.vue`, `MarcasView.vue`, `UnidadesMedidaView.vue`, `TiendasView.vue`, `ProveedoresView.vue`, `GruposTiendaView.vue`, `UsuariosView.vue`, `FelView.vue`, `TrasladosView.vue`, `CajaView.vue`, `CuentasPorPagarView.vue`, `CuentasPorCobrarView.vue`, `NotificacionesView.vue`, `GastosProgramadosView.vue`, `ProductoTiendasView.vue`) | Resuelto |
| 2026-09-02 | Rebrand de "Market" a "Inven365" (nombre + logo) | Pedido del cliente al tener ya el dominio `inven365.com.gt`: personalizar nombre y logo en vez de seguir con "Market" (genérico, nunca fue el nombre real del negocio). Nombre elegido combina "inventario" + "365" (todo el año), coincide con el dominio. Logo: no existía ningún archivo de imagen — era texto/CSS puro (cuadrado de color con una letra) — se mantuvo el mismo patrón simple, solo cambiando "M" por "i365", y se agregó un favicon SVG inline (antes no existía ninguno) con los mismos colores de marca ya definidos en `tokens.css`. Cubre backoffice completo (sidebar, login, título de pestaña, breadcrumb, textos descriptivos "de Market" en 3 vistas) y Flutter (nombre visible en login, `android:label` del ícono en el teléfono, título de `MaterialApp`) — decisión de incluir Flutter para consistencia de marca en todo el sistema, no solo el web. Verificado en Chrome: login, sidebar y breadcrumb del backoffice. Flutter: APK release generado (`flutter build apk --release`, `android:label="Inven365"` confirmado en el manifest final junto con el permiso INTERNET) e instalado por el cliente en la tablet real — confirmó que abre bien con el nuevo login "Inven365" | `market-backoffice` (`index.html`, `AdminLayout.vue`, `LoginView.vue`, `ClientesView.vue`, `TiendasView.vue`, `ProveedoresView.vue`), `market-flutter` (`AndroidManifest.xml`, `login_screen.dart`, `main.dart`) | Resuelto |
| 2026-09-02 | Rediseño visual del backoffice: formularios a ventana modal + modo oscuro | Pedido del cliente a partir de una propuesta visual (artifact) inspirada en las capturas de `market-design/` (plantilla "Avalon" de PrimeVue) — se le pidió confirmar dos decisiones antes de aplicar: (1) modal en vez de formulario expandido en el lugar, y (2) modo claro/oscuro. Cliente confirmó ambos, más alcance completo (Flutter también, ver fila aparte). **Modo oscuro**: `theme.store.ts` nuevo (Pinia), persistido en `localStorage` bajo la clave `inven365-tema`, aplicado vía atributo `data-theme` en `<html>` — nunca sigue `prefers-color-scheme` del sistema operativo a propósito (decisión explícita del cliente: siempre abre en claro, cada usuario elige oscuro si quiere). Un script inline en `index.html` aplica el atributo antes del primer render para evitar parpadeo en la recarga de un usuario que ya eligió oscuro. Los tokens de marca (`tokens.css`) ganan un bloque `:root[data-theme='dark']` — deliberadamente NO se tocan `--mk-brand`/`--mk-primary`/`--mk-accent` (sidebar y color de marca quedan iguales en los dos temas a propósito, para que la marca sea reconocible sin importar el tema), solo superficies/texto/borde y los 5 colores semánticos (con un piso de opacidad más alto para los badges, ver `main.css`). Botón de alternar (sol/luna) agregado al topbar de `AdminLayout.vue`. **Modal**: `ModalDialog.vue` nuevo (`src/components/common/`) — overlay + tarjeta centrada con título y botón X, sin cierre por click-afuera ni Escape a propósito (formularios con datos cargados, cierre solo explícito vía X o Cancelar, para no perder texto ya tecleado por accidente). Aplicado a las 14 vistas que tienen un patrón real de "abrir formulario para crear/editar" (Categorías, Marcas, Unidades de Medida, Tiendas, Proveedores, Grupos de Tiendas, Usuarios, Productos, Clientes, Ventas, Compras, Traslados, Inventario, Configuración por tienda de Producto) — deliberadamente NO se tocaron los formularios de Cuentas por Pagar/Cobrar (registrar pago/cobro, embebido en el detalle de una cuenta existente, no un alta de la entidad principal), Caja (acciones ligadas al estado de la sesión, no un alta opcional) ni FEL (formulario de emisión siempre visible) — no son el mismo patrón "Nuevo X" que pidió la referencia. Verificado en Chrome: modal simple (Categorías), modal ancho con líneas (Ventas) y modo oscuro combinado con el modal, sin problemas de contraste en el primer intento | `market-backoffice` (`theme.store.ts` nuevo, `ModalDialog.vue` nuevo, `tokens.css`, `main.css`, `index.html`, `main.ts`, `AdminLayout.vue`, y 14 vistas admin) | Resuelto |
| 2026-09-02 | Rediseño del login + "recordarme" + "olvidé mi contraseña" | Cliente agregó `login.png` (mismo look Avalon/PrimeVue) a `market-design/` y pidió rediseñar el login. Se presentó la propuesta en el mismo artifact de diseño, adaptada solo con lo que el login real tiene (usuario/contraseña) — se dejaron fuera "Sign in with Google/Apple", "Create an Account" de la referencia por no existir esa funcionalidad. El cliente aprobó y pidió sumar dos funciones nuevas: recordar usuario y recuperar contraseña. **Recordar usuario**: solo frontend, checkbox que guarda el username (nunca la contraseña) en `localStorage` (`inven365-usuario-recordado`) y prellena el campo. **Recuperar contraseña**: no existía nada en el backend — se creó el flujo completo "olvidé mi contraseña" con token de un solo uso: tabla nueva `password_reset_token` (Liquibase `seguridad/016-password-reset-token.xml`, guarda solo el hash SHA-256 del token, nunca el valor en claro, mismo criterio que `refresh_token`), endpoints públicos `POST /api/v1/auth/forgot-password` (busca por `username`, respuesta 200 genérica siempre — exista o no el usuario, tenga o no correo, esté activo o no — para no filtrar información) y `POST /api/v1/auth/reset-password` (canjea el token, valida la misma política de contraseña que ya existe, revoca todos los refresh tokens del usuario al cambiarla). El correo se envía con un servicio nuevo (`PasswordResetMailSenderImpl`) que reutiliza el mismo `JavaMailSender`/SMTP real (Gmail) ya configurado para alertas, pero como servicio separado (destinatario dinámico, no el fijo de alertas) — un fallo de envío nunca cambia la respuesta HTTP. Token expira en 30 minutos (configurable, `PASSWORD_RESET_TOKEN_TTL`), link apunta a `https://inven365.com.gt/restablecer-password` (configurable, `FRONTEND_PASSWORD_RESET_URL`). El rate limit de `forgot-password` reutiliza deliberadamente el mismo limitador de `/login` (mismo cupo por IP+usuario) en vez de uno dedicado — el cliente confirmó dejarlo así: 5 solicitudes de recuperación en 1 minuto también bloquean temporalmente el login de ese usuario, aceptado como trade-off razonable frente a construir un limitador aparte. Frontend: `ForgotPasswordView.vue` y `ResetPasswordView.vue` nuevas, enlace "¿Olvidaste tu contraseña?" en el login. Verificado en Chrome: login, olvidé-contraseña, restablecer-password (con token de prueba) y modo oscuro en las tres pantallas. Backend: 610 tests, 0 fallos, build OK | `market-backend` (migración `016-password-reset-token.xml`, `PasswordResetToken` + repositorio/entidad/mapper, `PasswordResetMailSender`/`PasswordResetMailSenderImpl`, `TokenResetInvalidoException`, endpoints y DTOs nuevos en `AuthController`, `AuthServiceImpl.solicitarRestablecimiento`/`restablecerPassword`, `SecurityConfig`, `SeguridadProperties`, `application.yml`), `market-backoffice` (`LoginView.vue` rediseñada, `ForgotPasswordView.vue` nueva, `ResetPasswordView.vue` nueva, `AuthService.ts`, `endpoints.ts`, `routes.ts`) | Resuelto |
| 2026-09-03 | Últimos detalles visuales del artifact de diseño: iconos de sidebar, avatar, acciones de fila como iconos | Cliente pidió aplicar "todo lo que definimos en el diseño" (mismo artifact). Modal/tema oscuro/login ya estaban aplicados; se confirmó con el cliente (AskUserQuestion) que faltaban 3 detalles del mockup y que sí quería los tres. **Sidebar**: `NavIcon.vue` nuevo (`components/common/`), un ícono SVG por cada uno de los 20 módulos del menú (varios tomados literalmente del mockup — Categorías/Marcas/Productos/Inventario/Compras/Clientes/Ventas/Caja/Dashboard —, el resto diseñados a juego ya que la referencia no los cubría: Unidades de Medida, Proveedores, Cuentas por Pagar/Cobrar, Traslados, FEL, Tiendas, Grupos de tiendas, Gastos Programados, Notificaciones, Reportes, Usuarios). **Avatar**: círculo con iniciales (primeras 2 letras del `username` en mayúscula) en el topbar de `AdminLayout.vue`, junto al nombre y botón Salir — no hay nombre completo en `user.store`, solo `username`, así que las iniciales salen de ahí. **Acciones de fila a iconos circulares**: `ActionIcon.vue` nuevo + clases `.mk-row-actions`/`.mk-row-btn`/`.mk-row-btn-danger`/`.mk-row-btn-success`/`.mk-row-btn-neutral` en `main.css`, aplicado a las 18 vistas con columna "Acciones" (Categorías, Marcas, Unidades de Medida, Tiendas, Proveedores, Grupos de Tiendas, Productos, Producto por Tienda, Clientes, Usuarios, Gastos Programados, Inventario —solo tabla de existencias—, Ventas, Compras, Traslados, Cuentas por Pagar, Cuentas por Cobrar, FEL, Notificaciones) — se preservaron exactos los mismos `v-if` de permisos/estado y los mismos manejadores `@click`, solo cambió la presentación (texto → ícono + `title`). Deliberadamente sin tocar los enlaces de barra de herramientas (Limpiar filtros, Agregar línea, Exportar CSV, Ver historial, breadcrumb "← Productos"), que siguen como texto igual que antes. Trabajo dividido en 3 lotes ejecutados en paralelo (subagentes) siguiendo a `CategoriasView.vue` como referencia exacta; verificado con `pnpm typecheck`/`lint`/`format`/`test` (58/58) tras cada lote y una revisión manual línea por línea de los casos especiales (FEL con botón de doble función, Gastos Programados con 4 acciones condicionales, Productos con el enlace "Tiendas" como `RouterLink`). Se encontró y corrigió un caso que ninguno de los 3 lotes cubrió (`NotificacionesView.vue`, botón "Marcar leída"). **Sin verificar visualmente en Chrome esta vez** — el backend local corriendo no tenía credenciales de admin válidas conocidas; queda pendiente una revisión visual rápida la próxima vez que haya acceso | `market-backoffice` (`NavIcon.vue` nuevo, `ActionIcon.vue` nuevo, `main.css`, `AdminLayout.vue`, y 18 vistas admin) | Resuelto (pendiente verificación visual) |
| 2026-09-03 | Diseño del backoffice adaptado al POS Flutter: paleta centralizada, modo oscuro, login rediseñado | Cliente pidió llevar el diseño del backoffice a `market-flutter`. Investigación previa: la paleta petróleo/esmeralda/ámbar ya coincidía casi 1:1 en Flutter (cada pantalla la redeclaraba como constantes locales `_brand`/`_primary`/`_danger` duplicadas, sin `ThemeData` real ni modo oscuro — deuda de copy-paste, no una paleta distinta). Se creó `core/theme/app_colors.dart` (fuente única, claro+oscuro, mismos valores que `tokens.css` del backoffice incluido el criterio de que la marca no cambia entre temas) y `core/theme/app_theme.dart` (`ThemeData` real vía `ColorScheme` explícito, cosa que el proyecto no tenía — ninguna pantalla usaba `Theme.of(context)` antes de esto). **Modo oscuro**: `ThemeNotifier` (Riverpod `Notifier<ThemeMode>`) + `shared_preferences` (dependencia nueva — no existía ningún mecanismo liviano de persistencia de preferencia de UI, solo `flutter_secure_storage`/Isar para otras cosas), mismo criterio que el backoffice: abre siempre en claro, cada usuario alterna, nunca sigue el tema del sistema operativo. Botón de sol/luna agregado al AppBar de `PosScreen` (pantalla principal tras login) y de los dos dashboards. **Login**: `login_screen.dart` rediseñado visualmente igual que el del backoffice (marca "i365" en vez de texto plano, sin `Card` con borde, campos tipo píldora) preservando intacta toda la lógica ya existente y documentada en este mismo archivo — el fix de scroll por teclado (`LayoutBuilder`/`SingleChildScrollView`/`ConstrainedBox`) y la distinción de mensaje de error genérico vs. de red no se tocaron. `pos_colors.dart` y `DashboardPalette` (`dashboard_widgets.dart`) se alinearon a los mismos valores que `AppColors.light` (duplicados como literales `const` porque Dart no permite leer un campo de instancia de otra clase dentro de una expresión `const` — se documentó con un comentario para no perder sincronía a futuro). De paso se corrigió un test viejo y roto desde antes de esta sesión (`test/widget_test.dart` buscaba el texto "Market", nombre anterior al rebrand a Inven365, nunca actualizado). **Alcance explícitamente parcial** — quedan sin migrar a `AppColors`/dark-mode-aware las demás pantallas y widgets con colores hardcodeados (`caja_screen.dart`, `cuentas_por_cobrar_screen.dart`, `pendientes_error_screen.dart`, `barcode_scanner_screen.dart`, `cobro_sheet.dart`, `cliente_selector_sheet.dart`, `connectivity_badge.dart`, y los widgets internos de `pos/`) — hoy se ven bien en claro (mismos valores de siempre) pero no cambian si el usuario activa el oscuro; queda para una fase aparte, igual que se hizo por partes en el backoffice. Verificado: `flutter analyze` limpio, `dart format` sin cambios, 62/62 tests (61 preexistentes + el fix del test viejo), y captura real en Chrome (`flutter run -d web-server`) del login en claro y en oscuro (forzando temporalmente el valor por defecto del notifier solo para la captura, revertido de inmediato) — no se pudo probar más allá del login por no contar con credenciales válidas contra el backend local | `market-flutter` (`core/theme/app_colors.dart` nuevo, `core/theme/app_theme.dart` nuevo, `core/theme/theme_notifier.dart` nuevo, `main.dart`, `login_screen.dart`, `pos_screen.dart`, `dashboard_encargado_screen.dart`, `dashboard_vendedor_screen.dart`, `pos/pos_colors.dart`, `dashboard/dashboard_widgets.dart`, `pubspec.yaml` — `shared_preferences` nuevo, `test/widget_test.dart`) | Resuelto (alcance parcial, fase 2 pendiente) |
| 2026-09-03 | Corrección: el sidebar del backoffice no tenía el degradado ni el petróleo más oscuro del modo oscuro que sí tenía la propuesta aprobada | Cliente detectó, comparando en vivo contra la propuesta, que el sidebar en modo oscuro se veía plano y con el mismo tono que en claro — no coincidía con el mockup, que usaba un degradado (`--brand` → `--brand-deep`) y un petróleo más oscuro específico para el tema oscuro. La causa: al implementar el modo oscuro (fila anterior del 2026-09-02) se decidió unilateralmente que la marca (sidebar/botón primario/acento) no cambiara entre temas "para que se reconozca sin importar el tema" — una decisión razonable en abstracto pero que en los hechos revirtió algo que la propuesta ya mostraba y el cliente ya había visto y aprobado, sin volver a consultarlo. Corregido: se separó el criterio — `--mk-primary`/`--mk-accent` (botones, acento) siguen iguales en ambos temas, pero `--mk-brand` gana un token hermano `--mk-brand-deep` y el sidebar pasa de `bg-mk-brand` (color plano) a una clase nueva `.mk-sidebar` con `linear-gradient(185deg, var(--mk-brand), var(--mk-brand-deep))` — igual que `.sidebar { background: linear-gradient(...) }` en el mockup. En modo oscuro ambos tokens se redefinen a un petróleo más profundo (`#0A323D`/`#051B21`, tomados literalmente de la sección oscura del mockup) en vez de heredar los valores claros. Verificado: `pnpm typecheck`/`lint`/`format` limpios, y confirmado en Chrome (claro y oscuro, con `getComputedStyle` además de captura visual) que el degradado y el tono más oscuro ya se ven | `market-backoffice` (`tokens.css`, `tailwind.config.ts`, `main.css`, `AdminLayout.vue`) | Resuelto |
| 2026-09-03 | Auditoría completa del mockup vs. backoffice real — 2 discrepancias más encontradas y corregidas | Tras el hallazgo del sidebar (fila anterior), el cliente pidió una revisión exhaustiva de todos los detalles estéticos, no solo los que él ya había señalado. Se comparó cada sección del mockup (paleta, sidebar, topbar, KPIs, tablas, badges, modal, login, ambos temas) contra el código real, en vivo en Chrome. **1. Ítem activo del sidebar**: usaba una barra de acento + overlay translúcido (patrón de una fase anterior a la propuesta de diseño); el mockup usa fondo esmeralda sólido + sombra (`.navitem.active`) — corregido en `AdminLayout.vue`. **2. Paleta oscura de Primario/Acento**: el panel "Paleta — clara y oscura" del mockup ya redefinía estos dos tonos para oscuro (`#34A06A`/`#E6B458`, más luminosos que el claro para leerse bien sobre fondo oscuro), pero solo se había aplicado al sidebar (ver fila anterior) — mismo patrón de "se decidió unilateralmente mantenerlos fijos entre temas" que ya había pasado con el sidebar. Corregido en `tokens.css`: ahora sí cambian en `:root[data-theme='dark']`. De paso se ajustaron dos tarjetas KPI del Dashboard (Ventas del mes, Caja) para usar un pill en la esquina como el mockup, en vez de texto plano debajo del número — mismos datos, sin tocar lógica. **Pendiente de decisión del cliente** (no se tocó): el resto de tarjetas KPI (Inventario, Ticket promedio, etc.) no tienen equivalente de "trend" en el mockup, se dejaron como estaban; el modo "por grupo" del dashboard no tiene datos de comparación mes-anterior así que no se le aplicó el mismo patrón de pill. Verificado: `pnpm typecheck`/`lint`/`format`/`test` (58/58) limpios, confirmado visualmente en Chrome (claro y oscuro) cruzando `zoom` + `getComputedStyle` para evitar el glitch conocido de la captura de pantalla completa | `market-backoffice` (`AdminLayout.vue`, `tokens.css`, `DashboardView.vue`) | Resuelto |
| 2026-09-03 | Botón para ocultar/mostrar el sidebar y ampliar el ancho útil de las tablas | Pedido del cliente: revisando vistas con muchas columnas (Ventas, Cuentas por Pagar/Cobrar, etc.) quería poder ocultar el sidebar para ganar espacio horizontal. Botón nuevo (ícono hamburguesa) al inicio del header, siempre visible — al hacer clic el `<aside>` pasa de `w-64` a `w-0` con `overflow-hidden` y una transición de 200ms, el contenido ocupa el ancho liberado. Estado persistido en `localStorage` (`inven365-sidebar-oculto`), mismo patrón que `theme.store.ts` — se mantiene ocultos/visible entre recargas, no es solo de la sesión. No se creó un store de Pinia aparte para esto (a diferencia del tema, que sí es global): es un `ref` local en `AdminLayout.vue`, único componente que lo usa. Verificado: `pnpm typecheck`/`lint`/`format`/`test` (58/58) limpios; probado en Chrome — ocultar/mostrar funciona, el ancho computado pasa correctamente de 256px a 0px y viceversa, y el estado persiste tras recargar la página | `market-backoffice` (`AdminLayout.vue`) | Resuelto |
| 2026-09-03 | Bug real: los badges de estado (Activo/Inactivo, etc.) no cambiaban de color de texto — solo el fondo | Cliente notó que en la propuesta el badge de "Estado" cambiaba de color según el valor, pero en el resultado real seguía viéndose "de un mismo color". Investigación: la lógica Vue (`:variant="... ? 'success' : 'neutral'"`) y el CSS (`.mk-badge-success`, `.mk-badge-neutral`, etc., cada uno con su propio `color`) estaban bien escritos — pero al inspeccionar el CSS realmente compilado (`pnpm build` y también `pnpm dev`, mismo resultado en ambos) con `getComputedStyle` y comparando byte a byte el archivo generado, se confirmó que las 6 reglas base `.mk-badge-{variant}` (las que traen el `color`) desaparecían por completo del CSS final — solo sobrevivía el override de modo oscuro (`:root[data-theme='dark'] .mk-badge-{variant}`, que solo trae `background-color`). Resultado: todo badge, sin importar su estado, terminaba heredando el `color` genérico de `body`, y solo el fondo (con muy poca opacidad, 12-18%) cambiaba de tono — un cambio tan sutil que a simple vista parece "siempre el mismo color", exactamente lo que reportó el cliente. Causa raíz no del todo clara (posible interacción de Tailwind/PostCSS al procesar `@layer components` cuando el mismo selector también aparece, fuera de cualquier `@layer`, en el override de modo oscuro más abajo en el archivo) pero el fix es reproducible: sacar las 6 reglas base de `.mk-badge-*` de `@layer components` (dejarlas como reglas planas, igual que ya estaban sus overrides de modo oscuro) resuelve el problema — verificado inspeccionando el CSS compilado antes/después del cambio, con ambas declaraciones (`background-color` y `color`) presentes tras el fix. Probablemente afectaba a las 18 vistas que usan `EstadoBadge` desde que existe el componente, no algo introducido en esta sesión. Verificado: `pnpm typecheck`/`lint`/`format`/`test` (58/58) limpios; confirmado en Chrome con `getComputedStyle` los 6 colores (éxito/pendiente/vencido/peligro/info/neutral) distintos entre sí en ambos temas, y en una fila real de Productos alternando Activo/Inactivo | `market-backoffice` (`main.css`) | Resuelto |
| 2026-09-03 | Formato de fecha unificado a dd/MM/yyyy (Guatemala) | Cliente pidió que todas las fechas usen el formato guatemalteco dd/MM/yyyy — hasta ahora cada vista llamaba `new Date(...).toLocaleDateString()`/`.toLocaleString()` directo, sin locale explícito, que da MM/DD/YYYY en un navegador con locale en-US (el caso típico). `DashboardView.vue` sí tenía un `formatFecha` propio con locale `'es-GT'`, pero con formato `día mes-abreviado año` ("02 sept 2026"), no dd/MM/yyyy tampoco. Se creó `utils/fecha.ts` (mismo patrón que `utils/money.ts`: función centralizada, nunca interpolar la fecha cruda) con `formatFecha` (dd/MM/yyyy) y `formatFechaHora` (agrega HH:mm 24h) — implementación manual con `padStart`, no `Intl`/`toLocaleDateString`, para no depender del locale del navegador de quien lo use. Reemplazado en las 11 vistas que mostraban fechas: Caja, Gastos Programados, Dashboard (se eliminó su `formatFecha` local, ahora usa el compartido), Cuentas por Pagar/Cobrar, Ventas, Compras, Traslados, Notificaciones, Reportes, Inventario. Verificado: `pnpm typecheck`/`lint`/`format`/`test` (58/58) limpios; confirmado en Chrome que Ventas muestra `02/09/2026 10:36` en vez del formato anterior dependiente del navegador | `market-backoffice` (`utils/fecha.ts` nuevo, y 11 vistas admin) | Resuelto |
| 2026-09-03 | Vista de factura imprimible para documentos FEL certificados | Repasando `market-design/` con el cliente para ver qué más incluir, `factura.png` (layout de factura del template de referencia) no se había aplicado a nada — Facturación Electrónica solo listaba documentos en una tabla, sin ninguna forma de ver la factura formateada. Nueva `FacturaView.vue` en la ruta `/fel/tiendas/:tiendaId/:documentoId/factura` — **fuera de `AdminLayout`** a propósito (sin sidebar/topbar) para que la hoja se imprima limpia; usa `@media print` para ocultar los botones "Volver"/"Imprimir" y quitar el borde/sombra de la tarjeta al imprimir. El "papel" de la factura es intencionalmente blanco con texto oscuro fijo, sin importar el tema activo del backoffice — igual que una factura real, no debe verse "modo oscuro". Contenido: marca + datos de la tienda (emisor), cliente (nombre/NIT/dirección), serie-número y UUID del documento FEL, fechas de emisión/certificación (con el formateador `dd/MM/yyyy` de la fila anterior), líneas de la venta (código, descripción, cantidad, precio unitario, subtotal) y total; si el documento está `ANULADO` muestra un aviso con el motivo. Se agregó el método `obtener(tiendaId, id)` a `ventasService`/`felService` (el backend ya tenía `GET .../{id}` en ambos controllers — el `VentaController` desde la idempotencia de ventas offline de Flutter, `FelController` desde siempre — pero el backoffice web no los usaba). Botón nuevo "Ver factura" (ícono ojo) en la fila de `FelView.vue`, solo visible cuando `estado === 'CERTIFICADO'`, abre en pestaña nueva. Verificado extremo a extremo con datos reales: se le dio stock a un producto (Inventario), se completó una venta, se emitió su FEL, y se confirmó en Chrome que la factura carga los datos reales correctos (cliente, líneas, total, UUID) — no solo con datos de prueba fabricados. `pnpm typecheck`/`lint`/`format`/`test` (58/58) limpios | `market-backoffice` (`FacturaView.vue` nueva, `FelView.vue`, `endpoints.ts`, `ventas.service.ts`, `fel.service.ts`, `routes.ts`) | Resuelto |
| 2026-09-03 | Paginación numerada en las tablas con paginación real | Segunda recomendación de la revisión de `market-design/`: `filtro tablas.png` usa botones de página (1 2 3 4 5, con «/» al inicio/final) en vez del "Anterior / Página X de Y / Siguiente" de puro texto que tenía el backoffice. Nuevo componente `PaginacionTabla.vue` (nombre con dos palabras — ESLint exige `vue/multi-word-component-names`) que recibe `pagina`/`total-paginas` (`v-model:pagina`) y genera los botones: con 7 páginas o menos los lista todas, con más muestra primera+última+una ventana alrededor de la actual, con "…" en los huecos — mismo criterio que el mockup, que tampoco lista cientos de páginas. Aplicado en las **11 tablas con paginación de servidor real** (más que las 6 documentadas en el CLAUDE.md de este proyecto — FEL y el historial de Caja también resultaron ser server-paginadas, no solo Ventas/Cuentas por Cobrar/Traslados/Productos/Inventario/Caja): Ventas, Compras, Cuentas por Pagar/Cobrar, Traslados, Notificaciones, Productos, Inventario (existencias y kardex, dos instancias independientes), Clientes, FEL, y el historial de sesiones de Caja. El selector de "10/25/50/100 por página" no se tocó, solo la fila de números. Verificado: `pnpm typecheck`/`lint`/`format`/`test` (58/58) y `pnpm build` limpios — de paso se confirmó que las clases CSS nuevas (`.mk-pager-btn`) sí llegan al bundle compilado, tras el susto del bug de badges de la sesión anterior. Con los datos de prueba actuales casi todas las tablas solo tienen 1 página, así que se verificó visualmente el estado base (número 1 resaltado, flechas deshabilitadas) pero no la lógica de truncado con "…" contra datos reales | `market-backoffice` (`PaginacionTabla.vue` nuevo, `main.css`, y 11 vistas admin) | Resuelto |
| 2026-09-03 | Paginación faltante en 4 módulos de catálogo | Auditoría pedida por el cliente ("revisa que todos los módulos tengan paginación") encontró que Categorías, Marcas y Unidades de Medida — pese a que el CLAUDE.md de este proyecto marca a Categorías como "referencia" del patrón completo, que incluye paginación — no tenían ninguna, solo filtro (renderizaban `filtered` completo, sin recorte). Gastos Programados tampoco, siendo el más riesgoso de los cuatro por ser una lista que crece sin límite natural (gastos recurrentes). Se aplicó el mismo patrón ya usado en Tiendas/Proveedores/Usuarios/Grupos de Tienda (paginación client-side: `page`/`pageSize` locales, `totalPages`/slice computados, selector 10/25/50/100 + Anterior/Página X de Y/Siguiente) — no el componente `PaginacionTabla` nuevo, que se reservó para las 11 tablas con paginación de servidor real. En Gastos Programados se agregó además `page.value = 1` al cambiar de tienda (la lista se recarga por completo en ese caso). Confirmado con esto que los 21 módulos de catálogo/listado del backoffice tienen paginación, salvo ProductoTiendas (deliberadamente sin ella, ya documentado — acotado por el número de tiendas). Verificado: `pnpm typecheck`/`lint`/`format`/`test` (58/58) limpios | `market-backoffice` (`CategoriasView.vue`, `MarcasView.vue`, `UnidadesMedidaView.vue`, `GastosProgramadosView.vue`) | Resuelto |
| 2026-09-03 | "Recordarme" y recuperar contraseña agregados a Flutter | Pedido del cliente: el backoffice ya tenía ambas cosas, la app Flutter solo pedía usuario/contraseña sin ninguna. "Recordarme" (checkbox en `LoginScreen`) solo recuerda el usuario tecleado vía `SharedPreferences` — misma clave lógica (`inven365-usuario-recordado`) y mismo alcance que `USUARIO_RECORDADO_KEY` del backoffice, no persiste la sesión (eso ya lo cubre la cookie de refresh, sin tocar). Recuperar contraseña: dos pantallas nuevas (`ForgotPasswordScreen`/`ResetPasswordScreen`) contra los mismos endpoints que ya usaba el backoffice (`/auth/forgot-password`, `/auth/reset-password`) — no fue necesario tocar el backend. Única diferencia real de UX con el backoffice: al no tener deep-linking configurado, el token no se lee de un enlace (`?token=`) sino que el usuario lo pega a mano en un campo, copiado del correo. Se extrajo `authPillDecoration` (antes duplicado dentro de `LoginScreen`) a un archivo compartido para las tres pantallas de auth. Verificado: `flutter analyze`/`dart format --set-exit-if-changed .` limpios, `flutter test` 70/70 (8 nuevos, validaciones sin red + checkbox/precarga con `SharedPreferences.setMockInitialValues`). **Verificado en vivo en Chrome contra el backend real** (a petición explícita del cliente): la causa de que `flutter run -d web-server` pareciera "congelarse" en intentos anteriores resultó ser CORS, no el renderer — `CORS_ALLOWED_ORIGINS` del backend solo traía `localhost:5173` por default, así que toda llamada real fallaba con "No se pudo conectar con el servidor"; con `localhost:8765` agregado, el flujo completo funcionó de punta a punta: login con Recordarme marcado → logout → recarga completa de página → usuario y checkbox siguen precargados (persistencia real de `SharedPreferences`, no solo estado en memoria); Olvidé mi contraseña con usuario real → "Revisa tu correo"; las 3 validaciones de Restablecer contraseña (código vacío, contraseña corta, no coinciden); y con un código inventado, la llamada real al backend devuelve 400 y el mensaje correcto. No se probó el canje de un token real porque el usuario `admin` sembrado no tiene correo registrado (no elegible por diseño) y forzarlo habría requerido un envío de correo real | `market-flutter` (`auth_api.dart`, `login_screen.dart`, `forgot_password_screen.dart` nuevo, `reset_password_screen.dart` nuevo, `auth_pill_decoration.dart` nuevo, `app_router.dart`) | Resuelto |
| 2026-09-03 | Activar/desactivar agregado a Marcas y Unidades de Medida | Al verificar en vivo la paginación de los 4 módulos recién corregidos, se necesitó crear y luego limpiar datos de prueba — y se descubrió que Marcas y Unidades de Medida no tenían ningún endpoint de baja lógica (`UnidadMedida.java` incluso traía un comentario explícito: "Sin estado: es un catálogo puro"), a diferencia de Categorías, que sí lo tiene. El cliente pidió agregarlo. Se replicó exactamente el patrón de Categorías: nuevo enum `EstadoMarca`/`EstadoUnidadMedida` (ACTIVA/INACTIVA), columna `estado` (migración Liquibase `addColumn` con `defaultValue="ACTIVA"` — las filas existentes nunca fueron desactivadas porque la función no existía, así que ese es su estado real, no uno fabricado), métodos `activar()`/`desactivar()` en el dominio, endpoints `POST /{id}/activar` y `/desactivar` (reutilizan el permiso `_EDITAR` existente, igual que Categorías — no se creó un permiso nuevo), y en el backoffice: columna Estado con `EstadoBadge`, filtro por estado, y botón de alternar en ambas vistas. Verificado: backend `mvn test` (616/616) y arranque real contra Postgres confirmando que ambos changesets de Liquibase corrieron limpio; `curl` end-to-end contra los 4 endpoints nuevos; backoffice `pnpm typecheck`/`lint`/`format`/`test` (58/58) limpios; confirmado visualmente en Chrome alternando el estado de una marca real ("Genérica") y revirtiéndolo de inmediato | `market-backend` (`marcas/*`, `unidadesmedida/*`, migraciones `002-marca-estado.xml`/`002-unidad-medida-estado.xml`), `market-backoffice` (`types/marca.ts`, `types/unidadMedida.ts`, `marcas.service.ts`, `unidadesMedida.service.ts`, `useMarcas.ts`, `useUnidadesMedida.ts`, `MarcasView.vue`, `UnidadesMedidaView.vue`, `endpoints.ts`) | Resuelto |
| 2026-09-03 | Modo oscuro fase 2 (Flutter): resto de pantallas con color hardcodeado migradas a `AppColors` | Tercera recomendación de la revisión de `market-design/`: la fase 1 del modo oscuro en Flutter (login/POS/dashboards) dejó fuera 7 archivos que seguían con constantes locales `_brand`/`_primary`/`_danger`/`_warning` fijas — se veían mal (colores claros fijos) al activar el tema oscuro del dispositivo. Migrados a `AppColors.of(context)` (mismo patrón de la fase 1): `caja_screen.dart`, `cuentas_por_cobrar_screen.dart` (`_warning` → `colors.pending`, no existe campo `warning`), `pendientes_error_screen.dart`, `barcode_scanner_screen.dart` (el visor de cámara en negro/blanco fijo se dejó igual a propósito, no es dependiente de tema), `cobro_sheet.dart`, `cliente_selector_sheet.dart` (de paso se migró también un rojo de error hardcodeado que tenía el mismo problema), y `connectivity_badge.dart` (no tenía constantes propias pero sí 3 colores de estado hardcodeados — mapeados a `colors.success`/`colors.pending`/`colors.danger` según el estado real de conexión). Cada `build(BuildContext)` de cada clase (varias por archivo en algunos casos) obtiene su propio `colors = AppColors.of(context)`. Verificado: `flutter analyze`/`dart format --set-exit-if-changed .`/`flutter test` limpios. **No verificado visualmente**: se intentó levantar `flutter run -d web-server` contra el backend local (con `CORS_ALLOWED_ORIGINS` ampliado para el puerto 8765) pero el renderer quedó en blanco/congelado más de un minuto sin avanzar (mismo tipo de inestabilidad de entorno ya vista con el backend en esta sesión) — se abortó el intento en vez de seguir insistiendo; queda pendiente para cuando haya un dispositivo/entorno estable donde probarlo | `market-flutter` (`caja_screen.dart`, `cuentas_por_cobrar_screen.dart`, `pendientes_error_screen.dart`, `barcode_scanner_screen.dart`, `cobro_sheet.dart`, `cliente_selector_sheet.dart`, `connectivity_badge.dart`) | Resuelto (sin verificación visual) |

## 9. Registro de avance

| Fase | Estado | PR/commit | Resultado de pruebas | Observaciones |
| --- | --- | --- | --- | --- |
| 1 — FEL | Parte A resuelta, parte B pendiente. Bandera temporal de desbloqueo desplegada en GCP | `85fd280` | `mvn verify` (con Docker): 533 unitarios + 8 IT, `BUILD SUCCESS` (suite completa sigue en 93 clases sin fallos tras la bandera) | Blindaje del simulado + correlativo con lock. Adaptador real necesita proveedor/credenciales. **2026-09-02**: `FEL_REQUERIDO_REAL=false` desplegado en GCP para sacar al backend del crash-loop en que quedó desde el 2026-08-28 (ver tabla de decisiones) — login real confirmado funcionando. Volver a `true` en cuanto haya adaptador FEL real. |
| 2 — Idempotencia POS | **Completa** (partes A, B y C, incluidos los 2 ítems que quedaban abiertos) | `a932a06`. **CI real en GitHub Actions confirmado verde** (run `33456149903`, los 5 jobs) | Backend: `mvn verify` (583 unitarios + 30 IT, `BUILD SUCCESS`). Flutter: `flutter analyze`/`flutter test` limpios; parte B verificada en Chrome contra backend/Postgres reales; parte C solo revisada por código, sin dispositivo real. Backoffice: `pnpm typecheck`/`lint`/`format:check`/`test`(53)/`build` limpios | Backend (caja/clientes/consulta ventas) + Flutter (UUID real, correlationId en venta online, conectividad real, cliente offline usable en la misma sesión, versionado de esquema Isar, minimización de PII local, logout bloqueado con pendientes) listos. Desinstalación marcada como no implementable vía app. De paso se encontraron y arreglaron dos bugs preexistentes: `AdminUserSeeder` y `ClientesApi.listar()` (pagination). **2026-08-31**: cerrados los 2 ítems que quedaban — `correlationId` obligatorio (`@NotBlank`) en toda venta HTTP (el bloqueador real era `market-backoffice`, no Flutter, que ya lo enviaba desde 2026-08-28) y "Consumidor Final" resuelto por nombre (`GET /clientes/consumidor-final`) en vez del id fijo `1`, más el test de concurrencia real de caja (`CajaConcurrenciaIT`) que quedaba pendiente. |
| 3 — Concurrencia | **Completa** | Sin commitear aún | `mvn verify` (con Docker): 552 unitarios + 24 IT, `BUILD SUCCESS` | `PESSIMISTIC_WRITE` (`findByIdConBloqueo`/`findAbiertaByTiendaIdConBloqueo`) en cliente (límite de crédito), caja (abrir/registrar movimiento/cerrar), CxC/CxP (cobro/pago/anular), gasto programado (generarPago), compra (recibir/anular), traslado (completar/anular), FEL (reintentar/anular), venta (completar/anular) y usuario (asignarTienda/asignarGrupo, serializando la regla "no asignación mixta" entre las dos tablas usuario_tienda/usuario_grupo_tienda). Caja además tiene índice único parcial para una sola sesión ABIERTA por tienda. `CHECK` de BD en los 10 módulos monetarios/de cantidad tocados, verificados con `CheckConstraintsIT`. `GlobalExceptionHandler` traduce `ConcurrencyFailureException` (deadlock/lock no adquirido) a 409 `CONFLICTO_CONCURRENCIA` en vez de 500 genérico. |
| 4 — Sesiones/seguridad | **Completa** salvo "tienda activa tras restaurar" (es de cliente, fuera de alcance) y rate limiter distribuido (no aplica hoy) | `b988a8a`, `110c3d0` | `mvn verify`: 574 unitarios + 24 IT, `BUILD SUCCESS`. En vivo: bloqueo de usuario invalida su token ya emitido de inmediato (sin esperar TTL), confirmado con curl. **CI real en GitHub Actions confirmado verde** (run `33423714537`, los 5 jobs) | `InMemoryLoginRateLimiter.limpiarBucketsLlenos()` purga buckets llenos. Autoservicio (`POST /auth/password`) y restablecimiento admin (`POST /usuarios/{id}/password/restablecer`). `debe_cambiar_password` bloquea el resto de la API. `SecurityVersionValidator` revalida `sver`; `POST /usuarios/{id}/sesiones/revocar` revoca sesiones. Caddy + Nginx con cabeceras de seguridad. **2026-08-31**: llaves dev/test retiradas del tracking de git (script `generar-llaves.sh` + paso nuevo en CI); rotación de llaves JWT probada (`JwtRotacionTest`); MFA evaluado y documentado (no implementado, decisión del usuario); política de contraseña/bloqueo/recuperación/baja documentada (`seguridad-desarrolladores.md` §13-14); "baja de empleados" pasó de dominio-sin-endpoint a 3 rutas reales (`desactivar`/`bloquear`/`activar`, permiso `USUARIOS_CAMBIAR_ESTADO`) — de paso se corrigió un bug de Fase 7 (`entidadId` de auditoría con el id del actor en vez del usuario objetivo). Bug de CI encontrado y corregido tras el push (`110c3d0`): `ProfileStartupIT` arranca perfil `local` real y lee `./local-dev/certs/dev-public.pem`, pero el workflow solo generaba llaves de test — faltaba el paso equivalente para `local-dev/certs`. |
| 5 — CI/pruebas | **Completa**, salvo Flutter checkout/cola/refresh (bloqueado, ver Fase 9) | `effa111`, `f37d1c7`. **CI real en GitHub Actions confirmado verde** (run `33550588137`, los 5 jobs) | `mvn verify`: 588 unitarios (583+5 nuevos) + 30 IT, `BUILD SUCCESS`. `pnpm typecheck`/`lint`/`format:check`/`test`/`build` backoffice limpios (21→53 tests). **CI real en GitHub Actions confirmado verde** (run `33449805070`, los 5 jobs, incluido Flutter y `docker-build`) | `.github/workflows/ci.yml` (5 jobs) + `.github/dependabot.yml` + Maven Wrapper + `packageManager` pnpm + `.fvmrc`. **2026-08-31**: 5 IT de flujo de negocio E2E nuevos (`e2e/*E2EIT.java` + helper `ApoyoE2E`, primer uso de login real + JWT real vía HTTP en este proyecto) + tests Vue de guards/refresh/permisos/composables (`guards.spec.ts`, `auth.store.spec.ts`, `permissions.store.spec.ts`, `useVentas.spec.ts`, `useCaja.spec.ts`). De paso se encontró que Spring Boot 4 separó `@AutoConfigureMockMvc` y Jackson 3 a paquetes nuevos — nueva dependencia `spring-boot-starter-webmvc-test` agregada. **2026-09-01**: pruebas contractuales de DTOs cerradas con snapshot tests livianos (`DtoContractSnapshotTest`, 5 DTOs de respuesta compartidos entre `market-flutter`/`market-backoffice`), decisión del usuario de no usar Pact. |
| 6 — Backups | **Completa** salvo el ensayo de volumen Docker perdido (necesita servidor de prueba) | `b4e8091`, `c8ba379`. **CI real en GitHub Actions confirmado verde** (run `33541819617`, los 5 jobs) | Verificado contra GCS/GitHub Actions **reales** (2026-09-01): `backup.sh` subió y verificó el bundle contra `gs://inven365-backups/`, y el `workflow_dispatch` de `backup-restore-drill.yml` (run `33541563596`) descargó, verificó checksum, descifró, restauró contra Postgres descartable y pasó el sanity check. Antes solo se había verificado con el backend `local` de `rclone`. SMTP real (Gmail) configurado y probado — `alert.sh` mandó un correo real, confirmado recibido por el usuario | `deploy/backup/*.sh` (backup/restore/check-freshness/alert), `docker-compose.yml`, `.env.example`, `.github/workflows/backup-restore-drill.yml`, `deploy/README.md`. **2026-09-01**: bucket `inven365-backups` + 2 service accounts + 3 secrets de GitHub + SMTP real (Gmail) creados/configurados por el usuario; bug real encontrado y arreglado (`rclone` necesitaba `bucket_policy_only` contra un bucket `--uniform-bucket-level-access`, ver tabla de decisiones). Falta solo: el ensayo de recuperación con volumen Docker perdido contra un servidor real. |
| 7 — Auditoría/observabilidad | Completa salvo dashboards (pospuestos, requieren Prometheus/Grafana) | Sin commitear aún | `mvn verify` (564 unitarios + 24 IT, `BUILD SUCCESS`); verificado en vivo contra backend local + Postgres real (`@Auditable` y `SecurityAuditPublisher` escribiendo en `audit_event`, `REFRESH_REUTILIZADO` reproducido, correlationId end-to-end, `/actuator/prometheus` con JWT) | Nuevo módulo `auditoria` (tabla append-only + AOP `@Auditable`), `docs/auditoria.md` reescrito, `CorrelationIdFilter`, `AlertaEmailService`, `micrometer-registry-prometheus`. 2 bugs encontrados y corregidos en la verificación local (actor "anonymousUser" en login, `/actuator/health` DOWN por mail health indicator). |
| 8 — Backoffice | "Base + quick wins" completa, resto pendiente | `8b8b7b9`, `e33afa6` | `pnpm typecheck`/`pnpm lint`/`pnpm test` (21 tests) limpios, `pnpm build` exitoso. Verificado en Chrome contra backend local real: login → recarga de página mantiene sesión (antes caía a `/login`); navegación rápida entre 6 módulos paginados sin errores de consola. **CI real en GitHub Actions confirmado verde** (run `33428154424`, los 5 jobs) | ESLint 9 (flat config) + Prettier en CI, `signal`/`AbortController` en `ApiClient` + 8 composables paginados server-side, refresh silencioso en `authGuard`. División de vistas grandes, componentes reutilizables, validación de formularios, accesibilidad y Playwright quedan para otra pasada. Un push necesitó una segunda corrección: `InventarioView.vue` no había convergido en una pasada de Prettier (interpolación al límite de `printWidth`) — CI lo detectó porque no reconfirmé `format:check` tras el `format --write` inicial. |
| 9 — Flutter | "Tests + dividir pos_screen" completa, resto pendiente (necesita hardware o decisiones del usuario) | `3087abe` | `flutter analyze` limpio, `flutter test`: 62 tests (antes 1), `dart format --set-exit-if-changed .` limpio, `flutter build web` exitoso. **CI real en GitHub Actions confirmado verde** (run `33436255761`, los 5 jobs, incluido `flutter build apk --release`) | `pos_screen.dart` (826 líneas) dividido en `presentation/pos/*.dart`. Tests nuevos: dominio de carrito, wiring de `CarritoNotifier`, clasificación de `ApiException`, parsers de `Venta`/`CuentaPorCobrar`/`ProductoCatalogo`, widgets de `TiendaPickerScreen`/`CobroSheet`/`PendientesErrorScreen`. `CheckoutNotifier`/auth-refresh/`SyncEngineNotifier` quedan sin test unitario puro (acoplados a `ApiClient`/Isar, sin librería de mocking en el proyecto). |
| 10 — Funciones comerciales | Pendiente | | | |
| 11 — Rendimiento | Auditoría + hallazgos accionables completos, resto pendiente de volúmenes de negocio del usuario | `cd9187d` | `mvn verify` backend (con Docker), `pnpm typecheck`/`lint`/`format:check`/`test`/`build` backoffice — todo limpio. Verificado contra backend real: los 3 listados migrados (CxP/FEL/notificaciones) devuelven el envelope paginado correcto (`curl`), nuevo endpoint de CxC por venta confirmado (404 esperado sin cuenta), vistas renderizadas en Chrome sin errores de consola. **CI real en GitHub Actions confirmado verde** (run `33444282148`, los 5 jobs) | HikariCP explícito, límite de tamaño de imagen de producto (2MB), fix real de `PermisosEfectivosResolverImpl.invalidar()` (nunca se llamaba, ni en single-instance), `CuentaPorCobrarRepository.findByVentaId` + endpoint nuevo, y paginación server-side agregada a Cuentas por Pagar/FEL/Notificaciones (backend + backoffice) — los 3 crecían sin límite natural igual que ventas/CxC. Benchmark de Argon2 en dev (~56.6ms, con margen real pero sin subir el costo sin medir en hardware de producción). Volúmenes esperados, `EXPLAIN ANALYZE` con datos representativos y prueba real de múltiples instancias quedan pendientes — necesitan una decisión de negocio o infraestructura desplegada que no puedo generar solo. |
