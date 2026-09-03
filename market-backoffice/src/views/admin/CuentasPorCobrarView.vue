<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useCuentasPorCobrar } from '@/composables/useCuentasPorCobrar'
import { useTiendas } from '@/composables/useTiendas'
import { useClientes } from '@/composables/useClientes'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import { formatCurrency } from '@/utils/money'
import { formatFecha, formatFechaHora } from '@/utils/fecha'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import ActionIcon from '@/components/common/ActionIcon.vue'
import PaginacionTabla from '@/components/common/PaginacionTabla.vue'
import type { CuentaPorCobrar } from '@/types/cuentaPorCobrar'

function estadoVisual(cuenta: CuentaPorCobrar) {
  if (cuenta.estado === 'PENDIENTE' && new Date(cuenta.fechaVencimiento).getTime() < Date.now()) {
    return { variant: 'overdue' as const, label: 'Vencida' }
  }
  if (cuenta.estado === 'PENDIENTE') return { variant: 'pending' as const, label: 'Pendiente' }
  if (cuenta.estado === 'COBRADA') return { variant: 'success' as const, label: 'Cobrada' }
  return { variant: 'neutral' as const, label: 'Anulada' }
}

const {
  items,
  listLoading,
  listError,
  pagina,
  tamano,
  totalElementos,
  totalPaginas,
  cargar,
  registrarCobro,
  anular,
} = useCuentasPorCobrar()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const { items: clientes, cargar: cargarClientes } = useClientes()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)
const detalleAbiertoId = ref<number | null>(null)
const montoCobro = ref('')

function nombreCliente(clienteId: number): string {
  return clientes.value.find((c) => c.id === clienteId)?.nombre ?? `#${clienteId}`
}

