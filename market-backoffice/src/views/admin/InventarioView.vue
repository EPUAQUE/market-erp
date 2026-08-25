<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useInventario } from '@/composables/useInventario'
import { useTiendas } from '@/composables/useTiendas'
import { useProductos } from '@/composables/useProductos'
import { usePermissionsStore } from '@/stores/permissions.store'
import type { TipoMovimiento } from '@/types/inventario'

const {
  items,
  listLoading,
  listError,
  pagina,
  tamano,
  totalElementos,
  totalPaginas,
  movimientos,
  movimientosLoading,
  movimientosError,
  movimientosPagina,
  movimientosTamano,
  movimientosTotalElementos,
  movimientosTotalPaginas,
  saveLoading,
  saveError,
  cargar,
  cargarMovimientos,
  registrarMovimiento,
} = useInventario()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const { items: productos, cargar: cargarProductos } = useProductos()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)
const kardexProductoId = ref<number | null>(null)
const showForm = ref(false)
const form = ref({ productoId: '', cantidad: '', costoUnitario: '', tipoMovimiento: 'COMPRA' as TipoMovimiento })

const TIPOS_MOVIMIENTO: TipoMovimiento[] = [
  'COMPRA',
  'VENTA',
  'AJUSTE_POSITIVO',
  'AJUSTE_NEGATIVO',
  'TRASLADO_ENTRADA',
  'TRASLADO_SALIDA',
  'DEVOLUCION_CLIENTE',
  'DEVOLUCION_PROVEEDOR',
]

function nombreProducto(productoId: number): string {
  return productos.value.find((p) => p.id === productoId)?.nombre ?? `#${productoId}`
}

const filaKardex = computed(() => (kardexProductoId.value !== null ? nombreProducto(kardexProductoId.value) : ''))

watch(tiendaId, (id) => {
  kardexProductoId.value = null
  pagina.value = 1
  if (id !== null) cargar(id)
})

watch(tamano, () => {
  pagina.value = 1
})
watch([pagina, tamano], () => {
  if (tiendaId.value !== null) cargar(tiendaId.value)
})

watch(movimientosTamano, () => {
  movimientosPagina.value = 1
})
watch([movimientosPagina, movimientosTamano], () => {
  if (tiendaId.value !== null && kardexProductoId.value !== null) {
    cargarMovimientos(tiendaId.value, kardexProductoId.value)
  }
})

async function onSubmit() {
  if (tiendaId.value === null) return
  const ok = await registrarMovimiento(tiendaId.value, {
    productoId: Number(form.value.productoId),
    cantidad: form.value.cantidad,
    costoUnitario: form.value.costoUnitario,
    tipoMovimiento: form.value.tipoMovimiento,
  })
  if (ok) {
    showForm.value = false
    form.value = { productoId: '', cantidad: '', costoUnitario: '', tipoMovimiento: 'COMPRA' }
  }
}

function verKardex(productoId: number) {
  if (tiendaId.value === null) return
  kardexProductoId.value = productoId
  movimientosPagina.value = 1
  cargarMovimientos(tiendaId.value, productoId)
}

