# Market · Backoffice ERP Retail Multi-Tienda

Backoffice administrativo para **Market**, un ERP Retail Multi-Tienda: varias
tiendas venden catálogos de productos distintos entre sí y necesitan controlar
inventario, compras, ventas, caja, créditos, gastos, rentabilidad y facturación
electrónica (FEL Guatemala) de forma centralizada. Construido con Vue 3,
TypeScript estricto y un sistema de diseño propio. Marca **Market** · sin
dependencias del ecosistema Prime.

> ⚠️ Al arrancar, los datos son ficticios y las operaciones de escritura están
> simuladas mediante MSW hasta conectar el backend real (ver sección "Mocks →
> API real"). El sistema completo tiene dos frentes: este backoffice (Vue) y
> un POS de tienda aparte (Flutter, `market-flutter`) que consume la misma API.

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
20. **Reportes** — exportación (Excel/CSV/PDF) de ventas, inventario y
    rentabilidad.

Este backoffice cubre la administración transversal (multi-tienda); el POS de
Flutter cubre la operación diaria de venta/caja dentro de una sola tienda.

## Stack tecnológico

| Área        | Tecnología                                                                           |
| ----------- | ------------------------------------------------------------------------------------ |
| Base        | Vue 3.5 · Vite 5 · TypeScript estricto · pnpm                                        |
| Estado      | Pinia (+ persistedstate) · TanStack Query                                            |
| Ruteo       | Vue Router (lazy + guards por permiso)                                               |
| UI          | Tailwind CSS · Radix Vue · Material Symbols · Lucide · Floating Vue · Vue Sonner     |
| Datos       | TanStack Table · Axios · **Decimal.js** · date-fns · Maska                           |
| Gráficos    | Apache ECharts · vue-echarts                                                         |
| Formularios | VeeValidate · Zod                                                                    |
| Exportación | SheetJS (xlsx) · jsPDF + autotable · FileSaver                                       |
| i18n        | vue-i18n (es predeterminado, en secundario)                                          |
| Calidad     | ESLint · Prettier · Vitest · Vue Test Utils · Playwright · MSW · Husky · lint-staged |

No se utiliza **PrimeVue / PrimeFaces / PrimeIcons** ni ninguna librería Prime.

## Requisitos

- Node.js LTS (≥ 20)
- pnpm ≥ 9

## Instalación

```bash
pnpm install
```

MSW instala su service worker en `public/mockServiceWorker.js` (ya incluido).

## Variables de entorno

Copiar `.env.example` a `.env`:

| Variable            | Descripción                                                      |
| ------------------- | ---------------------------------------------------------------- |
| `VITE_API_BASE_URL` | URL base del backend (por defecto `/api`, interceptado por MSW). |
| `VITE_ENABLE_MOCKS` | `true` activa MSW; `false` para conectar una API real.           |
| `VITE_APP_NAME`     | Nombre visible de la app.                                        |

## Scripts

```bash
pnpm dev            # Servidor de desarrollo
pnpm build          # Typecheck + build de producción
pnpm preview        # Previsualizar el build
pnpm typecheck      # vue-tsc sin emitir
pnpm lint           # ESLint
pnpm format         # Prettier
pnpm test           # Vitest (unitarias)
pnpm test:coverage  # Cobertura
pnpm test:e2e       # Playwright
```

## Credenciales de demostración

Contraseña para todas las cuentas: **`demo1234`**

| Rol                  | Correo                 |
| --------------------- | ---------------------- |
| Administrador         | `admin@market.demo`     |
| Encargado de tienda   | `encargado@market.demo` |
| Cajero / vendedor     | `cajero@market.demo`    |
| Auditor / inventario  | `auditor@market.demo`   |

## Arquitectura

Organización por dominios bajo `src/`:

```
components/  common · charts · tables · forms · layout
composables/ config/ constants/ i18n/ layouts/ mocks/
models(types)/ router/ services/ stores/ styles/ utils/ views/
```

- **Servicios** (`src/services`) encapsulan Axios; interceptores añaden
  `X-Correlation-Id`, `Authorization` y normalizan errores + sesión expirada.
- **Stores** (Pinia) mantienen estado con `loading`/`error`; persisten solo
  tema, idioma, preferencias de tabla, estado del sidebar, tienda/sucursal y
  período.
- **MSW** (`src/mocks`) simula latencia, paginación, filtros y errores.

## Sistema de diseño

Tokens semánticos en `src/styles/tokens.css` (canales RGB) + `tailwind.config.ts`.
Incluye tokens para superficies, texto, estados de negocio (ventas, mermas,
stock bajo, en tránsito, riesgo…) y gráficos, con variantes **claro/oscuro**.

## Manejo monetario

Todo cálculo usa **Decimal.js** (`src/utils/money.ts`) — nunca floats nativos.
El formato se centraliza en `src/utils/format.ts`: `formatCurrency`,
`formatAccountingCurrency` (negativos entre paréntesis), `formatCompactCurrency`,
`formatPercentage`, `formatDate`, etc. Cifras con números tabulares y alineadas
a la derecha.

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

## Mocks → API real

1. Poner `VITE_ENABLE_MOCKS=false` y `VITE_API_BASE_URL` a la URL real.
2. Implementar en el backend los endpoints de `src/mocks/handlers.ts`.
3. Los servicios y stores no cambian: consumen la misma capa HTTP.

## Decisiones técnicas

- Vite 5 + vue-tsc por compatibilidad estable del ecosistema.
- ECharts con importaciones específicas (`src/plugins/echarts.ts`) para tree-shaking.
- Sparklines en SVG propio para no cargar ECharts en cada tarjeta KPI.
- Tokens en canales RGB para permitir opacidad Tailwind (`rgb(var(--x) / <alpha>)`).

## Limitaciones conocidas (al arrancar la plantilla)

- Datos y escritura simulados (MSW) hasta conectar el backend real.
- Algunos módulos secundarios (compras avanzadas, trazabilidad de lotes,
  proyecciones de demanda) se entregan como **vistas de acceso** integradas
  a la capa de datos, listas para su desarrollo detallado.
- La exportación a PDF es una simulación con jsPDF hasta integrarse con el
  backend real.

## Mejoras futuras

- Vistas detalladas completas de inventario y punto de venta.
- Paginación/orden/filtrado 100 % del lado del servidor y virtualización de tablas.
- Flujos de aprobación multinivel (compras) y adjuntos reales.
- Cobertura de pruebas E2E por módulo.
</content>
