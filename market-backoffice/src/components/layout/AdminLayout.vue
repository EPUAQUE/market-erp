<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { useUserStore } from '@/stores/user.store'
import { usePermissionsStore } from '@/stores/permissions.store'
import { useThemeStore } from '@/stores/theme.store'
import NavIcon from '@/components/common/NavIcon.vue'

interface NavItem {
  label: string
  path: string
  permission: string
  icon: string
}

interface NavGroup {
  label: string
  items: NavItem[]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const userStore = useUserStore()
const permissions = usePermissionsStore()
const theme = useThemeStore()

const navGroups: NavGroup[] = [
  {
    label: 'Catálogo',
    items: [
      { label: 'Categorías', path: '/categorias', permission: 'CATEGORIAS_VER', icon: 'categorias' },
      { label: 'Marcas', path: '/marcas', permission: 'MARCAS_VER', icon: 'marcas' },
      {
        label: 'Unidades de Medida',
        path: '/unidades-medida',
        permission: 'UNIDADES_MEDIDA_VER',
        icon: 'unidades-medida',
      },
      { label: 'Productos', path: '/productos', permission: 'PRODUCTOS_VER', icon: 'productos' },
    ],
  },
  {
    label: 'Operación',
    items: [
      { label: 'Inventario', path: '/inventario', permission: 'INVENTARIO_VER', icon: 'inventario' },
      { label: 'Proveedores', path: '/proveedores', permission: 'PROVEEDORES_VER', icon: 'proveedores' },
      { label: 'Compras', path: '/compras', permission: 'COMPRAS_VER', icon: 'compras' },
      {
        label: 'Cuentas por Pagar',
        path: '/cuentas-por-pagar',
        permission: 'CUENTAS_POR_PAGAR_VER',
        icon: 'cuentas-por-pagar',
      },
      { label: 'Traslados', path: '/traslados', permission: 'TRASLADOS_VER', icon: 'traslados' },
    ],
  },
  {
    label: 'Ventas',
    items: [
      { label: 'Clientes', path: '/clientes', permission: 'CLIENTES_VER', icon: 'clientes' },
      { label: 'Ventas', path: '/ventas', permission: 'VENTAS_VER', icon: 'ventas' },
      {
        label: 'Cuentas por Cobrar',
        path: '/cuentas-por-cobrar',
        permission: 'CUENTAS_POR_COBRAR_VER',
        icon: 'cuentas-por-cobrar',
      },
      { label: 'Caja', path: '/caja', permission: 'CAJA_VER', icon: 'caja' },
      { label: 'Facturación Electrónica', path: '/fel', permission: 'FEL_VER', icon: 'fel' },
    ],
  },
  {
    label: 'Administración',
    items: [
      { label: 'Tiendas', path: '/tiendas', permission: 'TIENDAS_VER', icon: 'tiendas' },
      {
        label: 'Grupos de tiendas',
        path: '/grupos-tienda',
        permission: 'GRUPOS_TIENDA_VER',
        icon: 'grupos-tienda',
      },
      {
        label: 'Gastos Programados',
        path: '/gastos-programados',
        permission: 'GASTOS_PROGRAMADOS_VER',
        icon: 'gastos-programados',
      },
      {
        label: 'Notificaciones',
        path: '/notificaciones',
        permission: 'NOTIFICACIONES_VER',
        icon: 'notificaciones',
      },
      { label: 'Reportes', path: '/reportes', permission: 'REPORTES_VER', icon: 'reportes' },
      { label: 'Usuarios', path: '/usuarios', permission: 'USUARIOS_VER', icon: 'usuarios' },
    ],
  },
]

const visibleGroups = computed(() =>
  navGroups
    .map((group) => ({ ...group, items: group.items.filter((item) => permissions.can(item.permission)) }))
    .filter((group) => group.items.length > 0),
)

const quickActions = [
  { label: 'Crear Venta', path: '/ventas', permission: 'VENTAS_VER' },
  { label: 'Registrar Compra', path: '/compras', permission: 'COMPRAS_VER' },
  { label: 'Cobrar Cliente', path: '/cuentas-por-cobrar', permission: 'CUENTAS_POR_COBRAR_VER' },
  { label: 'Pagar Proveedor', path: '/cuentas-por-pagar', permission: 'CUENTAS_POR_PAGAR_VER' },
  { label: 'Trasladar Inventario', path: '/traslados', permission: 'TRASLADOS_VER' },
]

const visibleQuickActions = computed(() => quickActions.filter((a) => permissions.can(a.permission)))

const tiendaLabel = computed(() => {
  if (permissions.alcanceGlobal) return 'Todas las tiendas'
  const n = permissions.tiendaIds.size
  return n === 1 ? '1 tienda asignada' : `${n} tiendas asignadas`
})

const moduleTitle = computed(() => (route.meta.title as string | undefined) ?? 'Inven365')

const iniciales = computed(() => (userStore.username ?? '??').slice(0, 2).toUpperCase())

const searchOpen = ref(false)
const searchQuery = ref('')

const allNavItems = computed(() =>
  navGroups.flatMap((g) => g.items).filter((item) => permissions.can(item.permission)),
)

const searchResults = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return []
  return allNavItems.value.filter((item) => item.label.toLowerCase().includes(q)).slice(0, 8)
})

