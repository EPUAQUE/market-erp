# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

`market-flutter` is the **POS (point of sale) client** for **Market**, a Retail
Multi-Tienda ERP. It is a tablet-first app (10"-12" Android tablets) used by two
roles — **VENDEDOR** and **ENCARGADO** — for a full working day of fast-paced sales.
It is a separate app from `market-backoffice` (Vue) — no shared code, both consume
the same `market-backend` REST API (see that repo's `ARCHITECTURE.md` for the
backend's Modular Monolith design and `seguridad-desarrolladores.md` for the auth
model).

**What's live:** the scaffold — `flutter create` project (Android + web
targets), `ApiClient` (Dio, single-flight refresh, `ApiException`
normalization), the `auth` feature end-to-end (`AuthNotifier`, login/logout,
tienda auto-pick when the user has exactly one), `GoRouter` with the
session+tienda guard — plus, as of the Ventas phase, a real POS screen:
catalog (`productos` feature, combining `GET /productos` + the new
`GET /productos/tiendas/{id}` + `GET /inventario/tiendas/{id}` client-side),
cart (`ventas/domain/carrito.dart`, pure Decimal math), and the full checkout
flow (`CobroSheet` — Efectivo/Tarjeta/Transferencia/Crédito/Mixto, cambio
calculado, alta rápida de cliente vía `clientes` feature) plus the `caja`
feature (abrir turno, registrar ingreso/egreso, cerrar con diferencia contra
el saldo esperado — gated behind `CAJA_VER` in both the router redirect and
the POS app bar icon). Verified end-to-end against the real backend in Chrome
(`flutter run -d web-server`): a cash sale (crear → completar → cobrar por el
total, cuenta por cobrar queda COBRADA), a credit sale (crear cliente nuevo →
completar sin cobrar, cuenta por cobrar queda PENDIENTE), and a full caja
cycle (abrir con Q500 → ingreso de Q50 → cerrar contando Q545, diferencia
Q-5 calculada correctamente) were all driven through the real UI and confirmed
against the database. Barcode scanning is also live: `BarcodeScannerScreen`
(`mobile_scanner`) opens full-screen from the scan icon next to the search
field, feeds a detected code through the same `_onCodigoEscaneado` path the
manual search bar uses, and always shows a manual-entry field alongside the
camera preview (not just as an error fallback) — verified in Chrome by typing
a real `codigoBarras` into that field and watching it land in the cart.
Camera permission/feed itself wasn't exercised (no real camera in the
headless Chrome this was tested in), only that the widget doesn't crash
without one and that the manual path works end-to-end.

Offline-first core is also live: a local catalog mirror + a FIFO sync queue
behind a `LocalStore` abstraction, covering ventas (crear+completar+cobro),
movimientos de caja, and clientes nuevos (see "Offline-first architecture"
below). **Cobros sueltos** — paying down an existing `CuentaPorCobrar`
outside the automatic cobro fired right after completing a venta — now has
its own screen (`cuentas_por_cobrar` feature, "Cobros sueltos" below) but is
**online-only**; it was never in the offline queue design and isn't now.

`dashboard_vendedor` and `dashboard_encargado` are live too, both consuming
the same `GET /api/v1/dashboard/tiendas/{id}` (`dashboard` feature's
`dashboardResumenProvider`) — a single `DashboardRouterScreen` picks which one
to show via `sesion.can('CAJA_VER')` (the same permission `PosScreen` already
uses to distinguish ENCARGADO from VENDEDOR/CAJERO). Reached from a dashboard
icon in the POS app bar, gated by `DASHBOARD_VER`. `dashboard_encargado` shows
the full resumen (ventas, caja, inventario, cxc/cxp con aging + top
pendientes, alertas, sugerencias de compra/traslado); `utilidadMesTotal`/
`margenPromedioMes` render only when non-null (gated server-side by
`DASHBOARD_FINANCIERO_VER`, which neither CAJERO nor ENCARGADO_TIENDA has
today — intentional). `dashboard_vendedor` shows a reduced subset (ventas
hoy/mes, ticket promedio) plus an explicit disclaimer: **meta de ventas y
ranking interno por vendedor no existen** — the backend has no `usuarioId` in
`/me` and no per-vendedor filter on ventas, so "mis ventas" is not
computable client-side; showing store-wide figures with a note beats
fabricating a number. `dashboard_encargado` was driven end-to-end in Chrome
against the real backend and its numbers cross-checked against a direct
`curl` of the endpoint; `dashboard_vendedor` shares the same provider and
`_StatCard` composition and has since been click-tested the same way — see
below. Neither was exercised on a real Android device yet.

**`dashboard_vendedor` click-test — done this phase**: no CAJERO-only test
account existed, so one was created (`POST /api/v1/usuarios` +
`POST /api/v1/usuarios/{id}/tiendas` with the `CAJERO` rol, seeded in
`seguridad/005-seed-catalogo.xml`; no `usuario` desactivar endpoint exists
in `UsuarioController` today, so this test account — `cajero_qa` — is left
active rather than hand-editing the DB). Confirmed via `GET /auth/me` that
its `permisos` matched `CAJERO` exactly (has `DASHBOARD_VER`, lacks
`DASHBOARD_FINANCIERO_VER` and `CAJA_VER`) before touching Chrome. Logged in
as `cajero_qa` in Chrome: landed on POS (never a dashboard, as designed), no
Caja icon in the app bar (correctly gated by the missing `CAJA_VER`), and
the dashboard icon opened "Mi rendimiento" — the reduced `dashboard_vendedor`
view with the "meta de ventas y ranking interno... no están disponibles"
disclaimer, not the full `dashboard_encargado` one. Numbers cross-checked
against `curl GET /api/v1/dashboard/tiendas/1`: `ventasHoyTotal: 25.5000` /
3 ventas, `ventasMesTotal: 71.0000` / 8 ventas, `ticketPromedioMes: 8.88` —
all matched what the screen showed exactly.

One thing this phase's `ProductosApi.listarCatalogo` glosses over, worth
knowing before extending it: it re-fetches and re-joins all three lists on
every catalog load (no caching beyond the offline `LocalStore` mirror — fine
for a small dev catalog, would need addressing before a store with hundreds
of products).

Category filtering in the left column of `PosScreen` is live: `categoriasProvider`
(`GET /api/v1/categorias`, filtered to `ACTIVA`) feeds a chip list, tapping one
sets `categoriaSeleccionadaProvider` (`null` = "Todos"), and
`productosFiltradosProvider` applies it alongside the search text. Getting
this working for real POS users required a backend permission fix: `CATEGORIAS_VER`
was only seeded for ADMIN (`categorias/001-categoria.xml`) — CAJERO and
ENCARGADO_TIENDA would have hit a 403 opening the POS. Added
`seguridad/007-seed-categorias-ver-operativo.xml` to grant it to both. **This
migration needs the backend restarted to take effect** — Liquibase runs on
app startup, there's no separate migration-only command wired up for this
project. Verified in Chrome with a real second category: selecting a category
with no products showed the empty state, selecting one with products showed
only those, "Todos" showed everything again. Favoritos from the original
brief is still not built — there's no favorito concept anywhere in
`market-backend` (no per-user product marking at all), so it needs its own
design pass before implementing, not just wiring an existing endpoint.

The full architecture (navigation, offline sync design, screen-by-screen textual
mockups, Riverpod state shapes, conflict resolution) is documented in the
published design artifact linked from the project's working notes — this file
stays the durable, low-churn summary; the artifact is the detailed reference.

## Product goal

A sale must be completable in under 10 seconds during a rush. The home screen
after login is **never a dashboard** — it's always **POS · Nueva Venta**. Every
design decision (fewer taps, no incidental dialogs, big touch targets) serves
that goal. **Modo Venta Rápida** (built) is a session-only toggle
(`modoVentaRapidaProvider`, a bolt icon in the POS app bar) for peak-hour
queues: hides the categorías column, drops product-card images and cart
subtitles for a denser grid/list, and skips the barcode scanner's page
transition (`Duration.zero`) — never changes business rules, only
presentation. Verified in Chrome: toggling it live hides/shows the categorías
column, the grid goes from 4 to 6 columns without images, and the cart row
drops its "Q x c/u" subtitle.

## Roles

- **VENDEDOR**: crear ventas, buscar/crear clientes (captura mínima: nombre,
  teléfono, NIT), cobrar, consultar inventario (existencias, no costos), ver su
  propio rendimiento (ventas del día/mes, meta, ranking interno). Cannot see
  costs, utilidades, financial reports, compras, or system config.
- **ENCARGADO**: everything VENDEDOR can do, plus abrir/cerrar caja, registrar
  movimientos de caja, reportes de tienda, inventario detallado, traslados,
  alertas de abastecimiento, cobros pendientes.
- Both roles are scoped to the single tienda chosen at login (no tienda
  switcher, unlike the backoffice) — see Auth below.

## Stack

- Flutter, Dart null-safety.
- **Riverpod** for state management (`NotifierProvider`/`AsyncNotifierProvider`
  over legacy `StateNotifier`).
- **GoRouter** for navigation, with route guards mirroring the backend's
  permission model (see Auth below).
- **Dio** for HTTP, with interceptors for auth headers, correlation ID, and
  error normalization — mirrors the backoffice's `ApiClient` conventions.
- **Isar** (`isar_community`) for local persistence, behind a `LocalStore`
  abstraction (`core/db/local_store.dart`) — today it backs the catalog
  mirror and the ventas sync queue (see "Offline-first architecture" below).
  Never import `isar_community` or an `@collection` model directly from
  feature code (`catalogo_provider.dart`, `checkout_notifier.dart`,
  `sync_engine.dart`) — only `local_store_io.dart` may. Isar's generated
  schema files (`*.g.dart`) contain 64-bit collection-id literals that
  dart2js/DDC cannot represent exactly, which is a **compile** error, not a
  lint, if that file ends up anywhere in the web build's import graph — even
  behind a runtime `if (kIsWeb)` check, since that doesn't stop the compiler
  from including the file. `local_store_selector.dart` conditionally exports
  `local_store_io.dart` (native) or `local_store_web.dart` (web, no Isar
  import at all, all no-ops) the same way `with_credentials.dart` already
  did for the auth cookie — this is why the split exists, not just for
  cleanliness.
- **flutter_secure_storage** backs the Android/iOS persistent `CookieJar`
  (OS keychain/keystore, via `SecureCookieStorage`) — not used on web, where
  the browser owns the refresh-token cookie directly (see Auth below).
- **dio_cookie_manager** + **cookie_jar** — native-only refresh-token cookie
  handling (see Auth below); never imported on web (conditional export).
- **mobile_scanner** for barcode scanning (código de barras del producto).
- **decimal** package for all monetary values — never `double`.

## Commands

```bash
flutter pub get
flutter run -d chrome             # fastest iteration loop during development
flutter run                       # launch on connected Android device/emulator
flutter test                      # unit + widget tests
flutter analyze                   # static analysis, must be clean
dart format --set-exit-if-changed .
flutter build apk / web           # per target
```

Configure the backend URL per environment (e.g. `--dart-define=API_BASE_URL=...`),
never hardcode it. Chrome is the primary dev-loop target (no emulator needed) —
`flutter run -d web-server --web-port=<port> --dart-define=API_BASE_URL=...`
serves it without launching a browser tab itself, useful when driving it from
automation. Validate on an Android device/emulator before closing out a phase,
since `mobile_scanner` and Isar's file-based storage behave differently on web
than on-device (the web build should degrade the scanner to manual entry, not
crash). The backend's `CORS_ALLOWED_ORIGINS` must include whatever port this
app is served from, comma-separated alongside the backoffice's — it's a plain
Spring list, not a single value.

`pubspec.yaml` pins `path_provider_foundation` to `2.4.2` via
`dependency_overrides`. Versions ≥2.6.0 pull in the `objective_c` package
(macOS/iOS native bindings via native-assets hooks), whose build hook fails on
Windows when the user's home path contains a space — a real, reported Windows
native-assets bug, unrelated to this app, that otherwise blocks `flutter test`
entirely. Since this project doesn't target macOS/iOS, don't remove the
override to "fix" a stale-dependency warning.

### Android emulator, verified

