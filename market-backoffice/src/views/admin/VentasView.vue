<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useVentas } from '@/composables/useVentas'
import { useTiendas } from '@/composables/useTiendas'
import { useClientes } from '@/composables/useClientes'
import { useProductos } from '@/composables/useProductos'
import { usePermissionsStore } from '@/stores/permissions.store'
import { resolverImagenUrl } from '@/utils/imagenUrl'
import { productosService } from '@/services/productos.service'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { MetodoPago, Venta } from '@/types/venta'
import type { ProductoTienda } from '@/types/producto'
import type { EstadoBadgeVariant } from '@/components/common/EstadoBadge.vue'

const ESTADO_VARIANT: Record<string, EstadoBadgeVariant> = {
  BORRADOR: 'neutral',
  COMPLETADA: 'success',
  ANULADA: 'danger',
}
const ESTADO_LABEL: Record<string, string> = {
  BORRADOR: 'Borrador',
  COMPLETADA: 'Completada',
  ANULADA: 'Anulada',
}

// MIXTO se excluye a propósito: requiere un desglose de pagos por canal que
// esta pantalla no captura — `completar()` aquí no manda `pagos`.
const METODOS_PAGO: { valor: MetodoPago; etiqueta: string }[] = [
  { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
  { valor: 'TARJETA', etiqueta: 'Tarjeta' },
  { valor: 'TRANSFERENCIA', etiqueta: 'Transferencia' },
  { valor: 'CREDITO', etiqueta: 'Crédito' },
]

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
  completar,
  anular,
} = useVentas()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const { items: clientes, cargar: cargarClientes } = useClientes()
const { items: productos, cargar: cargarProductos } = useProductos()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)
const showForm = ref(false)

// GET /tiendas devuelve el catálogo completo sin filtrar por el caller — un usuario
// sin alcance global solo debe ver (y quedar preseleccionado en) las tiendas a las que
// está asignado, nunca las de otra sucursal. Con alcance global (administrador) se ve
// el catálogo completo, igual que antes.
const tiendasPermitidas = computed(() =>
  permissions.alcanceGlobal ? tiendas.value : tiendas.value.filter((t) => permissions.tiendaIds.has(t.id)),
)
const detalleAbiertoId = ref<number | null>(null)

const form = ref({
  clienteId: '',
  metodoPago: '' as MetodoPago | '',
  lineas: [{ productoId: '', cantidad: '', precioUnitario: '' }],
})

// Precios del producto en la tienda seleccionada (ProductoTienda.precioVenta),
// para autocompletar "Precio unitario" al elegir un producto en la línea —
// el catálogo genérico de useProductos() no trae precio, solo vive por tienda.
const productoTiendas = ref<ProductoTienda[]>([])

async function cargarProductoTiendas(id: number) {
  const pagina = await productosService.listarPorTienda(id)
  productoTiendas.value = pagina.contenido
}

function precioDeProducto(productoId: number): string | undefined {
  return productoTiendas.value.find((pt) => pt.productoId === productoId)?.precioVenta
}

function onSeleccionarProducto(index: number) {
  const productoId = Number(form.value.lineas[index].productoId)
  const precio = precioDeProducto(productoId)
  if (precio !== undefined) form.value.lineas[index].precioUnitario = precio
}

function nombreCliente(clienteId: number): string {
  return clientes.value.find((c) => c.id === clienteId)?.nombre ?? `#${clienteId}`
}

function nombreProducto(productoId: number): string {
  return productos.value.find((p) => p.id === productoId)?.nombre ?? `#${productoId}`
}

function imagenProducto(productoId: number): string | undefined {
  return resolverImagenUrl(productos.value.find((p) => p.id === productoId)?.imagenUrl)
}

function agregarLinea() {
  form.value.lineas.push({ productoId: '', cantidad: '', precioUnitario: '' })
}

function quitarLinea(index: number) {
  if (form.value.lineas.length > 1) form.value.lineas.splice(index, 1)
}

function abrirCrear() {
  const consumidorFinal = clientes.value.find((c) => c.nombre === 'Consumidor Final')
  form.value = {
    clienteId: consumidorFinal ? String(consumidorFinal.id) : '',
    metodoPago: '',
    lineas: [{ productoId: '', cantidad: '', precioUnitario: '' }],
  }
  showForm.value = true
}

watch(tiendaId, (id) => {
  detalleAbiertoId.value = null
  pagina.value = 1
  if (id !== null) {
    cargar(id)
    cargarProductoTiendas(id)
  }
})

watch(tamano, () => {
  pagina.value = 1
})
watch([pagina, tamano], () => {
  if (tiendaId.value !== null) cargar(tiendaId.value)
})

async function onSubmit() {
  if (tiendaId.value === null || form.value.metodoPago === '') return
  const ok = await crear(
    tiendaId.value,
    Number(form.value.clienteId),
    form.value.lineas.map((l) => ({
      productoId: Number(l.productoId),
      cantidad: l.cantidad,
      precioUnitario: l.precioUnitario,
    })),
    form.value.metodoPago,
  )
  if (ok) showForm.value = false
}

function toggleDetalle(venta: Venta) {
  detalleAbiertoId.value = detalleAbiertoId.value === venta.id ? null : venta.id
}

function onCompletar(venta: Venta) {
  if (tiendaId.value !== null) completar(tiendaId.value, venta)
}

function onAnular(venta: Venta) {
  if (tiendaId.value !== null) anular(tiendaId.value, venta)
}

const ventaEnDetalle = computed(() => items.value.find((v) => v.id === detalleAbiertoId.value) ?? null)