onMounted(async () => {
  await cargarTiendas()
  await cargarProductos()
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Inventario</h1>
      <p class="text-sm text-mk-text/70">Existencia y costo promedio por tienda; kardex append-only por producto.</p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <select
        v-model="tiendaId"
        class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
      >
        <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
      </select>
      <button
        v-if="permissions.can('INVENTARIO_AJUSTAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm = !showForm"
      >
        {{ showForm ? 'Cancelar' : 'Registrar movimiento' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="grid gap-3 sm:grid-cols-4">
        <div class="space-y-1">
          <label class="text-sm font-medium">Producto</label>
          <select
            v-model="form.productoId"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option value="" disabled>Seleccione…</option>
            <option v-for="producto in productos" :key="producto.id" :value="producto.id">
              {{ producto.nombre }}
            </option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Tipo</label>
          <select
            v-model="form.tipoMovimiento"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option v-for="tipo in TIPOS_MOVIMIENTO" :key="tipo" :value="tipo">{{ tipo }}</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Cantidad</label>
          <input
            v-model="form.cantidad"
            type="number"
            step="0.001"
            min="0"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Costo unitario</label>
          <input
            v-model="form.costoUnitario"
            type="number"
            step="0.0001"
            min="0"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
      </div>
      <p v-if="saveError" class="text-sm text-mk-danger" role="alert">{{ saveError }}</p>
      <button
        type="submit"
        :disabled="saveLoading"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {{ saveLoading ? 'Registrando…' : 'Registrar' }}
      </button>
    </form>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">Producto</th>
            <th class="mk-num px-4 py-2 font-medium">Existencia</th>
            <th class="mk-num px-4 py-2 font-medium">Costo promedio</th>
            <th class="px-4 py-2 font-medium">Kardex</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="4" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="items.length === 0">
            <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Sin movimientos registrados.</td>
          </tr>
          <tr v-for="inv in items" :key="inv.productoId" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ nombreProducto(inv.productoId) }}</td>
            <td class="mk-num px-4 py-2">{{ inv.existenciaActual }}</td>
            <td class="mk-num px-4 py-2">{{ inv.costoPromedioActual }}</td>
            <td class="px-4 py-2">
              <button type="button" class="text-mk-primary hover:underline" @click="verKardex(inv.productoId)">
                Ver kardex
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
        <button type="button" :disabled="pagina <= 1" class="disabled:opacity-40" @click="pagina--">Anterior</button>
        <span>Página {{ pagina }} de {{ totalPaginas }} ({{ totalElementos }} en total)</span>
        <button type="button" :disabled="pagina >= totalPaginas" class="disabled:opacity-40" @click="pagina++">
          Siguiente
        </button>
      </div>
    </div>

    <div v-if="kardexProductoId !== null" class="space-y-2">
      <h2 class="text-sm font-medium">Kardex — {{ filaKardex }}</h2>
      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Fecha</th>
              <th class="px-4 py-2 font-medium">Tipo</th>
              <th class="mk-num px-4 py-2 font-medium">Cantidad</th>
              <th class="mk-num px-4 py-2 font-medium">Costo unitario</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="movimientosLoading">
              <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
            </tr>
            <tr v-else-if="movimientosError">
              <td colspan="4" class="px-4 py-6 text-center text-mk-danger">{{ movimientosError }}</td>
            </tr>
            <tr v-else-if="movimientos.length === 0">
              <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Sin movimientos.</td>
            </tr>
            <tr v-for="mov in movimientos" :key="mov.id" class="border-b border-mk-border last:border-0">
              <td class="px-4 py-2">{{ new Date(mov.fecha).toLocaleString() }}</td>
              <td class="px-4 py-2">{{ mov.tipoMovimiento }}</td>
              <td class="mk-num px-4 py-2">{{ mov.cantidad }}</td>
              <td class="mk-num px-4 py-2">{{ mov.costoUnitario }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between text-sm text-mk-text/70">
        <select v-model.number="movimientosTamano" class="rounded border border-mk-border bg-transparent px-2 py-1">
          <option :value="10">10 / página</option>
          <option :value="25">25 / página</option>
          <option :value="50">50 / página</option>
          <option :value="100">100 / página</option>
        </select>
        <div class="flex items-center gap-2">
          <button
            type="button"
            :disabled="movimientosPagina <= 1"
            class="disabled:opacity-40"
            @click="movimientosPagina--"
          >
            Anterior
          </button>
          <span>
            Página {{ movimientosPagina }} de {{ movimientosTotalPaginas }} ({{ movimientosTotalElementos }} en total)
          </span>
          <button
            type="button"
            :disabled="movimientosPagina >= movimientosTotalPaginas"
            class="disabled:opacity-40"
            @click="movimientosPagina++"
          >
            Siguiente
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
