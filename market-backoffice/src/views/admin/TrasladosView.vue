<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useTraslados } from '@/composables/useTraslados'
import { useTiendas } from '@/composables/useTiendas'
import { useProductos } from '@/composables/useProductos'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { Traslado } from '@/types/traslado'
import type { EstadoBadgeVariant } from '@/components/common/EstadoBadge.vue'

const ESTADO_VARIANT: Record<string, EstadoBadgeVariant> = {
  BORRADOR: 'neutral',
  COMPLETADO: 'success',
  ANULADO: 'danger',
}
const ESTADO_LABEL: Record<string, string> = {
  BORRADOR: 'Borrador',
  COMPLETADO: 'Completado',
  ANULADO: 'Anulado',
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
  completar,
  anular,
} = useTraslados()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const { items: productos, cargar: cargarProductos } = useProductos()
const permissions = usePermissionsStore()

const showForm = ref(false)
const detalleAbiertoId = ref<number | null>(null)

const form = ref({
  tiendaOrigenId: '',
  tiendaDestinoId: '',
  lineas: [{ productoId: '', cantidad: '' }],
})

function nombreTienda(tiendaId: number): string {
  return tiendas.value.find((t) => t.id === tiendaId)?.nombre ?? `#${tiendaId}`
}

function nombreProducto(productoId: number): string {
  return productos.value.find((p) => p.id === productoId)?.nombre ?? `#${productoId}`
}

// Nota: con paginación del servidor, este filtro solo busca dentro de la
// página cargada, no en todo el listado (ver CLAUDE.md, "Server-side
// pagination").
const COLUMNAS_FILTRO: FiltroColumna<Traslado>[] = [
  { clave: 'origen', tipo: 'texto', valor: (t) => nombreTienda(t.tiendaOrigenId) },
  { clave: 'destino', tipo: 'texto', valor: (t) => nombreTienda(t.tiendaDestinoId) },
  {
    clave: 'estado',
    tipo: 'opciones',
    valor: (t) => t.estado,
    opciones: [
      { valor: 'BORRADOR', etiqueta: 'Borrador' },
      { valor: 'COMPLETADO', etiqueta: 'Completado' },
      { valor: 'ANULADO', etiqueta: 'Anulado' },
    ],
  },
]
const {
  busquedaGlobal,
  filtrosColumna,
  itemsFiltrados: trasladosFiltrados,
  limpiarFiltros,
  hayFiltrosActivos,
} = useFiltrosTabla(items, COLUMNAS_FILTRO)

function agregarLinea() {
  form.value.lineas.push({ productoId: '', cantidad: '' })
}

function quitarLinea(index: number) {
  if (form.value.lineas.length > 1) form.value.lineas.splice(index, 1)
}

function abrirCrear() {
  form.value = { tiendaOrigenId: '', tiendaDestinoId: '', lineas: [{ productoId: '', cantidad: '' }] }
  showForm.value = true
}

async function onSubmit() {
  const ok = await crear(
    Number(form.value.tiendaOrigenId),
    Number(form.value.tiendaDestinoId),
    form.value.lineas.map((l) => ({ productoId: Number(l.productoId), cantidad: l.cantidad })),
  )
  if (ok) showForm.value = false
}

function toggleDetalle(traslado: Traslado) {
  detalleAbiertoId.value = detalleAbiertoId.value === traslado.id ? null : traslado.id
}

const trasladoEnDetalle = computed(() => items.value.find((t) => t.id === detalleAbiertoId.value) ?? null)

watch(tamano, () => {
  pagina.value = 1
})
watch([pagina, tamano], () => {
  cargar()
})

onMounted(async () => {
  await cargarTiendas()
  await cargarProductos()
  await cargar()
})
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Traslados</h1>
      <p class="text-sm text-mk-text/70">
        Movimientos de inventario entre tiendas. Completar registra la salida en origen y la entrada en
        destino.
      </p>
    </header>

    <div class="flex items-center justify-end">
      <button
        v-if="permissions.can('TRASLADOS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm ? (showForm = false) : abrirCrear()"
      >
        {{ showForm ? 'Cancelar' : 'Nuevo traslado' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="space-y-1">
          <label class="text-sm font-medium">Tienda origen</label>
          <select
            v-model="form.tiendaOrigenId"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option value="" disabled>Seleccione…</option>
            <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Tienda destino</label>
          <select
            v-model="form.tiendaDestinoId"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option value="" disabled>Seleccione…</option>
            <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
          </select>
        </div>
      </div>

      <div class="space-y-2">
        <label class="text-sm font-medium">Líneas</label>
        <div v-for="(linea, index) in form.lineas" :key="index" class="grid gap-3 sm:grid-cols-3">
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
          <div class="flex gap-2">
            <input
              v-model="linea.cantidad"
              type="number"
              step="1"
              min="1"
              required
              placeholder="Cantidad"
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
            <th class="px-4 py-2 font-medium">Fecha</th>
            <th class="px-4 py-2 font-medium">Origen</th>
            <th class="px-4 py-2 font-medium">Destino</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
          <tr class="border-b border-mk-border bg-mk-surface/50">
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.origen"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.destino"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosColumna.estado"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todos</option>
                <option value="BORRADOR">Borrador</option>
                <option value="COMPLETADO">Completado</option>
                <option value="ANULADO">Anulado</option>
              </select>
            </th>
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
          <tr v-else-if="trasladosFiltrados.length === 0">
            <td colspan="5" class="px-4 py-6 text-center text-mk-text/60">Sin traslados registrados.</td>
          </tr>
          <tr
            v-for="traslado in trasladosFiltrados"
            :key="traslado.id"
            class="border-b border-mk-border last:border-0"
          >
            <td class="px-4 py-2">{{ new Date(traslado.fecha).toLocaleString() }}</td>
            <td class="px-4 py-2">{{ nombreTienda(traslado.tiendaOrigenId) }}</td>
            <td class="px-4 py-2">{{ nombreTienda(traslado.tiendaDestinoId) }}</td>
            <td class="px-4 py-2">
              <EstadoBadge
                :variant="ESTADO_VARIANT[traslado.estado]"
                :label="ESTADO_LABEL[traslado.estado]"
              />
            </td>
            <td class="px-4 py-2">
              <button
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="toggleDetalle(traslado)"
              >
                {{ detalleAbiertoId === traslado.id ? 'Ocultar' : 'Ver líneas' }}
              </button>
              <button
                v-if="traslado.estado === 'BORRADOR' && permissions.can('TRASLADOS_COMPLETAR')"
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="completar(traslado)"
              >
                Completar
              </button>
              <button
                v-if="traslado.estado === 'BORRADOR' && permissions.can('TRASLADOS_ANULAR')"
                type="button"
                class="text-mk-danger hover:underline"
                @click="anular(traslado)"
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

    <div v-if="trasladoEnDetalle" class="space-y-2">
      <h2 class="text-sm font-medium">Líneas del traslado #{{ trasladoEnDetalle.id }}</h2>
      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Producto</th>
              <th class="mk-num px-4 py-2 font-medium">Cantidad</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="linea in trasladoEnDetalle.lineas"
              :key="linea.id"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">{{ nombreProducto(linea.productoId) }}</td>
              <td class="mk-num px-4 py-2">{{ linea.cantidad }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