Ran end-to-end on a Pixel Tablet AVD (Android 15 / API 35) against the real
backend — login, catálogo, carrito, cobro (efectivo con cambio) all worked
identically to the Chrome build. Two things needed for that to work, both
environment-specific, not app bugs:

- **`--no-enable-impeller` is required on this emulator.** Impeller (the
  default renderer) fails to link its GLES shader pipeline against the
  emulator's software renderer (SwiftShader) — `Could not link pipeline
  program`, hundreds of dropped frames, UI stuck on the splash screen forever.
  `flutter run -d <emulator-id> --no-enable-impeller` fixes it outright. Try
  without the flag first on a real device or a hardware-accelerated
  emulator — this is specifically a software-GL limitation.
- **Forcing landscape didn't take effect on this AVD.** `MainActivity` has
  `android:screenOrientation="landscape"` (plus `SystemChrome
  .setPreferredOrientations` in `main.dart` for parity on other platforms),
  which is normally sufficient — but this AVD logs `Initializing Window
  Extensions ... activity embedding enabled=true`, Android's large-screen
  compatibility layer, which appears to letterbox the app inside whatever
  orientation the emulator itself is in rather than honor the fixed
  orientation. Reachable workaround for this emulator specifically: rotate the
  emulator itself — `adb shell settings put system accelerometer_rotation 0`
  then `adb shell settings put system user_rotation 1` — before launching.
  Whether a real tablet respects the manifest flag properly is untested; don't
  assume this AVD's behavior generalizes.

`10.0.2.2` is the emulator's alias for the host machine — use
`--dart-define=API_BASE_URL=http://10.0.2.2:8080` to reach a backend running
on localhost from inside the emulator (`localhost` there means the emulator
itself). CORS doesn't apply to native Android traffic at all, only to the
Chrome/web build.

**Known-flaky on this machine: virtio-wifi can wedge and never recover.**
Symptom: `wlan0` shows `NO-CARRIER`/`state DOWN` in `adb shell ip addr`, every
outbound request (even `curl` run from inside the emulator's own shell to
`10.0.2.2`) fails with "Network is unreachable", and the emulator's log
(`-verbose`) shows `VirtIO WiFi: unexpected full virtqueue`. This survived a
full guest reboot (`adb reboot`) and a full process restart (`adb emu kill` +
relaunch) in one incident — it's a QEMU/virtio-wifi backend issue, not
something Dart/app code caused or can fix. `-feature -Wifi` does not help
(no network device comes up at all, not even a legacy one — this AVD has no
non-WiFi network path). If this recurs: don't keep retrying app-level login
theories — first confirm with a plain `curl` from inside the emulator shell
whether `10.0.2.2:8080` is reachable at all before assuming the bug is in the
Flutter code.

## Architecture

Clean Architecture + Feature First, SOLID:

- `lib/core/` — Dio client, interceptors, error mapping, `LocalStore`
  (`db/`, Isar-backed on native, no-op on web), connectivity monitor, sync
  engine, app-wide config.
- `lib/features/<module>/` — one folder per module (`auth`, `ventas`, `caja`,
  `productos`, `clientes`, `cuentasporcobrar`, `dashboard_vendedor`,
  `dashboard_encargado`), each with:
  - `data/` — DTOs, Isar collections (local schema), a thin API client using the
    shared Dio instance, and a repository that decides local-vs-remote per the
    offline strategy below.
  - `domain/` (only where real client-side business rules exist — cálculo de
    cambio/vuelto, validación de stock local antes de agregar al carrito,
    resolución de conflictos de sync) — plain Dart, no Flutter/Riverpod imports.
  - `application/` — Riverpod providers/notifiers orchestrating `data` (+
    `domain` when present).
  - `presentation/` — screens and widgets, consuming providers only.
- `lib/router/` — GoRouter config; route guards check auth state and permission
  codes before entering a route, redirecting to login/forbidden otherwise. The
  initial route after login is always the POS screen, never a dashboard.

## Auth & permissions

Same model as the backend and backoffice (`market-backend/seguridad-desarrolladores.md`,
`market-backoffice/CLAUDE.md`) — do not diverge from it:

- Login (`POST /api/v1/auth/login`) returns a short-lived **access token** (JWT)
  in the JSON body. The access token is kept **in memory only**
  (`TokenService`, a plain singleton read by the Dio interceptor and by
  `AuthNotifier`), never written to disk.
