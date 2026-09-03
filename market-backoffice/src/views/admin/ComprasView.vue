<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useCompras } from '@/composables/useCompras'
import { useTiendas } from '@/composables/useTiendas'
import { useProveedores } from '@/composables/useProveedores'
import { useProductos } from '@/composables/useProductos'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import { formatCurrency, calcularSubtotal } from '@/utils/money'
import { formatFechaHora } from '@/utils/fecha'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import ModalDialog from '@/components/common/ModalDialog.vue'
import ActionIcon from '@/components/common/ActionIcon.vue'
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

// Nota: con paginación del servidor, este filtro solo busca dentro de la
// página cargada, no en todo el listado (ver CLAUDE.md, "Server-side
// pagination").
const COLUMNAS_FILTRO_COMPRAS: FiltroColumna<Compra>[] = [
  { clave: 'proveedor', tipo: 'texto', valor: (c) => nombreProveedor(c.proveedorId) },
  {
    clave: 'estado',
    tipo: 'opciones',
    valor: (c) => c.estado,
    opciones: [
      { valor: 'BORRADOR', etiqueta: 'Borrador' },
      { valor: 'RECIBIDA', etiqueta: 'Recibida' },
      { valor: 'ANULADA', etiqueta: 'Anulada' },
    ],
  },
]
const {
  busquedaGlobal: busquedaCompras,
  filtrosColumna: filtrosCompras,
  itemsFiltrados: comprasFiltradas,
  limpiarFiltros: limpiarFiltrosCompras,
  hayFiltrosActivos: hayFiltrosComprasActivos,
} = useFiltrosTabla(items, COLUMNAS_FILTRO_COMPRAS)

const form = ref({
  proveedorId: '',
  lineas: [{ productoId: '', cantidad: '', costoUnitario: '', subtotal: '' }],
})

const totalFormulario = computed(() =>
  form.value.lineas.reduce((acc, l) => acc + calcularSubtotal(l.cantidad, l.costoUnitario), 0),
)

type LineaCompraForm = (typeof form.value.lineas)[number]

// Cantidad/costo unitario y subtotal son dos formas de capturar lo mismo — el
// costo unitario es el único que se manda al backend (LineaCompraRequest no tiene
// subtotal, lo recalcula server-side), así que editar el subtotal solo sirve para
// despejar el costo unitario a partir de él, nunca al revés en simultáneo.
function onCantidadOCostoUnitarioInput(linea: LineaCompraForm) {
  if (linea.cantidad === '' || linea.costoUnitario === '') return
  linea.subtotal = calcularSubtotal(linea.cantidad, linea.costoUnitario).toFixed(2)
}

// Solo recalcula costoUnitario mientras se escribe — nunca reescribe linea.subtotal
// acá (es el mismo campo donde el usuario está tecleando; sobreescribirlo en cada
// tecla pelea con la escritura, ej. tipear "10" queda cortado en "1" a medio camino).
function onSubtotalInput(linea: LineaCompraForm) {
  const cantidad = Number(linea.cantidad)
  if (linea.cantidad === '' || cantidad <= 0 || linea.subtotal === '') return
  linea.costoUnitario = (Number(linea.subtotal) / cantidad).toFixed(2)
}

// Al salir del campo Subtotal, lo redondea a lo que realmente va a resultar de
// cantidad × costoUnitario (ya redondeado a 2 decimales) — para que lo que se ve
// coincida siempre con lo que el backend calcula, sin pelear con la escritura.
function onSubtotalBlur(linea: LineaCompraForm) {
  if (linea.cantidad === '' || linea.costoUnitario === '') return
  linea.subtotal = calcularSubtotal(linea.cantidad, linea.costoUnitario).toFixed(2)
}

function nombreProveedor(proveedorId: number): string {
  return proveedores.value.find((p) => p.id === proveedorId)?.nombre ?? `#${proveedorId}`
}

function nombreProducto(productoId: number): string {
  return productos.value.find((p) => p.id === productoId)?.nombre ?? `#${productoId}`
}

function agregarLinea() {
  form.value.lineas.push({ productoId: '', cantidad: '', costoUnitario: '', subtotal: '' })
}

function quitarLinea(index: number) {
  if (form.value.lineas.length > 1) form.value.lineas.splice(index, 1)
}

