<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useCompras } from '@/composables/useCompras'
import { useTiendas } from '@/composables/useTiendas'
import { useProveedores } from '@/composables/useProveedores'
import { useProductos } from '@/composables/useProductos'
import { usePermissionsStore } from '@/stores/permissions.store'
import { formatCurrency, calcularSubtotal } from '@/utils/money'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { Compra } from '@/types/compra'
import type { EstadoBadgeVariant } from '@/components/common/EstadoBadge.vue'

const ESTADO_VARIANT: Record<string, EstadoBadgeVariant> = {
  BORRADOR: 'neutral',
  RECIBIDA: 'success',
  ANULADA: 'danger',
}
const ESTADO_LABEL: Record<string, string> = {
  BORRADOR: 'Borrador',
  RECIBIDA: 'Recibida',
  ANULADA: 'Anulada',
}

const {
  items,
  listLoading,
  listError,
  saveLoading,
  saveError,
  pagina,
  tamano,
  totalElementos,
  totalPaginas,
  cargar,
  crear,
  recibir,
  anular,
} = useCompras()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const { items: proveedores, cargar: cargarProveedores } = useProveedores()
const { items: productos, cargar: cargarProductos } = useProductos()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)
const showForm = ref(false)
const detalleAbiertoId = ref<number | null>(null)

const form = ref({
  proveedorId: '',
  lineas: [{ productoId: '', cantidad: '', costoUnitario: '' }],
})

const totalFormulario = computed(() =>
  form.value.lineas.reduce((acc, l) => acc + calcularSubtotal(l.cantidad, l.costoUnitario), 0),
)

function nombreProveedor(proveedorId: number): string {
  return proveedores.value.find((p) => p.id === proveedorId)?.nombre ?? `#${proveedorId}`
}

function nombreProducto(productoId: number): string {
  return productos.value.find((p) => p.id === productoId)?.nombre ?? `#${productoId}`
}

function agregarLinea() {
  form.value.lineas.push({ productoId: '', cantidad: '', costoUnitario: '' })
}

function quitarLinea(index: number) {
  if (form.value.lineas.length > 1) form.value.lineas.splice(index, 1)
}