onMounted(async () => {
  await cargarTiendas()
  await Promise.all([cargarClientes(), cargarProductos()])
  if (tiendasPermitidas.value.length > 0) tiendaId.value = tiendasPermitidas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Ventas</h1>
      <p class="text-sm text-mk-text/70">
        Ventas por tienda. Completar una venta registra su salida de Inventario.
      </p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <select
        v-if="tiendasPermitidas.length > 1"
        v-model="tiendaId"
        class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
      >
        <option v-for="tienda in tiendasPermitidas" :key="tienda.id" :value="tienda.id">
          {{ tienda.nombre }}
        </option>
      </select>
      <p v-else-if="tiendasPermitidas.length === 1" class="text-sm font-medium">
        {{ tiendasPermitidas[0].nombre }}
      </p>
      <p v-else class="text-sm text-mk-danger">No tenés ninguna tienda asignada.</p>
      <button
        v-if="permissions.can('VENTAS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm ? (showForm = false) : abrirCrear()"
      >
        {{ showForm ? 'Cancelar' : 'Nueva venta' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="space-y-1">
        <label class="text-sm font-medium">Cliente</label>
        <select
          v-model="form.clienteId"
          required
          class="mk-input w-full max-w-sm rounded border border-mk-border bg-transparent px-3 py-2"
        >
          <option value="" disabled>Seleccione…</option>
          <option v-for="cliente in clientes" :key="cliente.id" :value="cliente.id">
            {{ cliente.nombre }}
          </option>
        </select>
      </div>

      <div class="space-y-1">
        <label class="text-sm font-medium">Método de pago</label>
        <select
          v-model="form.metodoPago"
          required
          class="mk-input w-full max-w-sm rounded border border-mk-border bg-transparent px-3 py-2"
        >
          <option value="" disabled>Seleccione…</option>
          <option v-for="metodo in METODOS_PAGO" :key="metodo.valor" :value="metodo.valor">
            {{ metodo.etiqueta }}
          </option>
        </select>
      </div>

      <div class="space-y-2">
        <label class="text-sm font-medium">Líneas</label>
        <div v-for="(linea, index) in form.lineas" :key="index" class="grid gap-3 sm:grid-cols-4">
          <div class="flex items-center gap-2 sm:col-span-2">
            <img
              v-if="linea.productoId && imagenProducto(Number(linea.productoId))"
              :src="imagenProducto(Number(linea.productoId))"
              alt=""
              class="h-9 w-9 shrink-0 rounded border border-mk-border object-cover"
            />
            <select
              v-model="linea.productoId"
              required
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
              @change="onSeleccionarProducto(index)"
            >
              <option value="" disabled>Producto…</option>
              <option v-for="producto in productos" :key="producto.id" :value="producto.id">
                {{ producto.nombre }}
              </option>
            </select>
          </div>
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
              v-model="linea.precioUnitario"
              type="number"
              step="0.0001"
              min="0"
              required
              placeholder="Precio unitario"
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
        </div>
        <button type="button" class="text-sm text-mk-primary hover:underline" @click="agregarLinea">
          + Agregar línea
        </button>
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
            <th class="px-4 py-2 font-medium">Cliente</th>
            <th class="px-4 py-2 font-medium">Vendedor</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="mk-num px-4 py-2 font-medium">Total</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="6" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="6" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="items.length === 0">
            <td colspan="6" class="px-4 py-6 text-center text-mk-text/60">Sin ventas registradas.</td>
          </tr>
          <tr v-for="venta in items" :key="venta.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ new Date(venta.fecha).toLocaleString() }}</td>
            <td class="px-4 py-2">{{ nombreCliente(venta.clienteId) }}</td>
            <td class="px-4 py-2">#{{ venta.vendedorId }}</td>
            <td class="px-4 py-2">
              <EstadoBadge :variant="ESTADO_VARIANT[venta.estado]" :label="ESTADO_LABEL[venta.estado]" />
            </td>
            <td class="mk-num px-4 py-2">{{ venta.total }}</td>
            <td class="px-4 py-2">
              <button
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="toggleDetalle(venta)"
              >
                {{ detalleAbiertoId === venta.id ? 'Ocultar' : 'Ver líneas' }}
              </button>
              <button
                v-if="venta.estado === 'BORRADOR' && permissions.can('VENTAS_COMPLETAR')"
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="onCompletar(venta)"
              >
                Completar
              </button>
              <button
                v-if="venta.estado === 'BORRADOR' && permissions.can('VENTAS_ANULAR')"
                type="button"
                class="text-mk-danger hover:underline"
                @click="onAnular(venta)"
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

    <div v-if="ventaEnDetalle" class="space-y-2">
      <h2 class="text-sm font-medium">Líneas de la venta #{{ ventaEnDetalle.id }}</h2>
      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium"></th>
              <th class="px-4 py-2 font-medium">Producto</th>
              <th class="mk-num px-4 py-2 font-medium">Cantidad</th>
              <th class="mk-num px-4 py-2 font-medium">Precio unitario</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="linea in ventaEnDetalle.lineas"
              :key="linea.id"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">
                <img
                  v-if="imagenProducto(linea.productoId)"
                  :src="imagenProducto(linea.productoId)"
                  alt=""
                  class="h-8 w-8 rounded border border-mk-border object-cover"
                />
              </td>
              <td class="px-4 py-2">{{ nombreProducto(linea.productoId) }}</td>
              <td class="mk-num px-4 py-2">{{ linea.cantidad }}</td>
              <td class="mk-num px-4 py-2">{{ linea.precioUnitario }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