function abrirCrear() {
  form.value = {
    proveedorId: '',
    lineas: [{ productoId: '', cantidad: '', costoUnitario: '', subtotal: '' }],
  }
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
        @click="abrirCrear()"
      >
        Nueva compra
      </button>
    </div>

    <ModalDialog v-model="showForm" title="Nueva compra" max-width="max-w-3xl">
      <form class="space-y-3" @submit.prevent="onSubmit">
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
          <div
            v-for="(linea, index) in form.lineas"
            :key="index"
            class="grid items-center gap-3 sm:grid-cols-12"
          >
            <select
              v-model="linea.productoId"
              required
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 sm:col-span-4"
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
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 sm:col-span-2"
              @input="onCantidadOCostoUnitarioInput(linea)"
            />
            <input
              v-model="linea.costoUnitario"
              type="number"
              step="0.01"
              min="0"
              required
              placeholder="Costo unitario"
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 sm:col-span-2"
              @input="onCantidadOCostoUnitarioInput(linea)"
            />
            <input
              v-model="linea.subtotal"
              type="number"
              step="0.01"
              min="0"
              placeholder="Subtotal"
              class="mk-input mk-num w-full rounded border border-mk-border bg-transparent px-3 py-2 sm:col-span-2"
              @input="onSubtotalInput(linea)"
              @blur="onSubtotalBlur(linea)"
            />
            <button
              type="button"
              class="text-mk-danger disabled:opacity-40 sm:col-span-2"
              :disabled="form.lineas.length <= 1"
              @click="quitarLinea(index)"
            >
              Quitar
            </button>
          </div>
          <button type="button" class="text-sm text-mk-primary hover:underline" @click="agregarLinea">
            + Agregar línea
          </button>
          <p class="mk-num text-sm font-semibold">Total: {{ formatCurrency(totalFormulario) }}</p>
        </div>

        <p v-if="saveError" class="text-sm text-mk-danger" role="alert">{{ saveError }}</p>
        <div class="flex justify-end gap-2">
          <button
            type="button"
            class="mk-btn mk-btn-ghost rounded px-4 py-2 text-sm"
            @click="showForm = false"
          >
            Cancelar
          </button>
          <button
            type="submit"
            :disabled="saveLoading"
            class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
          >
            {{ saveLoading ? 'Guardando…' : 'Crear' }}
          </button>
        </div>
      </form>
    </ModalDialog>

    <div class="flex items-center gap-2">
      <input
        v-model="busquedaCompras"
        type="search"
        placeholder="Buscar en todas las columnas…"
        class="mk-input w-full max-w-xs rounded border border-mk-border bg-transparent px-3 py-2"
      />
      <button
        v-if="hayFiltrosComprasActivos"
        type="button"
        class="text-sm text-mk-text/60 hover:underline"
        @click="limpiarFiltrosCompras"
      >
        Limpiar filtros
      </button>
    </div>

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
          <tr class="border-b border-mk-border bg-mk-surface/50">
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosCompras.proveedor"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosCompras.estado"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todos</option>
                <option value="BORRADOR">Borrador</option>
                <option value="RECIBIDA">Recibida</option>
                <option value="ANULADA">Anulada</option>
              </select>
            </th>
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="5" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="5" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="comprasFiltradas.length === 0">
            <td colspan="5" class="px-4 py-6 text-center text-mk-text/60">Sin compras registradas.</td>
          </tr>
          <tr
            v-for="compra in comprasFiltradas"
            :key="compra.id"
            class="border-b border-mk-border last:border-0"
          >
            <td class="px-4 py-2">{{ formatFechaHora(compra.fecha) }}</td>
            <td class="px-4 py-2">{{ nombreProveedor(compra.proveedorId) }}</td>
            <td class="px-4 py-2">
              <EstadoBadge :variant="ESTADO_VARIANT[compra.estado]" :label="ESTADO_LABEL[compra.estado]" />
            </td>
            <td class="mk-num px-4 py-2">{{ formatCurrency(compra.total) }}</td>
            <td class="px-4 py-2">
              <div class="mk-row-actions">
                <button
                  type="button"
                  class="mk-row-btn mk-row-btn-neutral"
                  :title="detalleAbiertoId === compra.id ? 'Ocultar' : 'Ver líneas'"
                  @click="toggleDetalle(compra)"
                >
                  <ActionIcon name="eye" />
                </button>
                <button
                  v-if="compra.estado === 'BORRADOR' && permissions.can('COMPRAS_RECIBIR')"
                  type="button"
                  class="mk-row-btn mk-row-btn-success"
                  title="Recibir"
                  @click="onRecibir(compra)"
                >
                  <ActionIcon name="check" />
                </button>
                <button
                  v-if="compra.estado === 'BORRADOR' && permissions.can('COMPRAS_ANULAR')"
                  type="button"
                  class="mk-row-btn mk-row-btn-danger"
                  title="Anular"
                  @click="onAnular(compra)"
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
