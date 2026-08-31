<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useCuentasPorPagar } from '@/composables/useCuentasPorPagar'
import { useTiendas } from '@/composables/useTiendas'
import { useProveedores } from '@/composables/useProveedores'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { CuentaPorPagar } from '@/types/cuentaPorPagar'

function estadoVisual(cuenta: CuentaPorPagar) {
  if (cuenta.estado === 'PENDIENTE' && new Date(cuenta.fechaVencimiento).getTime() < Date.now()) {
    return { variant: 'overdue' as const, label: 'Vencida' }
  }
  if (cuenta.estado === 'PENDIENTE') return { variant: 'pending' as const, label: 'Pendiente' }
  if (cuenta.estado === 'PAGADA') return { variant: 'success' as const, label: 'Pagada' }
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
  registrarPago,
  anular,
} = useCuentasPorPagar()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const { items: proveedores, cargar: cargarProveedores } = useProveedores()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)
const detalleAbiertoId = ref<number | null>(null)
const montoPago = ref('')

function nombreProveedor(proveedorId: number): string {
  return proveedores.value.find((p) => p.id === proveedorId)?.nombre ?? `#${proveedorId}`
}

function toggleDetalle(cuenta: CuentaPorPagar) {
  detalleAbiertoId.value = detalleAbiertoId.value === cuenta.id ? null : cuenta.id
  montoPago.value = ''
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

async function onPagar(cuenta: CuentaPorPagar) {
  if (tiendaId.value === null || !montoPago.value) return
  const ok = await registrarPago(tiendaId.value, cuenta, montoPago.value)
  if (ok) montoPago.value = ''
}

function onAnular(cuenta: CuentaPorPagar) {
  if (tiendaId.value !== null) anular(tiendaId.value, cuenta)
}

const cuentaEnDetalle = computed(() => items.value.find((c) => c.id === detalleAbiertoId.value) ?? null)

onMounted(async () => {
  await cargarTiendas()
  await cargarProveedores()
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Cuentas por pagar</h1>
      <p class="text-sm text-mk-text/70">Deudas con proveedores generadas al recibir una compra.</p>
    </header>

    <select
      v-model="tiendaId"
      class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
    >
      <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
    </select>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">Compra</th>
            <th class="px-4 py-2 font-medium">Proveedor</th>
            <th class="px-4 py-2 font-medium">Vencimiento</th>
            <th class="mk-num px-4 py-2 font-medium">Monto original</th>
            <th class="mk-num px-4 py-2 font-medium">Saldo pendiente</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="7" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="7" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="items.length === 0">
            <td colspan="7" class="px-4 py-6 text-center text-mk-text/60">Sin cuentas por pagar.</td>
          </tr>
          <tr v-for="cuenta in items" :key="cuenta.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">#{{ cuenta.compraId }}</td>
            <td class="px-4 py-2">{{ nombreProveedor(cuenta.proveedorId) }}</td>
            <td class="px-4 py-2">{{ new Date(cuenta.fechaVencimiento).toLocaleDateString() }}</td>
            <td class="mk-num px-4 py-2">{{ cuenta.montoOriginal }}</td>
            <td class="mk-num px-4 py-2">{{ cuenta.saldoPendiente }}</td>
            <td class="px-4 py-2">
              <EstadoBadge :variant="estadoVisual(cuenta).variant" :label="estadoVisual(cuenta).label" />
            </td>
            <td class="px-4 py-2">
              <button
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="toggleDetalle(cuenta)"
              >
                {{ detalleAbiertoId === cuenta.id ? 'Ocultar' : 'Ver pagos' }}
              </button>
              <button
                v-if="
                  cuenta.estado === 'PENDIENTE' &&
                  cuenta.pagos.length === 0 &&
                  permissions.can('CUENTAS_POR_PAGAR_ANULAR')
                "
                type="button"
                class="text-mk-danger hover:underline"
                @click="onAnular(cuenta)"
              >
                Anular
              </button>
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
      <div class="flex items-center gap-2">
        <button type="button" :disabled="pagina <= 1" class="disabled:opacity-40" @click="pagina--">
          Anterior
        </button>
        <span>Página {{ pagina }} de {{ totalPaginas }} ({{ totalElementos }} en total)</span>
        <button
          type="button"
          :disabled="pagina >= totalPaginas"
          class="disabled:opacity-40"
          @click="pagina++"
        >
          Siguiente
        </button>
      </div>
    </div>

    <div v-if="cuentaEnDetalle" class="space-y-3">
      <h2 class="text-sm font-medium">Pagos de la compra #{{ cuentaEnDetalle.compraId }}</h2>

      <form
        v-if="cuentaEnDetalle.estado === 'PENDIENTE' && permissions.can('CUENTAS_POR_PAGAR_PAGAR')"
        class="flex items-end gap-3"
        @submit.prevent="onPagar(cuentaEnDetalle)"
      >
        <div class="space-y-1">
          <label class="text-sm font-medium">Monto a abonar</label>
          <input
            v-model="montoPago"
            type="number"
            step="0.0001"
            min="0"
            required
            class="mk-input w-40 rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <button
          type="submit"
          class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        >
          Registrar pago
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
            <tr v-if="cuentaEnDetalle.pagos.length === 0">
              <td colspan="2" class="px-4 py-6 text-center text-mk-text/60">Sin pagos registrados.</td>
            </tr>
            <tr
              v-for="pago in cuentaEnDetalle.pagos"
              :key="pago.id"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">{{ new Date(pago.fecha).toLocaleString() }}</td>
              <td class="mk-num px-4 py-2">{{ pago.monto }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