function abrirCrear() {
  form.value = { proveedorId: '', lineas: [{ productoId: '', cantidad: '', costoUnitario: '' }] }
  showForm.value = true
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

async function onSubmit() {
  if (tiendaId.value === null) return
  const ok = await crear(
    tiendaId.value,
    Number(form.value.proveedorId),
    form.value.lineas.map((l) => ({
      productoId: Number(l.productoId),
      cantidad: l.cantidad,
      costoUnitario: l.costoUnitario,
    })),
  )
  if (ok) showForm.value = false
}

function toggleDetalle(compra: Compra) {
  detalleAbiertoId.value = detalleAbiertoId.value === compra.id ? null : compra.id
}

function onRecibir(compra: Compra) {
  if (tiendaId.value !== null) recibir(tiendaId.value, compra)
}

function onAnular(compra: Compra) {
  if (tiendaId.value !== null) anular(tiendaId.value, compra)
}

const compraEnDetalle = computed(() => items.value.find((c) => c.id === detalleAbiertoId.value) ?? null)

onMounted(async () => {
  await cargarTiendas()
  await Promise.all([cargarProveedores(), cargarProductos()])
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Compras</h1>
      <p class="text-sm text-mk-text/70">
        Órdenes de compra por tienda. Recibir una compra registra su ingreso en Inventario.
      </p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <select
        v-model="tiendaId"
        class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
      >
        <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
      </select>
      <button
        v-if="permissions.can('COMPRAS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm ? (showForm = false) : abrirCrear()"
      >
        {{ showForm ? 'Cancelar' : 'Nueva compra' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="space-y-1">
        <label class="text-sm font-medium">Proveedor</label>
        <select
          v-model="form.proveedorId"
          required
          class="mk-input w-full max-w-sm rounded border border-mk-border bg-transparent px-3 py-2"
        >
          <option value="" disabled>Seleccione…</option>
          <option v-for="proveedor in proveedores" :key="proveedor.id" :value="proveedor.id">
            {{ proveedor.nombre }}
          </option>
        </select>
      </div>

      <div class="space-y-2">
        <label class="text-sm font-medium">Líneas</label>
        <div v-for="(linea, index) in form.lineas" :key="index" class="grid gap-3 sm:grid-cols-4">
          <select
            v-model="linea.productoId"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 sm:col-span-2"
          >
            <option value="" disabled>Producto…</option>
            <option v-for="producto in productos" :key="producto.id" :value="producto.id">
              {{ producto.nombre }}
            </option>
          </select>
          <input
            v-model="linea.cantidad"
            type="number"
            step="1"
            min="1"
            required
            placeholder="Cantidad"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
          <div class="flex gap-2">
            <input
              v-model="linea.costoUnitario"
              type="number"
              step="0.01"
              min="0"
              required
              placeholder="Costo unitario"
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
            />
            <button
              type="button"
              class="text-mk-danger disabled:opacity-40"
              :disabled="form.lineas.length <= 1"
              @click="quitarLinea(index)"
            >
              Quitar
            </button>
          </div>
          <p class="mk-num text-sm text-mk-text/70 sm:col-start-4">
            Subtotal: {{ formatCurrency(calcularSubtotal(linea.cantidad, linea.costoUnitario)) }}
          </p>
        </div>
        <button type="button" class="text-sm text-mk-primary hover:underline" @click="agregarLinea">
          + Agregar línea
        </button>
        <p class="mk-num text-sm font-semibold">Total: {{ formatCurrency(totalFormulario) }}</p>
      </div>

      <p v-if="saveError" class="text-sm text-mk-danger" role="alert">{{ saveError }}</p>
      <button
        type="submit"
        :disabled="saveLoading"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {{ saveLoading ? 'Guardando…' : 'Crear' }}
      </button>
    </form>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">Fecha</th>
            <th class="px-4 py-2 font-medium">Proveedor</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="mk-num px-4 py-2 font-medium">Total</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="5" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="5" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="items.length === 0">
            <td colspan="5" class="px-4 py-6 text-center text-mk-text/60">Sin compras registradas.</td>
          </tr>
          <tr v-for="compra in items" :key="compra.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ new Date(compra.fecha).toLocaleString() }}</td>
            <td class="px-4 py-2">{{ nombreProveedor(compra.proveedorId) }}</td>
            <td class="px-4 py-2">
              <EstadoBadge :variant="ESTADO_VARIANT[compra.estado]" :label="ESTADO_LABEL[compra.estado]" />
            </td>
            <td class="mk-num px-4 py-2">{{ formatCurrency(compra.total) }}</td>
            <td class="px-4 py-2">
              <button
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="toggleDetalle(compra)"
              >
                {{ detalleAbiertoId === compra.id ? 'Ocultar' : 'Ver líneas' }}
              </button>
              <button
                v-if="compra.estado === 'BORRADOR' && permissions.can('COMPRAS_RECIBIR')"
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="onRecibir(compra)"
              >
                Recibir
              </button>
              <button
                v-if="compra.estado === 'BORRADOR' && permissions.can('COMPRAS_ANULAR')"
                type="button"
                class="text-mk-danger hover:underline"
                @click="onAnular(compra)"
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

    <div v-if="compraEnDetalle" class="space-y-2">
      <h2 class="text-sm font-medium">Líneas de la compra #{{ compraEnDetalle.id }}</h2>
      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Producto</th>
              <th class="mk-num px-4 py-2 font-medium">Cantidad</th>
              <th class="mk-num px-4 py-2 font-medium">Costo unitario</th>
              <th class="mk-num px-4 py-2 font-medium">Subtotal</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="linea in compraEnDetalle.lineas"
              :key="linea.id"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">{{ nombreProducto(linea.productoId) }}</td>
              <td class="mk-num px-4 py-2">{{ linea.cantidad }}</td>
              <td class="mk-num px-4 py-2">{{ formatCurrency(linea.costoUnitario) }}</td>
              <td class="mk-num px-4 py-2">
                {{ formatCurrency(calcularSubtotal(linea.cantidad, linea.costoUnitario)) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
