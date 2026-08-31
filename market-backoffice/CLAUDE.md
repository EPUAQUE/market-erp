# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

`market-backoffice` is the admin/backoffice frontend for **Market**, a Retail
Multi-Tienda ERP: inventario, compras, ventas, caja, créditos, gastos y facturación
electrónica (FEL Guatemala) across several stores, each selling its own product
catalog. All 19 modules are live: Tiendas, Categorías, Marcas, Unidades de Medida,
Productos (+ ProductoTienda), Inventario, Proveedores, Compras, Cuentas por Pagar,
Clientes, Ventas, Cuentas por Cobrar, Caja, Traslados, Gastos Programados,
Notificaciones, Dashboard, Reportes, and FEL — each with real views under
`src/views/admin/`, a service/composable/type per module, and a route guarded by
its own permission code. Nothing here is scaffolded/aspirational anymore; treat the
sections below as a description of what exists, not a target to build toward.
One caveat: FEL's backend certification currently runs against a dev/stub adapter
(no real SAT-certified provider like Infile/Digifact/G4S is wired up yet) — the
frontend flow (emitir/anular/reintentar) is real and complete regardless.

## Server-side pagination (Ventas, Cuentas por Cobrar, Traslados, Productos, Inventario, Caja)

The backend added real (`Pageable`/`Page<T>`) pagination to these six modules' listing
endpoints — each now returns `{contenido, pagina, tamano, totalElementos, totalPaginas}`
(`src/types/pagina.ts`, `Pagina<T>`) instead of a plain JSON array. Every affected
`*.service.ts` method now takes `(pagina: number, tamano: number)` (0-based, matching the
backend) and returns `Pagina<T>`; every affected composable exposes 1-based `pagina`/
`tamano`/`totalElementos`/`totalPaginas` refs (converts `pagina - 1` when calling the
service) and the views got the same pagination footer markup `ClientesView.vue` already
used for client-side paging (select 10/25/50/100 + Anterior/Página X de Y/Siguiente),
wired to a `watch([pagina, tamano], () => cargar(...))` instead of a `computed` slice.
Mutations (`crear`/`completar`/`anular`/etc.) now re-fetch the current page from the
server afterward instead of patching `items` in place client-side — needed to keep
`totalElementos`/`totalPaginas` correct, not just the visible rows.

**`useProductos()` is shared by non-paginated consumers** (`VentasView`/`TrasladosView`/
`ComprasView`/`InventarioView` all call it just for a full product dropdown, not the
catalog table) — since composables here are plain factory functions (not singletons),
each call site gets its own independent `pagina`/`tamano` refs. `tamano` defaults to
`5000` (effectively "the whole catalog") so those dropdown call sites are unaffected;
only `ProductosView.vue` explicitly overrides its own instance's `tamano.value = 10` to
get real pagination. `ProductoTiendasView` (`listarTiendas`/`listarPorProducto`) was
**deliberately left unpaginated** — the backend didn't paginate it either, since it's
bounded by the number of tiendas (naturally small), not worth the complexity.

**Known trade-off, not a bug**: the client-side text search boxes (Ventas has none;
Productos does) now only filter the *currently loaded page*, not the whole dataset —
before this change they filtered everything, since everything was loaded at once. Adding
real server-side search was out of scope for this pass (the backend only added `page`/
`size`, no search query params) — flagged here rather than silently changed.

**Bug found and fixed during visual verification**: changing the page-size selector while
on a page beyond what the new size covers (e.g. on página 2 of 10/página, switch to
25/página) left `pagina` unchanged, so the next fetch requested a page that no longer
existed — server returned an empty `contenido`, and the footer showed a nonsensical
"Página 2 de 1". Fixed in all six views by adding `watch(tamano, () => { pagina.value = 1 })`
ahead of the existing `watch([pagina, tamano], () => cargar(...))` (same fix applied to
`historialPagina`/`historialTamano` in `CajaView` and `movimientosPagina`/`movimientosTamano`
in `InventarioView`).

Verified end-to-end in a real browser against the real backend (Docker Compose,
seeded data: 15 productos, 3 ventas a crédito, 1 traslado, 1 sesión de caja cerrada,
5 filas de inventario con kardex): all six views — Ventas, Productos, Traslados,
Cuentas por Cobrar, Inventario (both the stock table and its independent kardex
sub-table), Caja (historial) — rendered their pagination footer with the correct
`totalElementos`, "Siguiente" correctly enabled/disabled at the last page, and clicking
through Productos' página 1 → 2 → resizing to 25/página actually re-fetched from the
server and landed back on página 1 with all 15 rows (post-fix). `pnpm typecheck` clean
after the fix.