// Nota: con paginación del servidor, este filtro solo busca dentro de la
// página cargada, no en todo el listado (ver CLAUDE.md, "Server-side
// pagination").
const COLUMNAS_FILTRO: FiltroColumna<CuentaPorCobrar>[] = [
  { clave: 'venta', tipo: 'texto', valor: (c) => `#${c.ventaId}` },
  { clave: 'cliente', tipo: 'texto', valor: (c) => nombreCliente(c.clienteId) },
  {
    clave: 'estado',
    tipo: 'opciones',
    valor: (c) => c.estado,
    opciones: [
      { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
      { valor: 'COBRADA', etiqueta: 'Cobrada' },
      { valor: 'ANULADA', etiqueta: 'Anulada' },
    ],
  },
]
const {
  busquedaGlobal,
  filtrosColumna,
  itemsFiltrados: cuentasFiltradas,
  limpiarFiltros,
  hayFiltrosActivos,
} = useFiltrosTabla(items, COLUMNAS_FILTRO)

function toggleDetalle(cuenta: CuentaPorCobrar) {
  detalleAbiertoId.value = detalleAbiertoId.value === cuenta.id ? null : cuenta.id
  montoCobro.value = ''
}

watch(tiendaId, (id) => {
  detalleAbiertoId.value = null
  pagina.value = 1
  if (id !== null) cargar(id)
})

watch(tamano, () => {
  pagina.value = 1
})
watch([pagina, tamano], () => {
  if (tiendaId.value !== null) cargar(tiendaId.value)
})

async function onCobrar(cuenta: CuentaPorCobrar) {
  if (tiendaId.value === null || !montoCobro.value) return
  const ok = await registrarCobro(tiendaId.value, cuenta, montoCobro.value)
  if (ok) montoCobro.value = ''
}

function onAnular(cuenta: CuentaPorCobrar) {
  if (tiendaId.value !== null) anular(tiendaId.value, cuenta)
}

const cuentaEnDetalle = computed(() => items.value.find((c) => c.id === detalleAbiertoId.value) ?? null)

onMounted(async () => {
  await cargarTiendas()
  await cargarClientes()
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Cuentas por cobrar</h1>
      <p class="text-sm text-mk-text/70">Deudas de clientes generadas al completar una venta.</p>
    </header>

    <select
      v-model="tiendaId"
      class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
    >
      <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
    </select>

    <div class="flex items-center gap-2">
      <input
        v-model="busquedaGlobal"
        type="search"
        placeholder="Buscar en todas las columnas…"
        class="mk-input w-full max-w-xs rounded border border-mk-border bg-transparent px-3 py-2"
      />
      <button
        v-if="hayFiltrosActivos"
        type="button"
        class="text-sm text-mk-text/60 hover:underline"
        @click="limpiarFiltros"
      >
        Limpiar filtros
      </button>
    </div>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">Venta</th>
            <th class="px-4 py-2 font-medium">Cliente</th>
            <th class="px-4 py-2 font-medium">Vencimiento</th>
            <th class="mk-num px-4 py-2 font-medium">Monto original</th>
            <th class="mk-num px-4 py-2 font-medium">Saldo pendiente</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
          <tr class="border-b border-mk-border bg-mk-surface/50">
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.venta"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.cliente"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosColumna.estado"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todos</option>
                <option value="PENDIENTE">Pendiente</option>
                <option value="COBRADA">Cobrada</option>
                <option value="ANULADA">Anulada</option>
              </select>
            </th>
            <th class="px-4 py-1.5"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="7" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="7" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="cuentasFiltradas.length === 0">
            <td colspan="7" class="px-4 py-6 text-center text-mk-text/60">Sin cuentas por cobrar.</td>
          </tr>
          <tr
            v-for="cuenta in cuentasFiltradas"
            :key="cuenta.id"
            class="border-b border-mk-border last:border-0"
          >
            <td class="px-4 py-2">#{{ cuenta.ventaId }}</td>
            <td class="px-4 py-2">{{ nombreCliente(cuenta.clienteId) }}</td>
            <td class="px-4 py-2">{{ formatFecha(cuenta.fechaVencimiento) }}</td>
            <td class="mk-num px-4 py-2">{{ formatCurrency(cuenta.montoOriginal) }}</td>
            <td class="mk-num px-4 py-2">{{ formatCurrency(cuenta.saldoPendiente) }}</td>
            <td class="px-4 py-2">
              <EstadoBadge :variant="estadoVisual(cuenta).variant" :label="estadoVisual(cuenta).label" />
            </td>
            <td class="px-4 py-2">
              <div class="mk-row-actions">
                <button
                  type="button"
                  class="mk-row-btn mk-row-btn-neutral"
                  :title="detalleAbiertoId === cuenta.id ? 'Ocultar' : 'Ver cobros'"
                  @click="toggleDetalle(cuenta)"
                >
                  <ActionIcon name="eye" />
                </button>
                <button
                  v-if="
                    cuenta.estado === 'PENDIENTE' &&
                    cuenta.cobros.length === 0 &&
                    permissions.can('CUENTAS_POR_COBRAR_ANULAR')
                  "
                  type="button"
                  class="mk-row-btn mk-row-btn-danger"
                  title="Anular"
                  @click="onAnular(cuenta)"
                >
                  <ActionIcon name="x" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="flex items-center justify-between text-sm text-mk-text/70">
      <select v-model.number="tamano" class="rounded border border-mk-border bg-transparent px-2 py-1">
        <option :value="10">10 / página</option>
        <option :value="25">25 / página</option>
        <option :value="50">50 / página</option>
        <option :value="100">100 / página</option>
      </select>
      <div class="flex items-center gap-3">
        <span>{{ totalElementos }} en total</span>
        <PaginacionTabla v-model:pagina="pagina" :total-paginas="totalPaginas" />
      </div>
    </div>

    <div v-if="cuentaEnDetalle" class="space-y-3">
      <h2 class="text-sm font-medium">Cobros de la venta #{{ cuentaEnDetalle.ventaId }}</h2>

      <form
        v-if="cuentaEnDetalle.estado === 'PENDIENTE' && permissions.can('CUENTAS_POR_COBRAR_COBRAR')"
        class="flex items-end gap-3"
        @submit.prevent="onCobrar(cuentaEnDetalle)"
      >
        <div class="space-y-1">
          <label class="text-sm font-medium">Monto a cobrar</label>
          <input
            v-model="montoCobro"
            type="number"
            step="0.01"
            min="0"
            required
            class="mk-input w-40 rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <button
          type="submit"
          class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        >
          Registrar cobro
        </button>
      </form>

      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Fecha</th>
              <th class="mk-num px-4 py-2 font-medium">Monto</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="cuentaEnDetalle.cobros.length === 0">
              <td colspan="2" class="px-4 py-6 text-center text-mk-text/60">Sin cobros registrados.</td>
            </tr>
            <tr
              v-for="cobro in cuentaEnDetalle.cobros"
              :key="cobro.id"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">{{ formatFechaHora(cobro.fecha) }}</td>
              <td class="mk-num px-4 py-2">{{ formatCurrency(cobro.monto) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