- The refresh token **never appears in the response body** — `AuthController`
  sets it as an `HttpOnly, Secure, SameSite=Strict` cookie
  (`refresh_token`, path `/api/v1/auth`). This app's Dart code never reads or
  stores it directly:
  - **Web**: the browser owns the cookie. Dio's `BrowserHttpClientAdapter`
    must have `withCredentials = true` (see `core/network/with_credentials.dart`)
    or the cookie is silently dropped and every refresh 401s.
  - **Android/iOS** (built): Dio doesn't manage cookies on its own, so
    `ApiClient` attaches a `CookieManager` (`dio_cookie_manager`) wrapping a
    `PersistCookieJar` (`cookie_jar`) to both the main `dio` and the
    `_refreshDio` — same jar instance for both, selected only on native via
    `core/network/cookie_manager_selector.dart` (same conditional-export
    pattern as `with_credentials.dart`, so `cookie_jar`/`dio_cookie_manager`
    never enter the web build's import graph at all). The jar persists
    through `SecureCookieStorage` (`core/network/secure_cookie_storage.dart`),
    a `cookie_jar` `Storage` backed by `flutter_secure_storage` (Android
    Keystore/iOS Keychain) instead of the package's default flat-file
    storage — this is what `flutter_secure_storage` in the dependency list is
    actually for. `ApiClient.instance.clearCookies()` wipes the jar and is
    called from `AuthNotifier.logout()` — without it a revoked refresh token
    would sit cached on the device after logout. **Not yet verified
    end-to-end** (real refresh-after-app-restart on a device/emulator) — this
    session's AVD has a virtio-wifi networking bug blocking any native
    verification (see "Known-flaky on this machine" below); only `flutter
    analyze`/`test` and a web-build compile check confirm this doesn't touch
    or break the web path.
- A 401 from Dio triggers a single in-flight `POST /api/v1/auth/refresh`
  (cookie sent automatically); concurrent requests queue behind it. If refresh
  fails while online, clear the access token and route to login. If it fails
  while offline (no network at all, not an auth rejection), stay logged in
  against the last known permissions/tienda and let queued writes retry once
  connectivity — and a successful refresh — return.
- Authorization is flat RBAC + tienda scope: permission codes (`VENTAS_CREAR`,
  `CAJA_CERRAR`, …) plus the tienda(s) the user is assigned to. The POS only
  ever operates against the single tienda chosen at login — every request
  carries that `tiendaId` explicitly, never inferred silently.
- Logout revokes the refresh token server-side and clears secure storage and
  the local Isar database (no leftover data for the next person logging in on
  a shared tablet).

## Money — non-negotiable

Never use `double` for money (precios, totales, vuelto, saldos, límites de
crédito). Use the `decimal` package end-to-end from parsing the API response to
rendering — the backend sends amounts as strings for this exact reason. Match
the backoffice's convention of treating amounts as exact decimal values, never
floats.

## Offline-first architecture

The POS must keep selling through a connectivity loss — a sale should never
stall because the network dropped. This is a real, documented requirement (not
speculative). **What's actually built today** is only the first two bullets
below (catalog mirror as a read-through cache, and a ventas-only sync queue)
— everything else in this section (cobros/movimientos de caja/clientes
nuevos offline, the stock guard, conflict resolution) is still the original
design, not implemented, kept here as the target to build toward next:

- **Local mirror** (built, `productos` catalog only): `catalogoProvider`
  fetches network-first and writes through to the `LocalStore` on success; on
  a network failure it falls back to whatever was last cached, so the POS
  keeps selling with stale prices/stock rather than showing an error screen.
  This is network-first-then-cache-fallback, not the stricter
  always-local-then-refresh-in-background the rest of this section
  describes — a deliberate simplification. Categorías, clientes, precios,
  stock disponible, and permisos are **not** mirrored yet; those reads still
  require network.
- **Writes while offline** (built: ventas, clientes nuevos, movimientos de
  caja): `CheckoutNotifier`, `ClienteSelectorSheet`, and `CajaActionsNotifier`
  all branch on `redDisponibleProvider` — offline, each writes its own
  pending-item type to the `LocalStore` instead of calling the API (the
  vendedor never sees a network error mid-action for these three). **Cobros
  sueltos now exist (`cuentas_por_cobrar` feature, see below) but are
  online-only** — no offline queue for them; attempting one offline shows a
  network error like any other unqueued action. See "Clientes y movimientos
  de caja offline" below for the caja/clientes offline design and its one
  real limitation (a cliente created offline can't be used in the same
  offline session's venta).
- **Stock guard**: if the device is offline and local stock for a producto is
  at 0, that producto cannot be added to a new sale — show why, don't silently
  allow overselling. An encargado can still record an authorized manual
  adjustment once back online; there is no offline override.
- **Sync engine** (built: ventas, clientes, movimientos de caja queues):
  `SyncEngineNotifier` drains all three on reconnect — clientes first, then
  ventas, then movimientos de caja (order doesn't matter between them; see
  "Clientes y movimientos de caja offline" below for why). A network failure
  on one item stops the drain of *that* queue only (the other two keep
  going); retried on
  the next reconnect, order preserved); a business failure (e.g. product no
  longer sellable) marks that item with `mensajeError` and continues with the
  rest, so one bad sale can't block the queue — it surfaces for an encargado
  to review manually instead. The `correlationId` is generated client-side
  and the backend now consumes it for idempotency (see "Idempotencia de
  ventas offline" below) — there's still no retry-with-backoff, a queued item
  either syncs on the next reconnect or waits for the one after.
- **Conflict resolution**: the backend is always the source of truth for
  stock and precios — a synced venta whose line no longer matches server stock
  is flagged for encargado review, never silently adjusted or dropped. Cobros
  against a cuenta por cobrar that changed server-side (e.g., another device
  already cobró it) are rejected by the backend's own balance check and surface
  as a reconciliation item, not a crash. **The two backend contracts this
  relies on are verified this phase** (see "Conflict resolution — verified
  against the real backend" below) — the client-side `SyncEngine` loop logic
  that consumes them (mark-and-continue vs. stop-the-drain) is still only
  code-reviewed, not click-tested, since that requires a real offline device.
- **Connectivity indicator**: always visible, three states — Conectado /
  Sincronizando / Sin conexión — and never blocks interaction.

## Known backend gaps (design around these, don't invent fields silently)

Three gaps this brief originally assumed away have since been closed on the
backend (`Venta.metodoPago`, `Cliente.limiteCredito`, per-cobro payment-method
breakdown — see below); this section now only covers what's still genuinely
missing:

- **Offline queue has no per-method breakdown.** `NuevaVentaPendiente` still
  stores one `montoACobrar` + one `metodoPago` string per queued venta, not a
  map of amounts per channel — see "Desglose de método de pago por cobro"
  below for why `Mixto` is blocked offline entirely rather than faked.
- ~~No credit-limit *enforcement*~~ — **closed**, see below.
- ~~No per-cobro payment-method breakdown~~ — **closed**, see below.

### Método de pago (`Venta.metodoPago`) — built this phase

`Venta` now has a required `metodoPago` (`EFECTIVO`/`TARJETA`/`TRANSFERENCIA`/
`CREDITO`/`MIXTO`) set at `crear()` time (`CrearVentaRequest.metodoPago`,
`@NotNull`) — persisted in a nullable `metodo_pago` column
(`ventas/003-metodo-pago.xml`; nullable because ventas created before this
migration have no real value to backfill, not because new ventas can omit
it). `market-flutter`'s `VentaApi.crear()` sends it (`metodoPagoToJson`,
matching the enum name uppercased); the sync queue reconstructs it from the
`LocalStore`-persisted string via `metodoPagoFromJson` (case-insensitive on
purpose — local storage keeps the Dart enum's lowercase `.name`) when
draining an offline sale. This is purely a reporting field — it changes
nothing about how `completar()`/`registrarCobro` behave.

### `Cliente.limiteCredito` — built this phase

Optional (`null` = no límite definido, not the same as Q0), settable via
`CrearClienteRequest`/`ActualizarClienteRequest` (`limite_credito` column,
`clientes/002-limite-credito.xml`, nullable, no backfill). `market-flutter`'s
`ClienteSelectorSheet` quick-create form has an optional "Límite de crédito"
field; `CobroSheet`'s venta-a-crédito step shows the selected cliente's
límite (or "Sin límite de crédito definido" when null).

### Enforcement del límite de crédito — built this phase

`VentaServiceImpl.completar()` now checks it: when `venta.metodoPago` is
`CREDITO` **or `MIXTO`**, before mutating any state it calls
`ClienteService.obtener(clienteId)` (a new method — Ventas already had
`InventarioService`/`CuentaPorCobrarService` as permitted cross-module
dependencies, `ClienteService` joins them the same way) and sums the
cliente's `PENDIENTE` `CuentaPorCobrar.saldoPendiente` **for the current
tienda only** (`cuentaPorCobrarService.listarPorTienda`, filtered) plus this
venta's total. If that exceeds a non-null `limiteCredito`, it throws
`LimiteCreditoExcedidoException` (409 `LIMITE_CREDITO_EXCEDIDO`) — since the
check runs before `venta.completar()`, inventory movements, or
`CuentaPorCobrar.crear()`, a rejected venta stays untouched in `BORRADOR`,
not half-applied. `limiteCredito == null` still means unrestricted.
Deliberately **tienda-scoped, not company-wide** — this app already scopes
every operation to one tienda at a time, and `CuentaPorCobrarRepository`
only supports `findByTiendaId`; summing a cliente's exposure across every
tienda they've shopped at would need a new cross-tienda query this phase
didn't add.

**`MIXTO` gap closed this phase**: `MIXTO` used to bypass this check
entirely (only `CREDITO` triggered it), even though `completar()`
unconditionally creates a `CuentaPorCobrar` for the *full* total regardless
of `metodoPago` — the actual paydown only happens via separate
`registrarCobro` HTTP calls the client fires *after* `completar()` returns
(one per tramo for `MIXTO` — see "Desglose de método de pago por cobro"
above). Nothing makes that second step atomic with completion, so treating
`MIXTO` as "definitely fully paid" was optimistic — a dropped connection
mid-checkout, or those calls simply never firing, left real unchecked
exposure. Now `MIXTO` validates against the full total exactly like
`CREDITO`, accepting a known trade-off: a cliente already near their limit
can have a large, fully-paid `MIXTO` sale rejected at `completar()`-time even
though the tramos would have covered it a moment later — deliberately
conservative, since the backend has no way to know at this point whether
those cobros will actually succeed.

Verified against the real backend via `curl`: a cliente with `limiteCredito=5`
and a Q8.50 venta a crédito got a 409 on `completar()`, and the venta stayed
`BORRADOR` (confirmed with a follow-up `GET`) — no inventory movement, no
`CuentaPorCobrar` created; a second cliente with `limiteCredito=1000` and the
same Q8.50 venta completed normally (200, `COMPLETADA`). Re-verified the same
two cases for `MIXTO` after closing the gap: a cliente with `limiteCredito=5`
and a Q8.50 `MIXTO` venta got the same 409, staying `BORRADOR`; a cliente
with `limiteCredito=1000` completed normally. `VentaServiceImplTest` covers:
within-limit passes for both `CREDITO` and `MIXTO`, over-limit throws and
touches nothing for both, `null` límite is unrestricted, existing pending
saldo counts toward the projection, and `EFECTIVO`/`TARJETA`/`TRANSFERENCIA`
ventas never call `ClienteService` at all.

Both migrations, plus `seguridad/007-seed-categorias-ver-operativo.xml` from
the categorías-filter phase, needed the backend restarted to take effect
(Liquibase runs on app startup) — confirmed applied (`limite_credito` and
`metodo_pago` columns both present) after restarting via `mvn spring-boot:run`.
`mvn test` passes (all unit tests updated for the new constructor/method
signatures across `ventas` and `clientes`, plus their cross-module test
fixtures in `dashboard`/`fel`/`reportes`), and `flutter analyze`/`test` pass.

**Verified end-to-end in Chrome against the real backend**: a full venta a
crédito — add a product, open `CobroSheet`, pick Crédito, create a new
cliente with `limiteCredito=1000` inline (the sheet shows "Límite de
crédito: Q 1000" immediately after creating it), confirm — and cross-checked
by `curl`: the resulting `Venta` has `metodoPago: "CREDITO"` and
`estado: "COMPLETADA"`; its `CuentaPorCobrar` has `estado: "PENDIENTE"`,
`cobros: []`, `saldoPendiente` equal to the full total (no cobro fired, as
expected for credit); the two pre-existing ventas from earlier phases still
read back `metodoPago: null`, confirming historical rows weren't fabricated
a value.

**Environment gotcha hit while restarting for this**: relaunching the
backend via `mvn spring-boot:run` does *not* inherit env vars an IDE run
configuration had set — it came back up with `CORS_ALLOWED_ORIGINS`
defaulted to just the backoffice's `http://localhost:5173`, silently
blocking every request from this app's `http://localhost:8765` (Chrome dev
server) with a 403 on the CORS preflight `OPTIONS`. Symptom looked exactly
like "wrong credentials" in the UI (`LoginScreen` shows the same generic
error for any login failure, network or auth — a pre-existing gap, see
`LoginScreen` in the auth section above) despite the backend accepting the
same credentials fine over `curl`. Diagnosed by reading actual network
requests in the browser tab, not by trusting the on-screen error. If this
backend needs restarting again outside the IDE, pass
`CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8765`
explicitly.

### Desglose de método de pago por cobro — built this phase

`Cobro` now carries its own `metodoPago` — a **separate, narrower** enum
(`cuentasporcobrar.domain.model.MetodoPago`: `EFECTIVO`/`TARJETA`/
`TRANSFERENCIA` only, no `CREDITO`/`MIXTO`) from `Venta.metodoPago`. The
distinction is deliberate: `Venta.metodoPago` is the checkout-level intent
(and `CREDITO`/`MIXTO` are meaningful there — "don't collect now" / "split
across channels"), while a `Cobro` is always money actually received through
one concrete channel — "credit" and "mixed" describe zero or many cobros,
never one, so they're not valid values for a single cobro's own field.
Persisted in a nullable `metodo_pago` column on `cobro`
(`cuentasporcobrar/002-metodo-pago-cobro.xml`; nullable/no-backfill, same
reasoning as `ventas/003-metodo-pago.xml` — historical cobros never recorded
a real channel). `RegistrarCobroRequest.metodoPago` is `@NotNull` — every new
cobro must declare its channel; `CobroResumen`/`CobroResponse` both expose it.

`market-flutter`'s `CuentaPorCobrarApi.registrarCobro()` now requires a
`metodoPago` too (reuses the venta-level Dart `MetodoPago` enum for
convenience — the 3 shared string values match — but callers must never pass
`credito`/`mixto`). `CobroSheet`'s "Mixto" chip finally does something: it
swaps the single "Monto recibido" field for one input per channel
(Efectivo/Tarjeta/Transferencia) with a running "Total ingresado: Q X de Q Y"
readout, and only enables the confirm button once the sum equals the total
exactly (no change-making in a split payment). `CheckoutNotifier.confirmar()`
gained an optional `desglose` param (`Map<MetodoPago, Decimal>`); for `Mixto`
it calls `registrarCobro` once per non-zero entry, each tagged with its own
channel, instead of the old single call that silently treated Mixto like
Efectivo. Non-mixto sales now tag their one `registrarCobro` call with the
chip's own method instead of leaving it untagged.

**Deliberately out of scope**: the offline queue (`NuevaVentaPendiente`)
still has no schema for a per-channel breakdown — it stores one
`montoACobrar` + one `metodoPago` string per queued venta. Rather than fake
a channel for a split payment made offline, `CheckoutNotifier.confirmar()`
throws (`'El pago mixto requiere conexión a internet.'`) if `Mixto` is
selected while offline, before ever touching the queue. `SyncEngine` tags its
one queued cobro with the venta's own `metodoPago` (`credito`/`mixto` never
reach that call — credito never has a `montoACobrar`, and mixto is blocked
upstream) — safe today, but brittle if that invariant ever changes; worth
revisiting if the offline queue grows a real breakdown.

Verified against the real backend via `curl`: created a cliente, a `MIXTO`
venta for Q8.50, completed it, then registered two cobros on its
`CuentaPorCobrar` — `EFECTIVO 5.00` then `TARJETA 3.50` — the account moved
PENDIENTE → COBRADA with `saldoPendiente: 0.0000` and both cobros in the
response each carrying their own `metodoPago`; a pre-existing cobro from an
earlier phase still reads back `metodoPago: null`, confirming the migration
didn't fabricate history. A `registrarCobro` call omitting `metodoPago`
returned 400. `mvn test` passes (domain/service/controller tests updated for
the new constructor/method signature in `cuentasporcobrar`); `flutter
analyze` passes on `lib/features/ventas` and `lib/core/sync`.

**Verified end-to-end in Chrome against the real backend**: added Coca Cola
600ml (Q8.5) to the cart, opened `CobroSheet`, picked Mixto — the three
per-channel fields rendered — entered Efectivo Q5.00 + Tarjeta Q3.50, watched
"Total ingresado: Q 8.5 de Q 8.5" turn from red to green and CONFIRMAR COBRO
enable only once the sum matched, confirmed, got the "Venta completada"
snackbar. Cross-checked by `curl`: the resulting `CuentaPorCobrar` is
`COBRADA` with `saldoPendiente: 0.0000` and exactly two `cobros` — `EFECTIVO
5.0000` and `TARJETA 3.5000` — each fired as its own `registrarCobro` call
from `CheckoutNotifier`, as designed.

### `LoginScreen` keyboard-overflow fix

The card's `Column` (título, dos `TextField`, error opcional, botón) sat
directly inside `Center` with no scrolling ancestor — on a short viewport
(small phone in landscape, or a software keyboard eating enough height), the
column's fixed content could exceed available height with nowhere to go,
throwing a `RenderFlex overflowed` error instead of just showing less of the
background. Fixed with the standard idiom: `body` is now a `LayoutBuilder` →
`SingleChildScrollView` → `ConstrainedBox(minHeight: constraints.maxHeight)`
→ `Center` → the same card as before. `minHeight` (not a fixed height) keeps
the card vertically centered when everything fits, and lets it scroll
instead of overflow when it doesn't; the `SingleChildScrollView`'s `padding`
also tracks `MediaQuery.viewInsets.bottom` so the keyboard never covers the
active field. `flutter analyze` passes; normal login still works
(re-exercised as part of the Mixto Chrome click-test above, same screen).
**Not verified against a real software keyboard** — desktop Chrome has none,
and this session's `resize_window` calls didn't actually shrink the
rendered viewport (screenshots stayed the same pixel size regardless of the
requested width/height), so the overflow condition itself couldn't be
reproduced here to confirm before/after. The fix is the standard Flutter
pattern for this exact failure mode; treat it as correct-by-construction
until it's exercised on a real device/emulator with an IME.

### `LoginScreen` generic error message — fixed

`_onSubmit` used to show `'Usuario o contraseña incorrectos.'` for *any*
login failure, network included — the exact gap that made the
`CORS_ALLOWED_ORIGINS` incident earlier this session look like a credentials
problem instead of a network/CORS one. `ApiException` already carries
`isNetworkError` (set in `ApiException.fromDioException` for
`connectionError`/`connectionTimeout`/`receiveTimeout`), so `LoginScreen` now
reads `state.error` and branches: `error is ApiException && error.isNetworkError`
shows `'No se pudo conectar con el servidor. Verifica tu conexión.'`;
anything else (401, wrong password, etc.) still shows the generic
credentials message — deliberately generic there, since distinguishing
"user doesn't exist" from "wrong password" would leak which usernames are
valid.

Verified in Chrome against the real backend, all three paths: stopped the
backend and submitted → network message; backend back up, wrong password →
"Usuario o contraseña incorrectos."; correct password → logged in
normally (POS screen loaded). `flutter analyze` passes on `lib/features/auth`.

### Clientes y movimientos de caja offline — built this phase

Extends the offline-write pattern ventas already had (`CheckoutNotifier`) to
two more actions, following the exact same shape: a plain `NuevoXPendiente`
→ an Isar `@collection` (`XPendienteIsar`) behind `LocalStore`
(`encolarXPendiente`/`listarXPendientes`/`marcarXPendienteConError`/
`eliminarXPendiente`/`contarXPendientes`), drained by `SyncEngineNotifier` on
reconnect. `WebLocalStore` still hardcodes `disponible = false`, so on web
(and everywhere until this ships to Android/iOS) these two new offline paths
can only be exercised as their "no local storage on this device" error —
same limitation as the pre-existing ventas offline path.

- **Movimientos de caja** (`CajaActionsNotifier.registrarMovimiento`):
  offline, queues a `NuevoMovimientoCajaPendiente` instead of calling
  `CajaApi.registrarMovimiento`. Deliberately narrower than the whole `caja`
  feature — `abrir`/`cerrar` turno stay online-only, since those are
  session-boundary operations validated against server state (is there
  already an open caja? what's the real computed saldo to close against?)
  that don't have a safe conflict-free offline story. One real UX gap: a
  queued movimiento isn't reflected in `cajaAbiertaProvider`'s
  saldo/movimientos list until it syncs (that view is purely server-sourced,
  no local overlay) — same "invisible until synced" tradeoff the ventas
  queue already had.
- **Clientes nuevos** (`ClienteSelectorSheet._guardarClienteNuevo`): offline,
  queues a `NuevoClientePendiente` instead of calling `ClientesApi.crear`,
  shows a snackbar, and pops the sheet with **no** `Cliente` (`pop()`
  instead of `pop(cliente)`). ~~This is the one real design constraint: a
  cliente created offline has no server id yet... can't be used for a
  credit sale in the same offline session.~~ **Closed — see "Dependencias
  de cola offline" below.**

`SyncEngineNotifier._drenarCola()` drains three queues (clientes → ventas →
movimientos de caja) — **the order now matters** (see below), it's not just
FIFO-per-queue anymore. Each queue's own network-vs-business-failure
handling is unchanged from the ventas pattern (network failure stops that
queue's drain and retries next reconnect; business failure — e.g.
`ClienteDuplicadoException` — marks that item with `mensajeError` for manual
review and moves on). The connectivity badge's pending-count provider was
renamed `ventasPendientesProvider` → `pendientesSincronizarProvider` and now
sums all three queues, with the tooltip wording generalized from "N
venta(s) pendiente(s)" to "N elemento(s) pendiente(s)".

Verified: `dart run build_runner build` generated the two new Isar
collections cleanly; `flutter analyze` passes across
`lib/core`/`lib/features/caja`/`lib/features/clientes`/`lib/features/ventas`/
`lib/shared`. The **online** paths for both actions were regression-tested
in Chrome against the real backend (both are one-line-guarded wrappers
around the pre-existing online code, so this mainly confirms the new
`if (!hayRed)` branch doesn't break the `hayRed == true` case): opened Caja,
"Abrir turno" succeeded (shares `CajaActionsNotifier._ejecutar` with the
now-modified `registrarMovimiento`); created a brand-new cliente from
`CobroSheet`'s crédito flow and confirmed it was immediately usable for a
Q8.50 credit sale ("Venta completada") — cross-checked by `curl` that the
cliente was actually persisted server-side. **The offline branches
themselves are unverified** — this Chrome environment hit repeated,
unrelated automation flakiness (a stuck `_MovimientoDialog` whose text
fields wouldn't accept input via the automation tool, then a genuinely
blank/frozen renderer requiring two fresh tabs to recover) that ate the
session's error budget before a real offline-mode click-test could happen;
combined with `LocalStore.disponible` being hardcoded `false` on web
regardless, offline queuing for these two actions needs verification on a
real Android device/emulator, same as the rest of the offline story.

### Dependencias de cola offline — built this phase (Fase 2 parte C, PLAN_MEJORAS.md)

Closes the gap above: a cliente created offline **can now be used
immediately** for a venta a crédito in the same offline session, before
either one has synced. Previously `ClienteSelectorSheet` popped with no
`Cliente` at all when offline — the vendedor had no way to attach a
brand-new client to a credit sale until reconnecting and trying again.

**`ClienteSeleccionado`** (`clientes/data/cliente.dart`) replaces `Cliente`
as what `ClienteSelectorSheet` returns — either `.sincronizado(cliente)` (a
real server id) or `.pendienteLocal(pendienteLocalId: ..., nombre: ...,
limiteCredito: ...)` (the local Isar id of a `ClientePendienteIsar` just
queued, no server id yet). `CobroSheet._clienteCredito` is now typed
`ClienteSeleccionado?`, shows a "se creará al reconectar" note under the
límite de crédito line when `esPendienteLocal`, and passes
`clientePendienteLocalId` through to `CheckoutNotifier.confirmar`.

**Forcing the offline path**: `CheckoutNotifier.confirmar` ANDs
`clientePendienteLocalId == null` into its `hayRed` check — a venta
referencing a not-yet-synced local cliente always queues (`_confirmarOffline`),
even if the backend is reachable at that exact moment, since the referenced
client has no real id to send yet either way. `NuevaVentaPendiente`/
`VentaPendienteIsar.clienteId` is now nullable — exactly one of `clienteId`
(real id) or `clientePendienteLocalId` (local id) is set per queued venta.

**Resolving the reference at sync time**: `ClientePendienteIsar` gained
`clienteServidorId` (nullable) — a synced cliente is no longer deleted after
`_sincronizarCliente` succeeds (unlike ventas/movimientos), it's kept with
`clienteServidorId` set so a venta that still references it by
`clientePendienteLocalId` can resolve the real id even across an app
restart mid-drain. `listarClientesPendientes`/`contarClientesPendientes`
exclude these already-synced rows (`clienteServidorIdIsNull()`), so they
don't get resynced or double-counted as pending — they're pure historical
mapping at that point, cleaned up whenever `limpiarTodo()` next runs (only
once every real pendiente, including any venta still waiting on them, is
gone — see `LocalStore.limpiarTodo` doc).

`SyncEngineNotifier._sincronizarVenta` resolves the dependency via a new
`_resolverClientePendiente`, one of three outcomes
(`_ResolucionClientePendiente` sealed hierarchy): the pending cliente's own
sync already failed for good → mark the venta itself with a
`mensajeError` explaining which cliente failed and why (surfaces in
`PendientesErrorScreen`, doesn't retry forever silently); it hasn't
synced yet (clientes always drain before ventas in the same
`_drenarCola()` pass, so this only happens if *that* drain itself stopped
early on a network failure) → treat exactly like this venta's own network
failure, return `false` (stop the ventas queue here, retry next
reconnect, order preserved); resolved → use the real
`clienteServidorId` for `VentaApi.crear`.

Verified: `dart run build_runner build` regenerated both Isar schemas
cleanly (`ClientePendienteIsar.clienteServidorId`,
`VentaPendienteIsar.clientePendienteLocalId` + nullable `clienteId`).
`flutter analyze`/`flutter test` clean across the whole project. **Not
click-tested** — this requires driving the actual offline branch (Isar
`disponible == true`), which needs a real Android device/emulator; `WebLocalStore`
still hardcodes `disponible = false`, so this can't be exercised in Chrome.
Same open item as the rest of this file's offline story: verify on a real
device before considering this closed end-to-end.

### Versión de esquema local Isar — built this phase (Fase 2 parte C, PLAN_MEJORAS.md)

Before this phase there was no versioning at all for the local Isar
database — a schema change that wasn't purely additive (Isar migrates a new
nullable field or a whole new `@collection` on its own; renaming/removing/
retyping an existing field is NOT something Isar reconciles for you) had no
defined behavior on a device upgrading from an older build. The real risk
isn't the catálogo mirror (a network-first cache, safe to lose) — it's the
three pending queues (ventas/clientes/movimientos), which hold real
unsynced business data that must never be silently dropped.

`core/db/local_schema_version.dart` defines `esquemaLocalVersionActual` (an
`int`, bump it whenever a change to any `@collection` isn't purely
additive) and documents the policy in full. `core/db/metadato_local_isar.dart`
is a new one-row `@collection` (`MetadatoLocalIsar`, fixed `id = 0`)
recording which version wrote the current on-disk database.
`crearLocalStore()` runs `_aplicarMigracionSiHaceFalta` right after
`Isar.open` and before returning the store to the rest of the app:

- No metadato row at all → first run of this versioning mechanism on this
  device (either a genuinely fresh install, or a device upgrading from a
  pre-versioning build). Never wipes anything here — just stamps the
  current version and starts tracking from now on, since there's no prior
  version to compare against and doing so would be indistinguishable from
  "just trust whatever Isar already has" for existing data.
- Stored version matches current → no-op, the common case on every normal
  launch.
- Stored version differs → only safe to reset if **nothing is truly
  pending** (`_hayAlgoPendiente`: any ventas/movimientos, or any cliente
  still unsynced — `clienteServidorIdIsNull()`, ver "Dependencias de cola
  offline" arriba). If so, clears catálogo + all three queues and restamps
  the version — a clean slate with zero real data at risk. If something
  IS pending, nothing gets deleted — the version is still restamped (so
  this doesn't re-trigger every launch), but no automatic reconciliation
  happens. This is intentionally the honest boundary of what a *generic*
  mechanism can safely do: an actual non-additive change to, say,
  `VentaPendienteIsar` needs its own explicit migration step written into
  `_aplicarMigracionSiHaceFalta` for that specific version bump before it
  ships — there's no way to auto-derive "how do I convert old field X into
  new shape Y" without knowing what X and Y actually are.

**Convention for future schema changes**: prefer additive-only changes
(new nullable field, new collection) whenever possible — they need no
version bump and no migration code at all, Isar just handles them. Only
bump `esquemaLocalVersionActual` for a genuinely breaking change, and when
you do, decide explicitly whether `_aplicarMigracionSiHaceFalta` needs a
real migration step for that version or whether "wipe if nothing pending,
otherwise leave alone" is an acceptable answer for that specific change.

Verified: `dart run build_runner build` generated the new collection
cleanly; `flutter analyze`/`flutter test` clean. **Not tested against a
real upgrade scenario** — this project has no existing Isar-level test
infrastructure at all (same gap noted elsewhere in this file for
`SyncEngineNotifier`), and simulating "device has an old on-disk Isar file,
app now ships a newer schema" realistically needs a real device/emulator
with a prior APK installed, not just unit tests.

### Cifrado de datos locales — evaluated this phase (Fase 2 parte C, PLAN_MEJORAS.md)

`isar_community` (the fork this app uses) has **no built-in encryption at
all** — confirmed by grepping its source for `encrypt` (nothing; `Isar.open`
takes no encryption-key parameter). Real field/database encryption would
mean hand-rolling AES around specific fields with a new crypto dependency
and key management (`flutter_secure_storage`, already a dependency, would
hold the key) — not something to add silently without the user picking
that tradeoff, same reasoning as the earlier remote-logging evaluation
above.

Inventoried what PII actually lives in Isar before deciding: only
`ClientePendienteIsar.nombre/telefono/nit` (a cliente created offline).
Every other collection (`VentaPendienteIsar`, `MovimientoCajaPendienteIsar`,
`ProductoCatalogoIsar`, `MetadatoLocalIsar`) stores ids/amounts/catalog
data, no direct PII.

**Decision (user, this phase): minimize instead of encrypt.** OS-level disk
encryption (on by default on modern Android) already covers the realistic
threat here — a lost/stolen tablet, powered off or locked. Field-level
encryption would additionally protect against someone with access to an
unlocked/rooted device reading the raw Isar file (this session did exactly
that, `run-as cat` on `default.isar`, per "Verified in a later session" note
above) — judged not worth a new crypto dependency for 3 short text fields
on a POS where physical device access is already controlled.

What *did* ship: `LocalStore.limpiarClientesPendientesSincronizadosSinReferencia()`,
called at the end of every `_drenarCola()` pass. A cliente row is kept
after syncing only as long as some pending venta still needs its
`clienteServidorId` (see "Dependencias de cola offline" above) — once
nothing references it anymore, it's deleted instead of lingering
indefinitely. Shrinks the exposure window for whatever PII does get
persisted, without adding any new dependency.

Verified: `flutter analyze`/`flutter test` clean. **Not click-tested** —
same reasoning as the rest of this phase (needs `LocalStore.disponible ==
true`, a real device/emulator).

### Bloqueo de logout con pendientes — built this phase (Fase 2 parte C, PLAN_MEJORAS.md)

`cerrarSesionConConfirmacion` (`logout_confirm.dart`, the only path that
ever calls `AuthNotifier.logout()` — nothing else in the app does) used to
warn and offer a "Cerrar sesión de todos modos" bypass when
`pendientesSincronizarProvider` was non-zero. **Decision (user, this
phase): hard block, no bypass.** With anything pending, the dialog now only
has "Ver pendientes" (jumps to `PendientesErrorScreen` via
`context.push('/pendientes-error')`) and "Entendido" — neither one calls
`logout()`. The only ways out are: reconnect and let the queue drain on its
own, or explicitly discard a genuinely stuck item from
`PendientesErrorScreen` (which already asks its own "no se puede deshacer"
confirmation before deleting anything).

Accepted tradeoff, made explicitly by the user rather than assumed: a
vendedor who needs to hand off a shared tablet to someone else with no
connectivity available is now stuck until they reconnect — chosen
deliberately over the risk of an accidental/casual bypass silently losing
real unsynced ventas.

**Bloqueo de desinstalación — evaluated, marked not implementable via app
code.** A normal Android/iOS app cannot intercept or prevent its own
uninstallation — that requires Device Admin / Android Enterprise (MDM), a
device-management deployment decision, not a code change in this repo.
User decision this phase: document this limit and close the plan item as
"no aplica vía app" rather than investigate MDM adoption (out of scope).

Verified: `flutter analyze`/`flutter test` clean. Only two call sites for
logout in the whole app (`PosScreen`, `TiendaPickerScreen`), both already
routed through `cerrarSesionConConfirmacion` — confirmed via grep, no other
path bypasses this dialog.

### Cobros sueltos (`cuentas_por_cobrar` feature) — built this phase

New feature, `lib/features/cuentas_por_cobrar/`. Before this phase there was
**no way at all** — online or offline — to pay down an existing
`CuentaPorCobrar` outside the one automatic cobro `CheckoutNotifier` fires
right after completing a venta; a cliente who came back later to pay off
(or partially pay off) an old balance had nowhere to do that in this app.
The backend already supported it fully (`GET
/cuentas-por-cobrar/tiendas/{id}` to list, `POST .../{id}/cobros` to pay) —
this phase is 100% frontend, no backend changes.

`CuentasPorCobrarScreen` (routed at `/cuentas-por-cobrar`, gated by
`CUENTAS_POR_COBRAR_VER` — a new AppBar icon on POS next to Caja, same
`sesion.can(...)`-guarded pattern) lists every `PENDIENTE` cuenta for the
tienda, soonest-`fechaVencimiento` first, cliente name resolved against
`clientesProvider` (falls back to `Cliente #id` if the list hasn't loaded
yet), with a warning icon on anything past due. Tapping one opens
`RegistrarAbonoSheet` (gated separately by `CUENTAS_POR_COBRAR_COBRAR` —
tapping is disabled, not just the confirm button, if the sesión lacks it):
pick a channel — **only Efectivo/Tarjeta/Transferencia**, deliberately no
Crédito (the cuenta already *is* the credit) and no Mixto (nothing stops
registering two separate abonos, one per channel, if a payment genuinely
arrived split) — enter a monto capped at the saldo pendiente, confirm. On
success it invalidates `cuentasPorCobrarPendientesProvider`, so a
fully-paid cuenta disappears from the list immediately (the provider
re-filters to `PENDIENTE` only).

`CuentaPorCobrar` (in `venta_api.dart`, shared with the checkout flow) grew
`estado` and `fechaVencimiento` — both already existed in
`CuentaPorCobrarResponse` and were just never read client-side before there
was a screen that needed them.

**Deliberately out of scope**: no offline queue (see above). `anular` and
search/filter — see below — were both added in later phases.

Verified end-to-end in Chrome against the real backend, as `admin`: the
screen listed the 3 real pending cuentas with correct cliente names, saldos,
and vencimientos; registered a full Q3.00 abono (Efectivo) against "Maria
Lopez" and watched it disappear from the list on success. Cross-checked by
`curl`: the cuenta's `cobros` array now has the new `EFECTIVO 3.0000`
entry, `estado: COBRADA`, `saldoPendiente: 0.0000`. `flutter analyze` passes
across the whole project.

### Idempotencia de ventas offline (`correlationId`) — built this phase

The client has generated a `correlationId` for every queued offline venta
since the sync queue was first built (`'${DateTime.now().microsecondsSinceEpoch}-$tiendaId'`
— see `CheckoutNotifier`/`VentaPendienteLocal`), but the backend never
received or looked at it — it was pure client-side bookkeeping. The real gap:
if a `POST /ventas` succeeds server-side but the response never reaches the
client (connection drops right after, or `SyncEngine`'s retry loop re-fires
for any reason), the client sees a network failure and keeps the item
queued — the *next* drain attempt would create a **second, duplicate**
venta for the same sale, since nothing tied the retry back to the original
attempt.

Backend: `CrearVentaRequest` gained an optional `correlationId` (no
validation annotation — `null` is the normal case for every online-direct
venta, which never had one and still doesn't need one). `Venta`/`VentaEntity`
persist it in a new nullable, **unique** `correlation_id` column
(`ventas/004-correlation-id.xml`; nullable/no-backfill, same reasoning as
`metodo_pago` — Postgres's `UNIQUE` constraint allows unlimited `NULL`s, so
existing ventas and every online-direct venta going forward coexist fine
without one). `VentaServiceImpl.crear()` is now genuinely idempotent when a
`correlationId` is given: it looks up `VentaRepository.findByCorrelationId`
first and, if found, returns that existing venta's resumen **without
creating anything** — a retried sync attempt is a no-op that returns the
same venta, not a duplicate. `Venta.nueva()` keeps its original 5-arg
overload (used by ~20 existing call sites/tests unaffected by this) plus a
new 6-arg one that accepts it.

`market-flutter`: `VentaApi.crear()` gained an optional `correlationId` param
that's sent in the request body when present; `SyncEngine._sincronizarVenta`
is the only caller that actually passes one (`venta.correlationId`, already
generated when the item was queued) — `CheckoutNotifier`'s direct online
path still doesn't generate or send one, since that path was never the one
with a duplicate-retry risk (it isn't a persisted, retried queue).

~~**Known simplification**: idempotency trusts the `correlationId` alone — it
does not re-validate that a retried request's `clienteId`/`lineas`/
`metodoPago` still match the original attempt.~~ **Closed 2026-08-25** — see
the note right after this block. Kept the original write-up below for
context on what changed and why.

Original gap (now closed): idempotency trusted the `correlationId` alone —
it did not re-validate that a retried request's `clienteId`/`lineas`/
`metodoPago` still match the original attempt. This was standard
idempotency-key behavior and matched the actual risk being defended against
(the same client re-sending the same queued item), not an attacker or a
genuinely different sale reusing an id. A rare theoretical race (two
concurrent inserts with the same brand-new `correlationId`, beating each
other past the `findByCorrelationId` check) would surface as
`ReferenciaInvalidaException` — "El cliente, la tienda o el producto
indicado no existe" — from `VentaRepositoryAdapter.save()`'s generic
`DataIntegrityViolationException` handler, which is a misleading message for
that specific case. Not handled specially: `SyncEngine` retries strictly
sequentially (one device, one item at a time, never concurrent with itself),
and two different devices colliding on the same microsecond-timestamp-based
id is astronomically unlikely — treated as an acceptable, documented gap
rather than added complexity.

**Update 2026-08-25 — closed on the backend, this app needed no changes.**
The backend's idempotency key is now composite (`tienda_id`, `vendedor_id`,
`correlation_id` — was globally unique on `correlation_id` alone, meaning
two different tiendas could never reuse the same value) and it now compares
a reused key's `clienteId`/`lineas`/`metodoPago` against the original
attempt: identical content still returns the existing venta, different
content now gets a real `409 CorrelationIdReutilizadoException` instead of
silently returning someone else's venta. The concurrent-insert race is
handled too — verified with 8 genuinely concurrent `POST /ventas` calls
against the real backend (Docker), all returning the same venta id, exactly
one row created. None of this required a client change here: this app
already sends one `correlationId` per queued item and already treats
whatever comes back as authoritative — see
`market-backend/docs/plan-mejoras.md`, Fase 6, for the backend-side detail.

Verified against the real backend via `curl`: two identical `POST /ventas`
calls with the same `correlationId` both returned `"id":11` (no duplicate
created); a third call with no `correlationId` created a genuinely new
venta (`"id":12`), confirming online-direct behavior is unchanged. Both test
ventas anulados afterward for cleanup. `mvn test` passes — new tests cover:
no `correlationId` never queries `findByCorrelationId`, an existing
`correlationId` returns the existing venta without calling `save()`, a new
`correlationId` is persisted on the saved venta, and the controller passes
`correlationId` through from the request body. `flutter analyze` passes on
`lib/features/ventas` and `lib/core/sync`.

### Conflict resolution — verified against the real backend

`SyncEngine`'s mark-and-continue design (network failure → stop that
queue's drain and retry later; business failure → mark `mensajeError` and
move to the next item) depends entirely on two backend contracts actually
holding. Both were reproduced via `curl` this phase — no Flutter code
changed, this was pure verification of assumptions the offline design
already relies on:

**Stock conflict at sync-time** — a venta queued offline when stock was
plentiful, whose line no longer fits by the time it syncs (another sale
consumed the stock in between): completed a real venta for 35 of 42
available units (dropping stock to 7), then attempted to complete a second
venta requesting 40 — got exactly the expected `409 STOCK_INSUFICIENTE`,
the second venta stayed `BORRADOR` (not partially completed), and stock
stayed at `7.000` (not further decremented by the rejected attempt). This
is the exact shape `SyncEngine._sincronizarVenta` needs: a non-network
`ApiException` it catches, marks the pendiente with `mensajeError`, and
moves on — never silently adjusts the line or drops the conflict.

**Cobro race between two devices** — a cuenta por cobrar fully paid by one
"device" while a second device's own (now-stale) cobro for the same amount
is still in its queue: registered a full Q20.00 cobro against a fresh
`CuentaPorCobrar` (→ `COBRADA`, `saldoPendiente: 0.0000`), then attempted a
second identical Q20.00 cobro against the same cuenta — got `400
ESTADO_CUENTA_POR_COBRAR_INVALIDO` ("La operación no es válida para una
cuenta por cobrar en estado COBRADA"), and the cuenta's `cobros` array still
had exactly one entry — no double-crediting, no silent overwrite, no
crash.

Cleaned up afterward: the `BORRADOR` venta from the stock scenario was
anulada; the two `COMPLETADA` ventas (one real inventory movement, one real
cobro) have no undo path in this app — same accepted limitation noted
elsewhere in this file — and are left as real historical records.

**Still not verified**: the client-side consumption of these contracts —
does `SyncEngineNotifier` actually call `marcarVentaPendienteConError` and
continue past a real 409 without crashing the isolate, does
`pendientesSincronizarProvider`'s badge count correctly reflect a marked
item, does an encargado actually see and can act on the flagged item —
needs a real device/emulator queuing and draining an actual offline venta
into a live conflict, not just the backend contract in isolation.

### Anular en cuentas por cobrar — built this phase

`CuentasPorCobrarScreen` gains the action deliberately deferred when the
feature was first built: `CuentaPorCobrarApi.anular()` (thin wrapper around
the existing `POST .../anular`) plus a trailing icon-button per cuenta,
gated by `CUENTAS_POR_COBRAR_ANULAR` (independent from the
`CUENTAS_POR_COBRAR_COBRAR` gate on tapping the row for an abono — a
sesión can have one without the other). Tapping it asks for confirmation
(`AlertDialog`, "Esta acción no se puede deshacer") before calling the
backend — this app's first destructive-confirmation dialog outside a
disabled-button pattern. The screen doesn't pre-filter which cuentas are
actually anulable (the backend only allows it for `PENDIENTE` with zero
cobros — `CuentaConCobrosException` otherwise): every cuenta gets the
button, and a rejection surfaces as a plain `SnackBar` rather than hiding
the button conditionally, since the Dart `CuentaPorCobrar` model doesn't
carry the `cobros` list (only aggregated `saldoPendiente`) and fetching it
per-row just to decide button visibility isn't worth the extra requests.

Verified in Chrome against the real backend, both outcomes: tapped anular
on a cuenta with an existing cobro (Q2.00 abonado via `curl` beforehand,
saldo Q6.50) → confirmed → `SnackBar` "No se pudo anular — puede que ya
tenga abonos registrados.", item stayed in the list, state unchanged.
Tapped anular on a cuenta with zero cobros (Q8.50) → confirmed → it
disappeared from the list immediately; cross-checked by `curl`, `estado:
ANULADA` on the backend. `flutter analyze` passes on
`lib/features/cuentas_por_cobrar` and `lib/features/ventas`.

### Búsqueda/filtro en cobros sueltos — built this phase

`CuentasPorCobrarScreen` converted from `ConsumerWidget` to
`ConsumerStatefulWidget` to hold the search field's local state (same shape
as `ClienteSelectorSheet`'s search — reuses `Cliente.coincideBusqueda`
rather than inventing a second matching rule). Filters client-side against
the already-fetched pending-cuentas list — no new backend call, no new
endpoint, since `cuentasPorCobrarPendientesProvider` and `clientesProvider`
were both already loaded for the list/name-resolution this screen already
did. A cuenta whose cliente hasn't resolved yet (`clientesProvider` still
loading) is shown rather than hidden by an active search — a real cuenta
disappearing because of a momentary data gap would be worse than a filter
that's briefly too permissive.

Verified in Chrome: typed "Consumidor" with `admin` logged in → list
narrowed from 2 cuentas to exactly "Consumidor Final"; typed a nonsense
query → "Sin resultados." `flutter analyze` passes on
`lib/features/cuentas_por_cobrar`.

### `PosScreen` overflow fixes (found on real Android device) — code done, visual check pending

While finally getting an Android emulator working this phase (see below),
running the real app surfaced two genuine `RenderFlex` overflows Chrome had
never shown, both on `pos_screen.dart`:

- **AppBar actions, portrait phone width (~411dp logical)**: title + the
  connectivity badge + up to 5 icon buttons (bolt, dashboard, caja, cuentas
  por cobrar, logout) don't fit in one row — `RIGHT OVERFLOWED BY 89
  PIXELS`. Fixed: below a 700px width breakpoint, the navigation icons
  (dashboard/caja/cuentas-por-cobrar/logout — everything except the bolt
  toggle and the always-visible connectivity badge, which stay inline since
  they're used every sale and must never be hidden per CLAUDE.md's
  "Connectivity indicator: always visible") collapse into a
  `PopupMenuButton` overflow menu instead of one `IconButton` each.
- **Product grid, landscape on the same phone-width device**: the fixed
  `crossAxisCount: 4` (or 6 in Modo Venta Rápida) made each cell too narrow
  once the two fixed-width side panels (categorías 160px + carrito 340px)
  ate most of a ~731dp-wide landscape screen — `BOTTOM OVERFLOWED BY 59
  PIXELS`, price text wrapping one digit per line. Fixed: `_GridProductos`
  now wraps the grid in a `LayoutBuilder` and computes
  `crossAxisCount = (constraints.maxWidth / 150).floor().clamp(2, max)` —
  columns shrink in count instead of shrinking past a legible minimum
  width, so the fixed `childAspectRatio` never starves the card's content.

Both fixes compile clean (`flutter analyze` on `pos_screen.dart`, no
issues) but **the actual on-device screenshot confirming the overflow
banners are gone is still pending** — the emulator died three times in a
row right at that step (see "Android emulator — finally working, then
died" below) with Windows showing real memory pressure (~1.2GB in Memory
Compression, no crash logged by the emulator itself), which reads as host
resource exhaustion rather than an app or AVD problem. Treat these as
correct-by-construction (the reasoning holds, `flutter analyze` is clean)
until re-verified visually on a device.

### Android emulator — finally working, then died

`flutter_emulator` (Pixel Tablet AVD, distinct from the `Pixel_7_Pro_API_35`
AVD that had the unfixable virtio-wifi bug earlier this session) booted
clean with real networking — `adb shell ping 10.0.2.2` (the host, from the
guest) succeeded, and `adb shell curl http://10.0.2.2:8080/api/v1/auth/login`
got a real JWT back. Ran the actual app on it with
`flutter run -d emulator-5554 --dart-define=API_BASE_URL=http://10.0.2.2:8080`
(the default `http://localhost:8080` only works for web/desktop — Android
needs the emulator's host-loopback alias). Confirmed on real hardware for
the first time this session: full login with the real Gboard software
keyboard (the `LoginScreen` keyboard-overflow fix scrolled correctly to
keep both fields visible above the keyboard), and POS loading real
catalog/session data.

Lost the device to repeated silent crashes right after (no crash log from
the emulator itself; Windows showed ~1.2GB in Memory Compression, i.e.
genuinely low free RAM) — swapping `-gpu swiftshader_indirect` (software
rendering) fixed one class of failure (`GraphicBufferAllocator` /
`SurfaceFlinger` buffer-allocation errors on hardware GL, which also broke
`adb screencap`) but the emulator still died two more times afterward.
Reads as host resource exhaustion from a long-running session with many
other processes (two `mvn spring-boot:run`, Chrome, this emulator, IDEs),
not a fixable app/AVD config issue. **Per explicit instruction, left
pending** — next attempt should free host memory first (close unused apps)
before relaunching. The full device verification this was meant to enable
— CookieJar refresh-after-restart, the offline queue (ventas/clientes/caja)
actually queuing and draining against a live conflict — is still
unverified, same as it's been all session.

### Backend pagination rollout — client adapted, not verified on-device

The backend added real (`Pageable`/`Page<T>`) pagination to its listing
endpoints; every one of them now returns `{contenido, pagina, tamano,
totalElementos, totalPaginas}` instead of a plain JSON array. Three of them
are consumed here: `GET /productos`, `GET /productos/tiendas/{id}`, and
`GET /inventario/tiendas/{id}` (all three via `ProductosApi.listarCatalogo`),
plus `GET /cuentas-por-cobrar/tiendas/{id}`
(`CuentaPorCobrarApi.listarPorTienda`). `GET /ventas/tiendas/{id}` (unused
here), `GET /traslados` (unused here), and `GET /caja/tiendas/{id}` (only
`obtenerAbierta`, a single object, is used here) were unaffected.

New `core/network/paginacion.dart`: `contenidoDePagina(data)` unwraps the
envelope's `contenido` array, and `tamanoPaginaCompleta` (5000, matching the
backend's own cap) is passed as `?size=` on all four calls above — this POS
needs the *entire* catalog/inventory/cuentas list in one shot (for the
offline cache and for `buscarPorVenta`'s O(n) client-side search), not real
pagination UI, so the fix is "ask for everything in one page" rather than
looping through pages. If any of these lists ever genuinely exceeds 5000
rows, this stops being "the whole list" silently — worth revisiting if that
becomes realistic.

Verified: the exact envelope shape (`{"contenido":[...],"pagina":0,...}`)
against the real backend via `curl` (empty-DB smoke test, all 4 endpoints),
and `flutter analyze` clean on `lib/features/productos`,
`lib/features/ventas`, `lib/core/network`. **Not verified**: an actual POS
session (Chrome or device) driving a catalog load / cobros-sueltos screen
against a database with real data end-to-end — this phase only confirmed
the contract and the parser change compile and match, not a full click-test.

### Flujo contable correcto de ventas — checkout simplificado, bug de fondo corregido

The backend used to unconditionally create a full-total `CuentaPorCobrar` for
*every* `completar()` call regardless of `metodoPago` — including EFECTIVO.
This app's checkout worked around that by immediately firing one or more
`registrarCobro` calls right after `completar()` to pay it back down (one for
simple sales, one per channel for Mixto) — real money only ever hit `Caja`
indirectly, through that follow-up cobro. If those follow-up calls never
fired (connection dropped right after `completar()`), a real, phantom
`CuentaPorCobrar` was left behind for a plain cash sale.

The backend now resolves this correctly inside `completar()` itself:
EFECTIVO/TARJETA/TRANSFERENCIA register their ingreso in Caja directly (no
`CuentaPorCobrar` at all), CREDITO is unchanged (no ingreso, cuenta by the
full total), and MIXTO now takes the payment desglose **in the same
`completar()` call** — it registers each channel's ingreso in Caja directly
and creates a `CuentaPorCobrar` only for the leftover saldo (often zero, if
the desglose covers the total). See `market-backend/docs/plan-mejoras.md`,
Fase 3, for the backend side.

This app's checkout is simpler as a result — `CheckoutNotifier._confirmarOnline`
no longer does the post-`completar()` `buscarPorVenta` + `registrarCobro`
dance at all: `VentaApi.completar()` gained an optional `pagosInmediatos`
param (only sent for Mixto — `CheckoutNotifier` passes `desglose` straight
through), and `SyncEngine._sincronizarVenta` dropped the same dead dance for
its own `completar()` call (the offline queue never carries Mixto, so it
never needed a desglose to begin with — EFECTIVO/TARJETA/TRANSFERENCIA/
CREDITO are all handled automatically by the backend now). `CobroSheet`'s
UI is unchanged — it still builds the same `desglose` map for Mixto, just
handed to `confirmar()` the same way as before.

**Removed as dead code**: `CheckoutNotifier.confirmar`'s `montoRecibido`
param — its only caller (`CobroSheet._confirmar`) always passed `null` (a
partial-abono-at-credit-time flow that was never wired to any UI control),
so it never did anything. `VentaPendienteLocal.montoACobrar` stays in the
Isar schema (removing a field means a schema/build_runner change, not worth
it for an already-unused-in-logic field) but is no longer read by
`SyncEngine` — harmless persisted-but-ignored data, not a bug.

Verified: `flutter analyze` and `flutter test` both clean across the whole
project after the change. **Not verified**: an actual checkout session
(Chrome or device) driving a real Mixto/Efectivo/Crédito sale against the
updated backend end-to-end — this phase confirmed the backend contract via
`curl` (see plan-mejoras.md) and that this client's code compiles and matches
it, not a full click-test of the POS itself.

### Fase 9 (market-backend/docs/plan-mejoras.md) — real-device blockers, closed at the code level

Explicit call from the user this phase: real device/emulator verification
stays blocked by the emulator instability documented above ("Android
emulator — finally working, then died"), so this phase did everything
implementable and buildable without one, and documents the rest as
genuinely unverified rather than claiming it.

**Release APK had no INTERNET permission at all — found and fixed.** Not in
the plan's original task list, found while implementing the cleartext
exception below: `android/app/src/{debug,profile}/AndroidManifest.xml` each
declare `android.permission.INTERNET`, but `android/app/src/main/AndroidManifest.xml`
(the one that actually merges into a **release** build) never did. Every
debug/profile run this session had network access purely because of the
debug-only grant; a real `flutter build apk --release` — the exact artifact
this task asks to document/script — would have installed on a device and
been completely unable to reach the backend, no error dialog, nothing
obviously wrong until someone tried to log in. Added the permission to
`main/AndroidManifest.xml`. Verified by actually running
`flutter build apk --release --dart-define=API_BASE_URL=https://test-server.local`
(succeeded, `app-release.apk`, 69.2MB) and grepping the real merged
manifest at `build/app/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`
for `INTERNET` — present. Before this fix it would not have been (main
manifest is the only one that merges into release; neither `connectivity_plus`
nor any other plugin in this project declares `INTERNET` on its own).

**Cleartext exception** (the plan's original "immediate blocker" item):
`android/app/src/main/res/xml/network_security_config.xml` (new),
allowlisting cleartext (HTTP) only for two `<domain>` entries — a
`test-server.local` placeholder (must be edited to the real test server's
domain/IP before building) and `10.0.2.2` (the emulator's host-loopback
alias, already used successfully this session per "Android emulator" above).
Never `usesCleartextTraffic="true"` globally. Referenced from
`AndroidManifest.xml` via `android:networkSecurityConfig="@xml/network_security_config"`.
**Update 2026-08-27**: backend deploy now has real TLS (Caddy reverse proxy,
automatic cert, `https://inven365.com.gt` — see `market-backend/deploy/README.md`,
section "TLS"). The `test-server.local` cleartext entry is no longer needed
for the production domain; the `10.0.2.2` entry (emulator loopback) can stay
for local dev against a non-TLS backend. Revert/trim this file once the app
is confirmed pointing at the real HTTPS domain end-to-end. Verified the same
release build above: AAPT would have failed at
compile time if the XML were malformed or the resource reference broken —
it didn't, and the merged manifest carries the attribute (resource itself
gets renamed to an obfuscated path by the release resource shrinker, so it
isn't greppable by filename in the final APK — that's expected, not a sign
it's missing).

**Production build procedure — documented, not scripted** (no `scripts/`
convention exists in this repo, and a single documented command doesn't
need one):
```
flutter build apk --release --dart-define=API_BASE_URL=https://<real-test-server-domain-or-ip>
```
Before running it: edit `network_security_config.xml`'s placeholder domain
to match. `environment.dart` already reads `API_BASE_URL` via
`String.fromEnvironment` with no hardcoding — this was already true before
this phase, only the manifest/network-security side was missing.

**Sync retry bug — the plan's item was already stale, fixed the current
version of the same risk.** The plan describes the bug in terms of a
separate `registrarCobro` call after `completar()` failing — that call no
longer exists (see "Flujo contable correcto de ventas" above, closed
earlier this session): `completar()` now does everything atomically
server-side. The *equivalent* risk survives in the new shape: if
`completar()` succeeds on the server but the response never reaches the
client (connection drops right after), the pendiente stays queued; the next
drain re-runs `crear()` (idempotent-safe by `correlationId`, returns the
same venta) and then calls `completar()` again on a venta that's already
`COMPLETADA` — the backend correctly rejects that with `409
ESTADO_VENTA_INVALIDO` (`EstadoVentaInvalidoException`), which
`SyncEngine._sincronizarVenta` used to treat as a genuine, permanent
business failure (`marcarVentaPendienteConError`) even though the sale had
actually gone through fine.

Fixed: on `ESTADO_VENTA_INVALIDO` specifically, `_sincronizarVenta` now
calls the new `VentaApi.obtener()` (new endpoint wrapper,
`GET /ventas/tiendas/{tiendaId}/{id}`) to check the venta's real current
`estado`. `COMPLETADA` → treat as success, delete the pendiente. Anything
else (e.g. `ANULADA`, or the confirmation call itself fails — including on
a network error, deliberately not assumed as success) → same
`mensajeError` path as before, for a human to review. `flutter analyze`
clean; no unit test covers `SyncEngineNotifier` (none did before this phase
either — it's Isar/Riverpod-coupled and this project has no existing
pattern for testing it, same gap as the rest of the offline queue).

**Update 2026-09-04 — the same bug existed on the direct online path too,
found while writing Fase 2's "respuesta perdida" test.** This fix above only
ever landed in `SyncEngine._sincronizarVenta` (the offline-queue drain
path). `CheckoutNotifier._confirmarOnline` — the direct online checkout, the
common case, never queued — called `crear()` then `completar()` with no
such reconciliation at all: a lost response right after a real
`completar()` success, followed by the vendedor pressing confirm again
(same `correlationId`, same as any manual retry this app relies on), hit
the identical `409 ESTADO_VENTA_INVALIDO` and surfaced as "no se pudo
completar la venta" even though the sale was already done and paid. Fixed
by extracting the reconciliation into a shared top-level function,
`ventaYaQuedoCompletada` (`venta_api.dart`) — used by both
`SyncEngine._sincronizarVenta` (replacing its former private
`_yaQuedoCompletada`) and the new `try`/`catch` around `completar()` in
`_confirmarOnline`. Covered by two new unit tests in
`checkout_notifier_test.dart` (a fake `VentaApi` simulating server-side
state): a lost response right after `crear()` (retry doesn't create a
second venta, relying on the backend's existing `correlationId`
idempotency) and right after `completar()` (retry no longer shows an
error). `flutter analyze`/`flutter test` clean (75/75). Same
unverified-on-a-real-device gap as the rest of this file — this is a unit
test against a fake, not a real dropped connection on a tablet.

**Update 2026-09-04 — `SyncEngineNotifier` has unit test coverage for the
first time**, closing the gap this file used to note right above (`no
unit test covers SyncEngineNotifier`). New `test/core/sync/sync_engine_test.dart`
covers Fase 2's "reintento tras matar la app durante cada estado":
`VentaPendienteLocal` only ever persists the full original request, never
an intermediate "already created, not yet completed" marker, so surviving a
kill depends entirely on a freshly-built `SyncEngineNotifier` (a
`ProviderContainer` built from scratch stands in for a relaunched app, with
no memory of which call got out before the kill) reconstructing the right
outcome purely from what the backend actually has for that
`correlationId`. Three fake-`VentaApi` seedings, one test each: the backend
never saw the venta (drains normally), the kill landed right after `crear()`
(retry doesn't duplicate), and right after `completar()` (retry recognizes
`COMPLETADA` via `ventaYaQuedoCompletada` instead of marking it
`mensajeError`) — all three converge to exactly one `COMPLETADA` venta and
an empty queue. The reactive trigger itself (`ref.listen(backendAlcanzableProvider, ...)`
inside `build()`, no `fireImmediately`) was also exercised for the first
time this way and does fire on the loading→data transition as long as
something has already read `syncEngineProvider` before that first value
resolves — same pattern the test uses (`container.listen(syncEngineProvider, ...)`
before awaiting `backendAlcanzableProvider.future`). `flutter analyze`/
`flutter test` clean (78/78). Still only a `ProviderContainer` unit test
against fakes, not a real killed process on a device with a real Isar file
on disk.

**Pending-with-error visibility — the other plan item, built.** Before this
phase, `mensajeError`-marked ventas/clientes/movimientos-de-caja were
excluded from the sync retry (`local_store_io.dart`'s `mensajeErrorIsNull()`
filter, correct — never silently retry a business failure) but there was no
way to see or act on them: buried in Isar, permanently. Added to
`LocalStore` (interface + both implementations): `listarXPendientesConError()`
(the `mensajeErrorIsNotNull()` mirror of the existing retry-list query) and
`reintentarXPendiente(id)` (clears `mensajeError`, so the next drain treats
it like a fresh item). Discarding reuses the existing `eliminarXPendiente`
— no new method needed.

New `PendientesErrorScreen` (`features/sync/presentation/`), reachable by
tapping `ConnectivityBadge` (now an `InkWell`, was static) — new route
`/pendientes-error`. Lists all 3 pendiente types in one flat list
(`ItemPendienteConError`, `pendientesConErrorProvider`), each card showing
what it is, the actual error message from the backend, and two actions:
REINTENTAR (clears the error, triggers an immediate drain — no need to wait
for a reconnect event) and DESCARTAR (confirmation dialog first — "no se
puede deshacer" — then deletes it for good, mirroring the existing anular
confirmation pattern in `cuentas_por_cobrar_screen.dart`). The badge's pill
count turns red when any pendiente has an error, not just when the total is
non-zero, so "something needs a human" is visually distinct from "still
syncing, wait".

**Remote logging/crash reporting — evaluated, not added.** The plan asks to
"evaluate", not to integrate — deliberately treated as a decision, not a
default action: neither Sentry nor Firebase Crashlytics is in `pubspec.yaml`
today, and adding either means an external account, an API key committed to
build config, and a live third-party data destination for whatever gets
logged (potentially including customer names/NIT from `clientes`, or sale
amounts) — not something to wire up unilaterally without the user picking
the provider and providing the key. Recommendation if/when this becomes a
priority: `Sentry` has the more turnkey Flutter SDK (`sentry_flutter`,
auto-captures uncaught errors + Dio request breadcrumbs with minimal setup)
and would pair naturally with the `ApiException`/`isNetworkError` shape
already in `core/network` — start there over Crashlytics if a `curl`-account
decision needs to be made, but don't add either without that decision being
made explicitly by the user first.

**Verified this phase**: `flutter analyze` and `flutter test` clean across
the whole project; a real `flutter build apk --release` succeeds and the
resulting manifest has both the `INTERNET` permission and the
`networkSecurityConfig` reference.

**Verified in a later session, real Android emulator (Pixel_C_API_33)**:
the full offline queue + recovery flow, end to end, against a real backend
and Postgres. Cut network for real (`adb shell svc wifi disable` + `svc data
disable`, confirmed via a failed `ping 10.0.2.2`, not just a UI toggle),
completed a cash sale — got the optimistic "Venta completada." snackbar,
cart cleared — and confirmed it actually persisted locally by reading the
raw Isar file (`run-as cat` on `app_flutter/default.isar`, `grep`-ed for the
product/payment-method strings). Restored network for real (ping succeeded
again) and watched `ConnectivityBadge` correctly flip Sin conexión →
Conectado. The queued item did **not** auto-drain on that reconnection —
not a `SyncEngineNotifier` bug: an earlier attempt (interrupted by a
force-stop/relaunch done mid-investigation) had already failed and left the
item marked with `mensajeError`, which is exactly the documented "business
failure — mark and don't auto-retry" behavior, working as designed. Opened
`PendientesErrorScreen` (tapping the connectivity badge), saw the item
listed with its error message, tapped REINTENTAR — the sale synced
immediately, `correlationId` present, `estado: COMPLETADA` confirmed via
`psql`. First real click-test of `PendientesErrorScreen` and of REINTENTAR
against a live queued item — previously code-reviewed only. The
software-keyboard `LoginScreen` scroll fix was also confirmed in this same
session: the login card stayed correctly scrolled above the real Gboard
keyboard.

**Still not verified**: DESCARTAR on a real queued error (only REINTENTAR
was exercised), the 2 `PosScreen` overflow fixes' on-device screenshot, and
a real cleartext connection against a genuine (non-`10.0.2.2`) test server.
Next session with a working device/emulator should prioritize these over
any new feature work.

### Layout de teléfono en `PosScreen` + orientación libre — built this phase

The client explicitly needs to test on, and possibly sell from, a phone —
not just the 10"-12" tablets this app was originally scoped for. Two
separate problems, both fixed:

**Layout**: below `anchoAngosto` (700px), `PosScreen`'s body used to be a
fixed 3-column `Row` (categorías 160px + carrito 340px) — on a phone those
two columns alone eat ~500px, leaving no real room for the catálogo. New
`_PosBodyTelefono` swaps that for a single column at that breakpoint:
categorías become a horizontal chip row (`_CategoriasChips`), the catálogo
gets the full width, and the carrito collapses into a persistent bottom bar
(`_BarraCarritoInferior`, cantidad + total) that opens the same
`_ColumnaCarrito` in a bottom sheet on tap — that sheet's only dismiss
gesture used to be dragging it down (hard to hit on a real phone), so it now
also has an explicit close (X) button.

**Orientation**: the app was hard-locked to landscape — `SystemChrome
.setPreferredOrientations` in `main.dart` only allowed
`landscapeLeft`/`landscapeRight`, and `MainActivity`'s
`android:screenOrientation="landscape"` in `AndroidManifest.xml` enforced
the same lock at the Android level (the actual source of truth on Android —
Flutter's `SystemChrome` call alone doesn't override it). This made every
screen, including `LoginScreen`, render sideways on a phone held normally
in portrait — not a layout bug, the orientation lock itself. Fixed by adding
`portraitUp` to the allowed list in `main.dart` and removing
`android:screenOrientation` from the manifest entirely (falls back to
`unspecified`, i.e. follows the device's own rotation/lock). `portraitDown`
(upside-down) stays excluded on purpose — no POS use case for it, only risk
of a confusing flip if the device tips over. `LoginScreen`/`TiendaPickerScreen`
needed no changes — both are already a centered, width-constrained card with
no landscape-specific assumptions.

Verified with a real release APK (`flutter build apk --release
--dart-define=API_BASE_URL=https://inven365.com.gt`, a real deployed
backend with HTTPS — no cleartext exception needed) installed on a real
phone: the user confirmed the login screen still rendered landscape after
the `PosScreen` phone-layout fix (correctly diagnosed as the orientation
lock, not a `LoginScreen` bug) before this fix went in. **The fix itself
(orientation unlock + `_PosBodyTelefono` in portrait) has not yet been
re-verified on that same physical device** — this phase's manifest/`main.dart`
change was made and reasoned through, but the APK wasn't rebuilt and
reinstalled again in this session to confirm visually. Rebuild and reinstall
before considering this closed.

### Sistema de tema centralizado + modo oscuro — built this phase, partial rollout

Before this phase there was no real theming at all: `main.dart`'s `ThemeData`
was a bare `colorSchemeSeed`, no screen ever read `Theme.of(context)`, and
the same brand hex values were redeclared as local `_brand`/`_primary`/
`_danger` constants in ~10 files (`pos_colors.dart`, `DashboardPalette`, and
private consts in `caja_screen.dart`, `cuentas_por_cobrar_screen.dart`,
`pendientes_error_screen.dart`, `barcode_scanner_screen.dart`,
`cobro_sheet.dart`, `cliente_selector_sheet.dart`). The values already
matched `market-backoffice`'s tokens almost exactly (same session, same
palette decisions carried over informally) — this phase is about giving them
one source of truth and adding a real dark mode, not changing what anything
looks like in light mode.

New `core/theme/`: `app_colors.dart` (`AppColors`, light + dark, same hex as
`market-backoffice/src/styles/tokens.css` — brand/primary/accent stay
identical between themes on purpose, only surfaces/text/border/semantic
tones change, same reasoning as the backoffice), `app_theme.dart`
(`ThemeData.light`/`.dark` built from `AppColors` via an explicit
`ColorScheme`, not `ColorScheme.fromSeed` — needed exact hex control),
`theme_notifier.dart` (`ThemeNotifier extends Notifier<ThemeMode>`, default
always `ThemeMode.light`, never follows the OS theme — same policy as the
backoffice's `theme.store.ts` — persisted via `shared_preferences` under key
`inven365-tema`, same name as the backoffice's `localStorage` key though the
two are unrelated storages on different devices). `shared_preferences` is a
new dependency — nothing lightweight existed before for a UI preference
(`flutter_secure_storage` is OS-keychain-backed, overkill for this;
`isar_community` backs the offline queues, not preferences).

A sol/luna `IconButton` toggling `themeModeProvider.notifier.alternar()` was
added to `PosScreen` (the actual home screen after login) and to both
`DashboardEncargadoScreen`/`DashboardVendedorScreen` app bars — mirroring
where the backoffice put its own toggle (always-visible, next to
notifications/logout). `LoginScreen` itself has no toggle (same as the
backoffice login) — it just renders whatever `ThemeMode` is already active.

`LoginScreen` was reskinned to match the backoffice's own redesigned login
(brand mark badge instead of plain "Inven365" text, no bordered `Card`,
pill-shaped fields via a local `_pillDecoration`, `AppColors.of(context)`
throughout) — **all existing logic was preserved untouched**: the
`LayoutBuilder`/`SingleChildScrollView`/`ConstrainedBox` keyboard-overflow
fix, and the generic-credentials-vs-network-error message split
(`ApiException.isNetworkError`) both documented earlier in this file. Global
`InputDecorationTheme` (`app_theme.dart`) got a mild 12px rounded rectangle
for every other text field in the app — the pill shape is Login-only, a
local override, exactly like the backoffice keeps `mk-input` mostly
square-ish and only makes the login fields pill-shaped.

`pos_colors.dart` and `DashboardPalette` (`dashboard_widgets.dart`) were
updated to carry the exact same values as `AppColors.light` — **still
duplicated as `const` literals, not references**, because Dart doesn't allow
reading an instance field of another class inside a `const` expression
(`const x = AppColors.light.brand;` is a compile error:
`const_eval_property_access`). Each file now has a comment pointing back to
`app_colors.dart` as the source of truth to keep them in sync by hand if it
ever changes. `DashboardPalette.ink`/`.inkMuted` were nudged from their
slightly-different original hex (`#1E293B`/`#64748B`) to match
`AppColors.light.text`/`.textMuted` (`#1F2937`/`#6B7280`) exactly — an
imperceptible shift, done for consistency.

**Deliberately partial, staged like the backoffice's own theme rollout**:
none of `caja_screen.dart`, `cuentas_por_cobrar_screen.dart`,
`pendientes_error_screen.dart`, `barcode_scanner_screen.dart`,
`cobro_sheet.dart`, `cliente_selector_sheet.dart`,
`connectivity_badge.dart`, or the `pos/` sub-widgets were touched — they
still hardcode their own local color constants and render in light-mode
colors regardless of `themeModeProvider`. Toggling dark mode today only
visibly re-themes `LoginScreen`, `PosScreen`'s own chrome (app bar,
scaffold background), and the two dashboards' app bars/scaffold — the rest
of the app doesn't yet look wrong, it just doesn't respond. Migrating the
rest to `AppColors.of(context)` is future work, not started this phase.

Also fixed in passing: `test/widget_test.dart` asserted `find.text('Market')`
— stale from before the Market→Inven365 rebrand (confirmed via `git stash`
that this assertion already failed on `main` before this phase's changes,
unrelated to anything done here) — updated to check for the new login's
actual heading text (`'Bienvenido de nuevo'`).

Verified: `flutter analyze` clean, `dart format --set-exit-if-changed .`
clean, `flutter test` 62/62 (61 pre-existing + the widget_test fix).
Visually confirmed in Chrome (`flutter run -d web-server --web-port=8765
--dart-define=API_BASE_URL=http://localhost:8080`) — light login matches the
backoffice's redesigned login; dark login confirmed by temporarily forcing
`ThemeNotifier.build()` to return `ThemeMode.dark` for one screenshot, then
reverting immediately (the real default is verified back to
`ThemeMode.light` by the clean `flutter analyze`/`test` run taken
afterward). **Not verified**: `PosScreen`/dashboards with the toggle
actually switching live — no valid credentials were available against the
local backend this session to log in past `LoginScreen`, so the toggle
button itself and the rest of `PosScreen`/dashboard theming were only
verified by code review, not click-tested. Needs a real login session next
time to confirm the toggle actually flips `PosScreen`/dashboard chrome and
that nothing renders unreadable in dark mode.

### Modo oscuro fase 2 — el resto de las pantallas migradas a `AppColors`

Cierra el alcance parcial de la fase anterior: `caja_screen.dart`,
`cuentas_por_cobrar_screen.dart`, `pendientes_error_screen.dart`,
`barcode_scanner_screen.dart`, `cobro_sheet.dart`,
`cliente_selector_sheet.dart` y `connectivity_badge.dart` seguían con
constantes locales `_brand`/`_primary`/`_danger`/`_warning` fijas
(`const Color(0xFFxxxxxx)`) — ahora todas usan `AppColors.of(context)`
(`core/theme/app_colors.dart`), mismo patrón que `login_screen.dart`/
`pos_screen.dart`/los dos dashboards de la fase anterior: `final colors =
AppColors.of(context)` dentro de cada `build`, incluyendo los widgets
privados/`State` internos de cada archivo que también pintan color (cada uno
con su propio `build(BuildContext context)`, no uno solo compartido).
`connectivity_badge.dart` no tenía constantes `_brand`/`_primary` propias,
pero sí 3 colores de estado hardcodeados (Conectado/Sincronizando/Sin
conexión) — se mapearon a los campos semánticos ya existentes en
`AppColors` (`success`/`pending`/`danger`, mismos hex) en vez de agregar
tokens nuevos. `cliente_selector_sheet.dart` de paso migró un rojo de error
que estaba hardcodeado sin ser `const _xxx` (mismo hueco de fondo).

Con esto, TODAS las pantallas que pintan color propio ya responden al
modo oscuro — no queda ninguna pantalla con paleta fija conocida.

Verificado: `flutter analyze` limpio, `dart format --set-exit-if-changed .`
limpio, `flutter test` 62/62. **No verificado visualmente en Chrome/device**:
se intentó levantar `flutter run -d web-server` contra el backend local (que
sí respondía, con credenciales válidas) pero el renderer quedó en blanco/
congelado más de un minuto sin avanzar (mismo tipo de inestabilidad de
entorno vista con el backend en esta sesión) — se abortó el intento en vez
de seguir insistiendo; verificado solo por revisión de código y por los tres
chequeos automatizados de arriba.

### "Recordarme" + recuperar contraseña — built this phase

Paridad con el backoffice, que ya tenía ambas cosas (`LoginView.vue`,
`ForgotPasswordView.vue`, `ResetPasswordView.vue`) — la app solo tenía
usuario/contraseña sin ninguna de las dos.

- **Recordarme**: igual que en el backoffice, **solo recuerda el usuario
  tecleado** (`SharedPreferences`, clave `inven365-usuario-recordado` — misma
  clave lógica que `USUARIO_RECORDADO_KEY` en `LoginView.vue`), nunca la
  contraseña ni la sesión — la persistencia de sesión entre reinicios de la
  app es un tema aparte, ya cubierto por la cookie de refresh
  (`SecureCookieStorage`) y no se tocó. Checkbox en `LoginScreen`; si está
  marcado al hacer login exitoso guarda el usuario, si no lo borra.
- **Recuperar contraseña**: dos pantallas nuevas, `ForgotPasswordScreen`
  (`/olvide-password`) y `ResetPasswordScreen` (`/restablecer-password`),
  contra los mismos endpoints que ya usaba el backoffice
  (`POST /auth/forgot-password`, `POST /auth/reset-password` — no fue
  necesario tocar el backend). Diferencia real con el backoffice: el
  backoffice lee el token del enlace desde `?token=` en la URL (es una SPA
  web); esta app no tiene deep-linking configurado para abrir ese enlace
  directo, así que `ResetPasswordScreen` pide el código **pegado a mano** en
  un campo de texto en vez de leerlo de una URL — el usuario copia el código
  del correo y lo pega. `ForgotPasswordScreen` muestra siempre el mismo
  mensaje de éxito exista o no el usuario (igual que el backend, que nunca
  distingue el caso para no filtrar qué usuarios existen). Ambas rutas
  agregadas a `app_router.dart` como accesibles sin sesión (junto a
  `/login`), igual que en el resto del guard.
- Decoración de campo compartida extraída a `auth_pill_decoration.dart`
  (`authPillDecoration`) — antes vivía duplicada como método privado dentro
  de `LoginScreen`; ahora la usan las tres pantallas de auth.

Verificado: `flutter analyze` limpio, `dart format --set-exit-if-changed .`
limpio, `flutter test` 70/70 (8 tests nuevos: validaciones de
`ForgotPasswordScreen`/`ResetPasswordScreen` sin llamar al backend, y el
checkbox/precarga de `LoginScreen` con `SharedPreferences.setMockInitialValues`).

**Verificado en Chrome contra el backend real** (`flutter run -d web-server`,
segundo intento — la congelada de `flutter_run3.log` esta vez sí terminó de
compilar tras esperar más, y la causa real de los intentos previos "en
blanco" quedó identificada: no era el renderer, era CORS. `CORS_ALLOWED_ORIGINS`
del backend solo traía `http://localhost:5173` por default — el `POST
/auth/forgot-password` fallaba con "No se pudo conectar con el servidor" pese
a que el backend respondía perfecto por `curl`. Reiniciado con
`CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8765` y ya
funcionó). Flujo real probado de punta a punta: login con "Recordarme"
marcado → logout → recarga completa de página (no solo re-navegación SPA) →
usuario y checkbox siguen precargados, confirmando que `SharedPreferences`
persiste de verdad en el navegador. "¿Olvidaste tu contraseña?" → usuario
`admin` → "Revisa tu correo" (llamada real, sin error). "Ya tengo un código" →
las 3 validaciones de cliente (código vacío, contraseña corta, contraseñas
que no coinciden) muestran su mensaje sin tocar la red; con un código
inventado sí llama al backend real y muestra "El código es inválido o ya
expiró" (400 real). No se pudo probar el canje de un token real: el usuario
`admin` sembrado no tiene correo registrado (`correo: null`), así que el
backend nunca genera token ni envía correo para él (mismo diseño no-op que
usa para no filtrar qué usuarios existen) — probarlo de verdad requeriría un
usuario con correo real, lo que dispararía un envío de correo real por el
SMTP configurado; no se hizo sin que el cliente lo pida explícitamente.