The companion backend (`market-backend`, see its `ARCHITECTURE.md`) is a Modular
Monolith with Java 25 / Spring Boot 4. This frontend talks to it over REST; it does
not share code with the Flutter POS app (`market-flutter`), which is a separate
client hitting the same backend for store-level point-of-sale operations.

## Commands

Package manager is **pnpm** (Node ≥ 20).

```bash
pnpm dev                        # Vite dev server
pnpm build                      # vue-tsc -b + vite build
pnpm typecheck                  # vue-tsc -b --noEmit
pnpm test                       # vitest run (unit) — colocated *.spec.ts next to source
pnpm test:watch

# Single unit test file / by name:
pnpm vitest run src/services/clientes.service.spec.ts
pnpm vitest run -t "cargar convierte pagina"
```

**ESLint 9 (flat config, `eslint.config.js`) + Prettier (`.prettierrc.json`) added
2026-08-31** (Fase 8, PLAN_MEJORAS.md) — `pnpm lint` / `pnpm format` / `pnpm format:check`
exist and both run in CI (`pnpm lint` and `pnpm format:check`, before typecheck). No
Playwright yet — don't assume `test:e2e` exists. Vitest itself was only wired up when
tests were first written for the Clientes/Compras pagination work — most of the app
still has zero test coverage, this is not yet a general convention followed project-wide.

Copy `.env.example` to `.env` (or use a committed `.env.development`) and set
`VITE_API_BASE_URL` (real backend, e.g. `http://localhost:8080`) and `VITE_API_TIMEOUT`.
`environment.ts` throws at startup if `VITE_API_BASE_URL` is missing — Vitest sets it
explicitly via `test.env` in `vite.config.ts` (no separate `vitest.config.ts` file) since
the HTTP layer reads it at import time.

## Architecture

Path alias `@/` → `src/`. TypeScript strict (`noUnusedLocals`, `noImplicitReturns`,
`verbatimModuleSyntax` — use `import type` for types, `exactOptionalPropertyTypes` off).
Vue SFCs use `<script setup lang="ts">`.

**HTTP layer — `src/services/http/`:**

- `ApiClient.ts` exports a singleton `apiClient` wrapping Axios entirely: no Vue Router
  import, no Pinia store import (auth state lives in a decoupled `token.service.ts` to
  avoid circular imports). Base URL/timeout come from `src/config/environment.ts`.
- Per-request auth is controlled by `requiresAuth` in `ApiRequestOptions` (default `true`),
  not by URL — the login call itself passes `requiresAuth: false` so a 401 there doesn't
  trigger session teardown.
- On a 401 for an authenticated request: `ApiClient` attempts a **single silent refresh**
  via `POST /api/v1/auth/refresh` (see Auth & permissions below); only if that also fails
  does `tokenService.clear()` run, followed by an `onUnauthorized` callback (wired in
  `src/router/index.ts`, not inside `ApiClient`) that logs out and redirects to
  `/login?sessionExpired=1`. Concurrent 401s must share one in-flight refresh call, not
  fire a refresh per failed request.
- `ApiClient.ts` also exports the internal `refreshAccessToken` function it uses for that
  401 path — `auth.store.ts`'s `trySilentLogin()` reuses it (same in-flight dedupe) to
  attempt a **proactive** refresh from `authGuard` (`src/router/guards.ts`) whenever
  `tokenService.hasToken()` is `false` on navigation, before redirecting to `/login`. The
  access token lives only in memory (see below) so it's always gone after a page reload;
  this lets a reload silently restore the session from the refresh-token cookie instead of
  always bouncing to the login screen (added 2026-08-31, Fase 8).
- `get`/`post`/`put`/`delete` on `apiClient` all accept an optional `signal` (AbortSignal)
  in `ApiRequestOptions`, threaded through to Axios. The 8 composables backing
  server-paginated lists (`useCaja`/`useClientes`/`useCompras`/`useCuentasPorCobrar`/
  `useInventario`/`useProductos`/`useTraslados`/`useVentas`) use it: each `cargar*`
  function aborts its own previous in-flight call before starting a new one (a
  module-scoped `AbortController`, guarded by `if (thisController === controller)` in
  `finally` so a superseded call's `finally` can't clobber `*Loading` after a newer call
  already started) — added 2026-08-31 (Fase 8) to stop a fast page/size change from
  racing an older response into overwriting a newer one. Other composables (plain-array,
  client-side-paginated modules) don't need this — they don't refetch on every page click.
- All errors are normalized through `error.mapper.ts` into `ApiClientError` (carries
  `status`, `code`, `correlationId`, `retryAfterMs` for 429s, `isCanceled` for aborted
  requests).