function goToResult(item: NavItem) {
  router.push(item.path)
  searchQuery.value = ''
  searchOpen.value = false
}

function onSearchBlur() {
  window.setTimeout(() => {
    searchOpen.value = false
  }, 150)
}

async function onLogout() {
  // authStore.logout() relanza si el POST a /auth/logout falla (ver
  // auth.store.spec.ts) — el estado local ya quedó limpio en ese caso, así que
  // igual hay que redirigir a /login en vez de dejar al usuario varado.
  try {
    await authStore.logout()
  } finally {
    router.push({ name: 'login' })
  }
}
</script>

<template>
  <div class="flex min-h-screen bg-mk-bg text-mk-text">
    <aside class="mk-sidebar flex w-64 shrink-0 flex-col text-mk-brand-ink">
      <div class="flex items-center gap-2 px-5 py-5">
        <div
          class="flex h-9 items-center justify-center rounded-md bg-white/10 px-2 text-xs font-bold tracking-wide"
        >
          i365
        </div>
        <div class="leading-tight">
          <p class="text-sm font-bold tracking-wide">Inven365</p>
          <p class="text-[11px] text-white/60">ERP Retail Multi-Tienda</p>
        </div>
      </div>

      <div class="mx-4 mb-4 rounded-md bg-white/5 px-3 py-2">
        <p class="text-[10px] font-semibold uppercase tracking-wider text-white/50">Sucursal</p>
        <p class="truncate text-sm font-medium">{{ tiendaLabel }}</p>
      </div>

      <nav class="flex-1 space-y-5 overflow-y-auto px-3 pb-4">
        <div v-for="group in visibleGroups" :key="group.label">
          <p class="px-2 pb-1.5 text-[11px] font-semibold uppercase tracking-wider text-white/45">
            {{ group.label }}
          </p>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="group flex items-center gap-2.5 rounded-md px-2.5 py-2 text-sm font-medium text-white/75 shadow-none transition-colors hover:bg-white/10 hover:text-white"
            active-class="!bg-mk-primary !text-white !shadow-[0_4px_12px_-4px_rgba(46,139,87,0.7)]"
          >
            <NavIcon
              :name="item.icon"
              class="h-4 w-4 shrink-0 opacity-80 group-[.router-link-active]:opacity-100"
            />
            {{ item.label }}
          </RouterLink>
        </div>
      </nav>
    </aside>

    <div class="flex min-h-screen flex-1 flex-col">
      <header
        class="flex items-center justify-between gap-4 border-b border-mk-border bg-mk-surface px-6 py-3"
      >
        <div class="min-w-0">
          <h1 class="truncate text-base font-bold text-mk-text">{{ moduleTitle }}</h1>
          <p class="truncate text-xs text-mk-text-muted">Inven365 / {{ moduleTitle }}</p>
        </div>

        <div class="relative w-full max-w-sm">
          <svg
            class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-mk-text-muted"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="11" cy="11" r="7" />
            <path d="m21 21-4.3-4.3" stroke-linecap="round" />
          </svg>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Buscar un módulo…"
            class="mk-input w-full rounded-md border border-mk-border py-2 pl-9 pr-3 text-sm"
            @focus="searchOpen = true"
            @blur="onSearchBlur"
          />
          <div
            v-if="searchOpen && searchResults.length > 0"
            class="mk-card absolute z-10 mt-1 w-full overflow-hidden py-1"
          >
            <button
              v-for="item in searchResults"
              :key="item.path"
              type="button"
              class="block w-full px-3 py-1.5 text-left text-sm hover:bg-mk-surface-2"
              @click="goToResult(item)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <div v-if="visibleQuickActions.length > 0" class="hidden items-center gap-2 lg:flex">
            <RouterLink
              v-for="action in visibleQuickActions.slice(0, 2)"
              :key="action.path"
              :to="action.path"
              class="mk-btn mk-btn-primary px-3 py-1.5 text-xs"
            >
              {{ action.label }}
            </RouterLink>
          </div>

          <button
            type="button"
            class="mk-btn-ghost flex h-9 w-9 items-center justify-center rounded-md"
            :title="theme.tema === 'oscuro' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
            @click="theme.alternar()"
          >
            <svg
              v-if="theme.tema === 'oscuro'"
              class="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="12" cy="12" r="4" />
              <path
                d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"
                stroke-linecap="round"
              />
            </svg>
            <svg
              v-else
              class="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8Z"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>

          <RouterLink
            v-if="permissions.can('NOTIFICACIONES_VER')"
            to="/notificaciones"
            class="mk-btn-ghost flex h-9 w-9 items-center justify-center rounded-md"
            title="Notificaciones"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path
                d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </RouterLink>

          <div class="mx-1 h-6 w-px bg-mk-border" />

          <div
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-mk-accent text-xs font-extrabold text-mk-brand"
            :title="userStore.username ?? ''"
          >
            {{ iniciales }}
          </div>
          <span class="hidden text-sm text-mk-text-muted sm:inline">{{ userStore.username }}</span>
          <button type="button" class="mk-btn mk-btn-ghost px-3 py-1.5 text-sm" @click="onLogout">
            Salir
          </button>
        </div>
      </header>

      <main class="flex-1">
        <RouterView />
      </main>
    </div>
  </div>
</template>
