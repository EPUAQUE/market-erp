# Market · Backoffice ERP Retail Multi-Tienda

Backoffice administrativo para **Market**, un ERP Retail Multi-Tienda: varias
tiendas venden catálogos de productos distintos entre sí y necesitan controlar
inventario, compras, ventas, caja, créditos, gastos, rentabilidad y facturación
electrónica (FEL Guatemala) de forma centralizada. Construido con Vue 3,
TypeScript estricto y un sistema de diseño propio. Marca **Market** · sin
dependencias del ecosistema Prime.

> Este backoffice consume el backend real (`market-backend`, Java/Spring Boot) —
> no usa datos simulados ni mocks en tiempo de ejecución. El sistema completo
> tiene dos frentes: este backoffice (Vue) y un POS de tienda aparte (Flutter,
> `market-flutter`) que consume la misma API.

## Este proyecto es parte de un monorepo

Este repositorio vive dentro de [`market-erp`](https://github.com/EPUAQUE/market-erp)
junto a `market-backend` y `market-flutter`, como carpetas hermanas — el
`docker-compose.yml` de `market-backend` referencia `../market-backoffice`
como build context, así que ambos deben mantener esa estructura relativa:

```
market-erp/
  market-backend/       (Dockerfile, docker-compose.yml, pom.xml, src/, deploy/)
  market-backoffice/    (este repo)
  market-flutter/
```

Clonar el monorepo completo, no este directorio suelto, si el objetivo es
levantar el sistema con Docker Compose (ver `market-backend/deploy/README.md`).

## Módulos

1. **Seguridad** — usuarios, roles, permisos planos, alcance por tienda.
2. **Tiendas** — catálogo de sucursales.
3. **Unidades de Medida** — catálogo de unidades.
4. **Categorías** — catálogo de categorías de producto.
5. **Marcas** — catálogo de marcas.
6. **Productos** — catálogo global de productos, con configuración por tienda
   (precio de venta, stock mínimo/máximo, permitir venta/ingreso).
7. **Inventario** — existencias y kardex (movimientos de inventario).
8. **Compras** — órdenes de compra a proveedores, contado o crédito.
9. **Proveedores** — catálogo de proveedores.
10. **Cuentas por Pagar** — saldos y pagos a proveedores.
11. **Clientes** — catálogo de clientes.
12. **Ventas** — ventas por tienda con métodos de pago mixtos, incluida venta
    a crédito.
13. **Cuentas por Cobrar** — saldos y pagos de clientes.
14. **Caja** — apertura/cierre y movimientos de caja por tienda.
15. **Traslados** — movimiento de inventario entre tiendas.
16. **Gastos Programados** — gastos recurrentes.
17. **Notificaciones** — alertas y avisos (vencimientos, stock, etc.).
18. **Facturación Electrónica FEL** — integración con certificador FEL Guatemala.
19. **Dashboard** — indicadores de negocio en tiempo real.
20. **Reportes** — exportación CSV de ventas y compras.

Este backoffice cubre la administración transversal (multi-tienda); el POS de
Flutter cubre la operación diaria de venta/caja dentro de una sola tienda.

## Stack tecnológico

| Área     | Tecnología                                    |
| -------- | ---------------------------------------------- |
| Base     | Vue 3.5 · Vite 6 · TypeScript estricto · pnpm  |
| Estado   | Pinia                                          |
| Ruteo    | Vue Router (lazy + guards por permiso)         |
| UI       | Tailwind CSS 3 (sistema de diseño propio, sin librería de componentes) |
| Datos    | Axios                                          |
| Gráficos | Chart.js · vue-chartjs                         |

Sin librería de tablas/formularios/exportación de terceros — tablas, formularios
y validación son Vue simple (`ref`/`computed`/`watch`) por módulo, no una capa
compartida tipo TanStack/VeeValidate. Sin i18n (texto en español directo en los
templates). Vitest está configurado (`pnpm test`) pero la cobertura hoy es
parcial — solo Clientes y Compras (servicios + composables) tienen tests; el
resto del proyecto no tiene ninguno. Sin ESLint, Prettier ni Playwright. No se
utiliza **PrimeVue / PrimeFaces / PrimeIcons** ni ninguna librería Prime.

## Requisitos

- Node.js LTS (≥ 20)
- pnpm ≥ 9

## Instalación

```bash
pnpm install
```

## Variables de entorno

Copiar `.env.example` a `.env` (o usar el `.env.development` ya incluido, con
los mismos valores por defecto):

| Variable           | Descripción                                                            |
| ------------------ | ----------------------------------------------------------------------- |
| `VITE_API_BASE_URL` | URL base del backend real (obligatoria — sin esto la app no arranca). |
| `VITE_API_TIMEOUT`  | Timeout de las peticiones HTTP en ms (default `15000`).               |

## Scripts

```bash
pnpm dev            # Servidor de desarrollo
pnpm build          # vue-tsc -b + build de producción
pnpm preview        # Previsualizar el build
pnpm typecheck      # vue-tsc -b --noEmit
pnpm test           # vitest run — cobertura parcial, ver "Limitaciones conocidas"
pnpm test:watch
```

## Credenciales

No hay cuentas de demostración locales — este backoffice autentica contra el
backend real. El primer usuario ADMIN se siembra desde `market-backend`
(`SEED_ADMIN_USERNAME`/`SEED_ADMIN_PASSWORD`); ver
`market-backend/deploy/README.md` § "Primer admin" para el procedimiento
completo en un deploy nuevo.

## Arquitectura

Organización por dominios bajo `src/`:

```
components/common · components/layout
composables/ config/ router/ services/ stores/ styles/ types/ views/
views/admin/ (los 21 módulos) · views/LoginView.vue · views/ForbiddenView.vue
```

- **Servicios** (`src/services`, uno por módulo + `services/http/`) encapsulan
  Axios; `ApiClient` centraliza el manejo de sesión expirada/refresh (ver
  "Manejo de permisos" abajo).
- **Composables** (`src/composables`, un `use<Módulo>.ts` por módulo) son la
  única capa de estado por vista — `ref`/`computed`/`watch` simple, sin store
  de Pinia por módulo ni librería de fetching (TanStack Query y similares no
  están en uso).
- **Stores** (Pinia: `auth`, `user`, `permissions`) son puramente en memoria —
  sin plugin de persistencia; recargar la página pierde la sesión por diseño,
  igual que el access token (ver abajo).
- Tablas, formularios y validación son Vue simple por vista, no una capa
  compartida — no hay librería de tablas/formularios en el proyecto.

## Sistema de diseño

Tokens semánticos en `src/styles/tokens.css` (canales RGB) + `tailwind.config.ts`.
Incluye tokens para superficies, texto, estados de negocio (ventas, mermas,
stock bajo, en tránsito, riesgo…) y gráficos, con variantes **claro/oscuro**.

## Manejo de permisos

Modelo de autorización RBAC plano + alcance por tienda: códigos de permiso planos
(`PermissionCode`, ej. `PRODUCTOS_VER`, `VENTAS_CREAR`, `CAJA_CERRAR`, ver
`src/types/auth.ts`) más la lista de tiendas a las que el usuario está asignado.
Las rutas declaran `meta.permission` (y `meta.tiendaScoped` cuando la vista opera
sobre una tienda concreta) y el guard (`src/router/guards.ts`) bloquea el acceso
directo por URL; la UI oculta acciones no autorizadas mediante
`usePermissionsStore().can(permissionCode)` / `canAccessTienda(tiendaId)`.

El **access token** vive solo en memoria (nunca en `localStorage`); su renovación
usa un **refresh token** opaco y rotatorio gestionado por el backend — el frontend
nunca lo lee ni lo parsea, solo dispara `POST /api/v1/auth/refresh` cuando el
access token expira.

## Decisiones técnicas

- Vite 6 + vue-tsc, sin librerías de terceros para tablas/formularios/gráficos
  más allá de Chart.js — mantiene el bundle chico y evita atarse a un sistema
  de componentes ajeno al diseño propio de Market.
- Tokens en canales RGB para permitir opacidad Tailwind (`rgb(var(--x) / <alpha>)`).

## Limitaciones conocidas

- Cobertura de pruebas parcial — solo Clientes y Compras tienen tests
  (Vitest, `pnpm test`); el resto del proyecto no tiene ninguno, y no hay E2E.
- Sin i18n — texto en español hardcodeado en los templates.
- Exportación limitada a CSV hecho a mano (`Reportes` — ventas/compras); sin
  Excel ni PDF (no hay librerías de exportación instaladas).

## Mejoras futuras

- Paginación real del lado del servidor ya cubre Ventas, Cuentas por Cobrar,
  Traslados, Productos, Inventario, Caja, Clientes y Compras — Categorías,
  Marcas, Proveedores y Unidades de Medida se quedan client-side a propósito
  (catálogos chicos, no vale la pena la complejidad).
- Orden/filtrado del lado del servidor y virtualización de tablas grandes.
- Flujos de aprobación multinivel (compras) y adjuntos reales.
- Extender la cobertura de pruebas al resto de módulos, y agregar E2E.
</content>