- Endpoint paths are centralized in `src/config/endpoints.ts` (`API_ENDPOINTS`) — never
  inline a URL string in a service.

**Service → composable → view (no Pinia store for CRUD modules):**

- `src/services/<module>.service.ts` — a thin class instance (e.g. `productosService`,
  `ventasService`, `categoriasService`) exposing typed CRUD methods that call `apiClient`
  directly.
- `src/composables/use<Module>.ts` — owns UI state (`items`, `*Loading`, `*Error` refs),
  in-flight request cancellation via `AbortController`, and calls the service. Prefer this
  over a Pinia-store-per-module pattern — CRUD/maintenance modules don't need their own store.
- `src/views/admin/<Module>View.vue` + `src/components/<module>/*` — search/filter state,
  table, form dialog, bulk-upload dialog, log/audit table.
- Pinia (`src/stores/*.store.ts`) is reserved for genuinely global/session state:
  `auth.store`, `user.store`, `permissions.store`, `theme.store`, `layout.store`,
  `tienda.store` (current tienda/sucursal in scope for the session), `notifications.store`.

**Auth & permissions:**

- Login (`authService.login`, `src/services/auth/AuthService.ts`) hits
  `/api/v1/auth/login`, then immediately fetches `/api/v1/auth/me` for effective
  authorization (roles, flat permission codes, assigned tiendas); both are hydrated into
  `permissions.store` and `user.store` from `auth.store.login()`.
- The **access token lives only in memory** (`token.service.ts`) — no localStorage,
  sessionStorage, cookies. Reloading the page always loses the access token by design.
- The **refresh token** is a separate concern from the access token: it is opaque,
  single-use and rotating on the backend (see `market-backend/seguridad-desarrolladores.md`
  §5). This frontend never reads or parses it — it either lives in an `HttpOnly` cookie
  set by the backend (preferred) or is handled by a thin wrapper that never exposes it to
  application code. `token.service.ts` only ever holds the current access token in a
  module-level variable.
- Authorization model is **flat RBAC + tienda scope**, not the old opción/proceso/bitstring
  model: `PermissionCode` (`src/types/auth.ts`, e.g. `'PRODUCTOS_VER' | 'VENTAS_CREAR' |
  'CAJA_CERRAR'`) plus the list of `tiendaId`s the user is scoped to.
  `usePermissionsStore().can(permissionCode)` / `canAny(...)` gate UI; a permission check
  for a tienda-specific screen must also call `usePermissionsStore().canAccessTienda(tiendaId)`
  — a permission alone does not imply access to every tienda. Unknown permission codes from
  the backend are dropped (fail-closed).
- Route guarding (`src/router/guards.ts`, `authGuard`): checks `meta.requiresAuth`, lazily
  calls `auth.loadAuthorization()` on first authenticated navigation, then checks
  `meta.permission` (and `meta.tiendaScoped` when the view operates on a single tienda)
  against the permissions store, redirecting to `forbidden` on denial. Add `permission` to
  `meta` on any new protected route.
- Rate limiting: a 429 on login sets `rateLimitUntil` from `ApiClientError.retryAfterMs`
  (parsed by `src/utils/retry-after.ts`); the login form must respect `isRateLimited()`.

**Tienda context:** most CRUD modules (inventario, caja, ventas, traslados) are scoped to
the tienda currently selected in `tienda.store`. A user assigned to a single tienda skips
the selector; a user assigned to multiple tiendas (or an `ADMIN` role) gets a tienda
switcher in the layout header. Every request to a tienda-scoped endpoint must carry the
selected `tiendaId` explicitly (path or query param) — never infer it silently from stored
state inside a service.

**Mocks:** MSW (`src/mocks/`) should cover at least the auth endpoints (login, refresh,
me) for tests/local fallback; real CRUD modules hit the real API per `.env` once the
backend exists.

## Money — non-negotiable

All monetary math uses **Decimal.js** via `src/utils/money.ts` — **never native float
arithmetic**. Values are `Money { amount: string, currency, precision }`. Use
`money()`, `addMoney`/`subtractMoney`/`multiplyMoney`/`sumMoney`, `toDecimal`,
`percentageChange` (guards divide-by-zero). Same-currency ops assert and throw on mismatch.
Formatting lives in `src/utils/format.ts` (`formatCurrency`, `formatAccountingCurrency`, etc.).
Required for precios de venta, costos, utilidades, saldos de cuentas por cobrar/pagar y
cualquier cálculo de caja.

## Design system

