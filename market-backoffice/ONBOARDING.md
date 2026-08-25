# ONBOARDING — Market Backoffice

Guía rápida para nuevos desarrolladores. Basada en `CLAUDE.md`, pero corregida
contra el código real: donde `CLAUDE.md` describe una convención que todavía no
existe en el repo, aquí se marca explícitamente como pendiente — no lo tomes
como implementado solo porque está documentado.

## Qué es esto

Backoffice administrativo (Vue 3 + Pinia + Tailwind) del ERP **Market**, un
Retail Multi-Tienda: inventario, compras, ventas, caja, créditos, gastos
programados y facturación electrónica (FEL Guatemala). Habla por REST con
`market-backend` (Modular Monolith, Java 25 / Spring Boot 4). No comparte
código con `market-flutter`, la app POS de tienda que golpea el mismo backend.

## Arranque rápido

```bash
pnpm install
cp .env.example .env      # o usa .env.development ya versionado
pnpm dev                  # Vite dev server
```

Variables clave en `.env`: `VITE_API_BASE_URL` (backend real, p. ej.
`http://localhost:8080`), `VITE_API_TIMEOUT`, `VITE_ENABLE_MOCKS`.

Scripts realmente configurados en `package.json` hoy:

```bash
pnpm dev
pnpm build        # vue-tsc -b && vite build
pnpm typecheck    # vue-tsc -b --noEmit
pnpm preview
```

No hay `lint`, `format`, `test` ni `test:e2e` configurados todavía (ver
"Brechas conocidas" abajo).

## Módulos (19) — todos con ruta, vista y permiso propio

| Módulo | Ruta | Permiso VER |
|---|---|---|
| Dashboard | `/dashboard` | `DASHBOARD_VER` |
| Usuarios | `/usuarios` | `USUARIOS_VER` |
| Tiendas | `/tiendas` | `TIENDAS_VER` |
| Unidades de Medida | `/unidades-medida` | `UNIDADES_MEDIDA_VER` |
| Categorías | `/categorias` | `CATEGORIAS_VER` |
| Marcas | `/marcas` | `MARCAS_VER` |
| Productos (+ ProductoTienda) | `/productos`, `/productos/:productoId/tiendas` | `PRODUCTOS_VER` |
| Inventario | `/inventario` | `INVENTARIO_VER` |
| Proveedores | `/proveedores` | `PROVEEDORES_VER` |
| Compras | `/compras` | `COMPRAS_VER` |
| Cuentas por Pagar | `/cuentas-por-pagar` | `CUENTAS_POR_PAGAR_VER` |
| Clientes | `/clientes` | `CLIENTES_VER` |
| Ventas | `/ventas` | `VENTAS_VER` |
| Cuentas por Cobrar | `/cuentas-por-cobrar` | `CUENTAS_POR_COBRAR_VER` |
| Caja | `/caja` | `CAJA_VER` |
| Traslados | `/traslados` | `TRASLADOS_VER` |
| Gastos Programados | `/gastos-programados` | `GASTOS_PROGRAMADOS_VER` |
| Notificaciones | `/notificaciones` | `NOTIFICACIONES_VER` |
| Reportes | `/reportes` | `REPORTES_VER` |
| FEL | `/fel` | `FEL_VER` |

`/` redirige a `/dashboard`. Cada ruta protegida exige `meta.permission`; el
guard (`src/router/guards.ts`) redirige a `/forbidden` si el usuario no lo
tiene.

## Patrón por módulo (real, así están hechos los 19)

```
src/types/<modulo>.ts              # interfaces TS del dominio (Money viaja como string)
src/services/<modulo>.service.ts   # clase delgada, llama apiClient directo
src/composables/use<Modulo>.ts     # items/loading/error refs + acciones
src/views/admin/<Modulo>View.vue   # selector de tienda local + tabla + formulario
```

No hay Pinia store por módulo — solo para estado global de sesión (`auth`,
`user`, `permissions`; hoy son los únicos tres stores que existen).

**Capa HTTP** (`src/services/http/`): `ApiClient.ts` envuelve Axios;
`token.service.ts` guarda el access token solo en memoria (recargar la página
lo pierde, a propósito); en un 401 intenta un refresh silencioso una sola vez
antes de cerrar sesión. Rutas de API centralizadas en `src/config/endpoints.ts`
— nunca hardcodear una URL en un service.

**Auth y permisos**: RBAC plano + alcance por tienda.
`usePermissionsStore().can('ALGO_VER')` gatea UI y rutas.
`AuthService.login` + `/api/v1/auth/me` hidratan `permissions.store` y
`user.store`.

**Alcance por tienda — distinto a lo que dice `CLAUDE.md`**: no hay un
`tienda.store` global ni un switcher en el header del layout. Cada vista
tienda-scoped (Inventario, Caja, Ventas, Cuentas x Pagar/Cobrar, Gastos
Programados, Notificaciones, Reportes, FEL, Traslados) trae su propio
`<select>` de tienda local, cargado con `useTiendas()` al montar. Así están
hechos los 19 módulos hoy; si se agrega un store global de tienda más
adelante, esta guía y `CLAUDE.md` deben actualizarse juntos.

**Dinero**: el backend formatea `BigDecimal.toPlainString()` y lo manda como
`string` en el JSON; el frontend solo lo muestra (clase `mk-num` para
alinear cifras). No existe todavía un `src/utils/money.ts` con Decimal.js
— si se necesita sumar/restar montos en el cliente, ese util aún no está
construido.

## Backend companion

`market-backend` — ver su `ARCHITECTURE.md`. Modular Monolith + DDD + Clean
Architecture, Postgres + Liquibase, JWT + refresh tokens. Cada módulo del
backoffice tiene su espejo 1:1 en el backend (dominio → aplicación →
infraestructura → API).

## Brechas conocidas (documentadas en `CLAUDE.md`, no implementadas aún)

- **Sin lint/format/tests**: no hay ESLint, Prettier, Vitest ni Playwright
  configurados en `package.json` pese a que `CLAUDE.md` los documenta.
- **Sin i18n**: no existe `src/i18n/`; los textos están hardcodeados en
  español directo en las vistas, no vía claves de traducción.
- **Pantallas de mantenimiento simples**: ningún módulo implementa aún la
  spec normativa completa de `CLAUDE.md` (paginación 10/25/50/100, exportar
  Excel/CSV/PDF con log de auditoría, carga masiva, histórico). Las vistas
  actuales son tabla + formulario + acciones básicas.
- **FEL usa un certificador de desarrollo**: `DevCertificadorFelAdapter` (en
  el backend) genera un UUID local en vez de integrar con un proveedor
  certificador real autorizado por la SAT (Infile, Digifact, G4S, etc.). El
  flujo de emitir/anular/reintentar es real y completo; lo que falta es la
  integración fiscal real antes de producción.
- **Sin mocks (MSW)**: no existe `src/mocks/`; todo apunta al backend real
  vía `.env`.

Si vas a cerrar alguna de estas brechas, actualiza esta sección y la de
`CLAUDE.md` en el mismo cambio para que no vuelvan a divergir.
