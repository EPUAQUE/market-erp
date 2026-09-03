<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useFel } from '@/composables/useFel'
import { useTiendas } from '@/composables/useTiendas'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import ActionIcon from '@/components/common/ActionIcon.vue'
import type { DocumentoFel } from '@/types/fel'
import type { EstadoBadgeVariant } from '@/components/common/EstadoBadge.vue'

const {
  items,
  listLoading,
  listError,
  emitirLoading,
  emitirError,
  pagina,
  tamano,
  totalElementos,
  totalPaginas,
  cargar,
  emitir,
  reintentar,
  anular,
} = useFel()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)
const ventaIdEmitir = ref('')
const anulandoId = ref<number | null>(null)
const motivoAnular = ref('')

const ETIQUETAS_ESTADO: Record<string, string> = {
  PENDIENTE: 'Pendiente',
  CERTIFICADO: 'Certificado',
  ANULADO: 'Anulado',
  ERROR: 'Error',
}
const ESTADO_VARIANT: Record<string, EstadoBadgeVariant> = {
  PENDIENTE: 'pending',
  CERTIFICADO: 'success',
  ANULADO: 'neutral',
  ERROR: 'danger',
}

// Nota: con paginación del servidor, este filtro solo busca dentro de la
// página cargada, no en todo el listado (ver CLAUDE.md, "Server-side
// pagination").
const COLUMNAS_FILTRO: FiltroColumna<DocumentoFel>[] = [
  { clave: 'venta', tipo: 'texto', valor: (d) => `#${d.ventaId}` },
  { clave: 'serieNumero', tipo: 'texto', valor: (d) => `${d.serie}-${d.numero}` },
  { clave: 'uuid', tipo: 'texto', valor: (d) => d.uuid ?? '' },
  {
    clave: 'estado',
    tipo: 'opciones',
    valor: (d) => d.estado,
    opciones: [
      { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
      { valor: 'CERTIFICADO', etiqueta: 'Certificado' },
      { valor: 'ANULADO', etiqueta: 'Anulado' },
      { valor: 'ERROR', etiqueta: 'Error' },
    ],
  },
]
const {
  busquedaGlobal,
  filtrosColumna,
  itemsFiltrados: documentosFiltrados,
  limpiarFiltros,
  hayFiltrosActivos,
} = useFiltrosTabla(items, COLUMNAS_FILTRO)

watch(tiendaId, (id) => {
  pagina.value = 1
  if (id !== null) cargar(id)
})

watch(tamano, () => {
  pagina.value = 1
})
watch([pagina, tamano], () => {
  if (tiendaId.value !== null) cargar(tiendaId.value)
})

async function onEmitir() {
  if (tiendaId.value === null || !ventaIdEmitir.value) return
  const ok = await emitir(tiendaId.value, Number(ventaIdEmitir.value))
  if (ok) ventaIdEmitir.value = ''
}

function abrirAnular(documento: DocumentoFel) {
  anulandoId.value = anulandoId.value === documento.id ? null : documento.id
  motivoAnular.value = ''
}

async function onAnular(documento: DocumentoFel) {
  if (tiendaId.value === null || !motivoAnular.value) return
  await anular(tiendaId.value, documento, motivoAnular.value)
  anulandoId.value = null
}

function onReintentar(documento: DocumentoFel) {
  if (tiendaId.value !== null) reintentar(tiendaId.value, documento)
}

onMounted(async () => {
  await cargarTiendas()
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Facturación electrónica (FEL)</h1>
      <p class="text-sm text-mk-text/70">
        Documentos FEL de ventas completadas. La certificación real ante la SAT depende de un proveedor
        certificador — mientras no haya uno configurado, se usa un adaptador de desarrollo.
      </p>
    </header>

    <select
      v-model="tiendaId"
      class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
    >
      <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
    </select>

    <form
      v-if="permissions.can('FEL_EMITIR')"
      class="flex items-end gap-3 rounded border border-mk-border p-4"
      @submit.prevent="onEmitir"
    >
      <div class="space-y-1">
        <label class="text-sm font-medium">Emitir FEL para la venta #</label>
        <input
          v-model="ventaIdEmitir"
          type="number"
          min="1"
          required
          class="mk-input w-40 rounded border border-mk-border bg-transparent px-3 py-2"
        />
      </div>
      <button
        type="submit"
        :disabled="emitirLoading"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {{ emitirLoading ? 'Emitiendo…' : 'Emitir' }}
      </button>
    </form>
    <p v-if="emitirError" class="text-sm text-mk-danger" role="alert">{{ emitirError }}</p>

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
            <th class="px-4 py-2 font-medium">Serie-Número</th>
            <th class="px-4 py-2 font-medium">UUID</th>
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
                v-model="filtrosColumna.serieNumero"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.uuid"
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
                <option value="PENDIENTE">Pendiente</option>
                <option value="CERTIFICADO">Certificado</option>
                <option value="ANULADO">Anulado</option>
                <option value="ERROR">Error</option>
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
          <tr v-else-if="documentosFiltrados.length === 0">
            <td colspan="5" class="px-4 py-6 text-center text-mk-text/60">Sin documentos FEL.</td>
          </tr>
          <template v-for="documento in documentosFiltrados" :key="documento.id">
            <tr class="border-b border-mk-border last:border-0">
              <td class="px-4 py-2">#{{ documento.ventaId }}</td>
              <td class="px-4 py-2">{{ documento.serie }}-{{ documento.numero }}</td>
              <td class="px-4 py-2 font-mono text-xs">{{ documento.uuid ?? '—' }}</td>
              <td class="px-4 py-2">
                <EstadoBadge
                  :variant="ESTADO_VARIANT[documento.estado]"
                  :label="ETIQUETAS_ESTADO[documento.estado]"
                />
              </td>
              <td class="px-4 py-2">
                <div class="mk-row-actions">
                  <button
                    v-if="documento.estado === 'ERROR' && permissions.can('FEL_EMITIR')"
                    type="button"
                    class="mk-row-btn"
                    title="Reintentar"
                    @click="onReintentar(documento)"
                  >
                    <ActionIcon name="refresh" />
                  </button>
                  <button
                    v-if="documento.estado === 'CERTIFICADO' && permissions.can('FEL_ANULAR')"
                    type="button"
                    class="mk-row-btn mk-row-btn-danger"
                    :title="anulandoId === documento.id ? 'Cancelar' : 'Anular'"
                    @click="abrirAnular(documento)"
                  >
                    <ActionIcon name="x" />
                  </button>
                </div>
              </td>
            </tr>
            <tr
              v-if="anulandoId === documento.id"
              class="border-b border-mk-border last:border-0 bg-mk-surface"
            >
              <td colspan="5" class="px-4 py-3">
                <form class="flex items-end gap-3" @submit.prevent="onAnular(documento)">
                  <div class="flex-1 space-y-1">
                    <label class="text-sm font-medium">Motivo de anulación</label>
                    <input
                      v-model="motivoAnular"
                      required
                      class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
                    />
                  </div>
                  <button
                    type="submit"
                    class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
                  >
                    Confirmar anulación
                  </button>
                </form>
              </td>
            </tr>
            <tr
              v-if="documento.estado === 'ERROR' && documento.mensajeError"
              class="border-b border-mk-border last:border-0"
            >
              <td colspan="5" class="px-4 py-2 text-sm text-mk-danger">
                Error: {{ documento.mensajeError }}
              </td>
            </tr>
            <tr
              v-if="documento.estado === 'ANULADO' && documento.motivoAnulacion"
              class="border-b border-mk-border last:border-0"
            >
              <td colspan="5" class="px-4 py-2 text-sm text-mk-text/60">
                Motivo: {{ documento.motivoAnulacion }}
              </td>
            </tr>
          </template>
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
  </div>
</template>