Real brand as of the latest redesign (verde petróleo / esmeralda / ámbar ERP look —
not the old generic blue). Light-only (no dark mode — deliberately dropped when the
palette was set; the old `tokens.css` had a `prefers-color-scheme: dark` block, this
one doesn't).

- Tokens in `src/styles/tokens.css` are **RGB channels** (`--mk-*`) so Tailwind opacity
  works: `rgb(var(--mk-primary) / <alpha>)`. Key ones: `--mk-brand` (petróleo #0F4C5C —
  sidebar), `--mk-primary` (esmeralda #2E8B57 — buttons/active state), `--mk-accent`
  (ámbar #D9A441), `--mk-bg` (#F8FAFC), plus semantic state tokens `--mk-success`,
  `--mk-pending`, `--mk-overdue`, `--mk-danger`, `--mk-info`.
- Font: **Plus Jakarta Sans** (loaded via Google Fonts `@import` in `main.css`) for
  everything — no separate mono/serif face.
- Reusable component classes in `src/styles/main.css` `@layer components`: `mk-card`
  (rounded-mk + shadow-mk + border), `mk-btn` / `mk-btn-primary` / `mk-btn-outline` /
  `mk-btn-ghost` / `mk-btn-danger`, `mk-input`, `mk-num` (tabular figures), `mk-scroll-x`,
  and `mk-badge` / `mk-badge-{success,pending,overdue,danger,info,neutral}` — soft pill
  status tags. Prefer these over ad-hoc utility soup.
- **Status badges**: every module that renders an `estado`/`activo` column uses the
  shared `src/components/common/EstadoBadge.vue` (`:variant` + `:label` props) instead
  of raw text — map each module's own enum values to one of the 6 variants locally in
  that view (see `CuentasPorPagarView.vue`/`CuentasPorCobrarView.vue` for the pattern
  that also derives an `overdue` variant by comparing `fechaVencimiento` to now).
- **Layout**: `AdminLayout.vue` is a real sidebar + header shell — sidebar grouped nav
  (Catálogo/Operación/Ventas/Administración) with an active-item accent bar, a
  "sucursal" indicator (derived from `permissions.store`'s `alcanceGlobal`/`tiendaIds`,
  not a dedicated tienda store — see the Tienda context note below), header with
  module title + breadcrumb (from each route's `meta.title`), a working client-side
  global search over the nav items, a notifications bell, and 2 quick-action buttons
  (Crear Venta / Registrar Compra) gated by permission.
- No icon library — the handful of header/sidebar icons are hand-written inline SVGs.
  **No PrimeVue / Prime\* libraries** either — intentionally absent, do not add them.
- No i18n — text is hardcoded Spanish directly in templates (no `src/i18n/`, no
  `vue-i18n` dependency, despite what an older pass of this doc used to say).

## Forms & maintenance screens

This is the **normative spec** for every search/list/maintenance screen. Key points:

- Pick a simple catalog module (e.g. **Categorías** or **Marcas**) as the **reference
  implementation** for plain lookup CRUD — new maintenance screens must implement its full
  capability set unless a documented functional/technical exception applies: header
  (title, description, icon), audit-logged export, bulk upload, single/consecutive create,
  general + per-column search, table, view/edit/delete, histórico, granular per-action
  permissions, pagination, interface states. **Productos** builds on the same pattern but
  adds category/marca/unidad relations and per-tienda overrides (`ProductoTienda`); use it
  as the reference for a richer module with tienda-scoped child data.
- Boolean domain fields with backend values `S`/`N` (e.g. `Activo`, `PermitirVenta`,
  `PermitirIngreso`) must always render as **Sí/No** to the user — never show `S`/`N` in
  tables, filters, or exports; both need individual filter selectors (Todos/Sí/No) in
  addition to being covered by general search.
- Pagination starts at **10 rows/page**, selectable **25/50/100**; if the backend returns
  the full array (unpaginated), search/filter/pagination run **client-side** — don't invent
  query params the API doesn't support. **Six modules now paginate server-side** (see
  "Server-side pagination" below): Ventas, Cuentas por Cobrar, Traslados, Productos
  (catálogo), Inventario (existencias + kardex), Caja (historial de sesiones). Everything
  else (Categorías, Marcas, Clientes, Proveedores, Compras, etc.) still gets the full array
  from the backend and paginates/filters client-side as before — don't assume every module
  paginates server-side just because these six do.
- Export menu must offer **Excel (.xlsx) / CSV / PDF**, export the filtered result set (not
  just the visible page), and register an audit log call when an export endpoint exists for
  that option.
- Breadcrumbs must show human-readable names, not raw codes/IDs.
</content>
