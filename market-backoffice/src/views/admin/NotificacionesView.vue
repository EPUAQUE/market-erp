<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useNotificaciones } from '@/composables/useNotificaciones'
import { useTiendas } from '@/composables/useTiendas'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import ActionIcon from '@/components/common/ActionIcon.vue'
import type { Notificacion, TipoNotificacion } from '@/types/notificacion'

const {
  items,
  listLoading,
  listError,
  generarLoading,
  pagina,
  tamano,
  totalElementos,
  totalPaginas,
  cargar,
  generar,
  marcarLeida,
} = useNotificaciones()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)

const ETIQUETAS_TIPO: Record<TipoNotificacion, string> = {
  CUENTA_POR_PAGAR_VENCIDA: 'Cuenta por pagar vencida',
  CUENTA_POR_COBRAR_VENCIDA: 'Cuenta por cobrar vencida',
  GASTO_PROGRAMADO_VENCIDO: 'Gasto programado vencido',
  STOCK_BAJO: 'Stock bajo',
}

// Nota: con paginación del servidor, este filtro solo busca dentro de la
// página cargada, no en todo el listado (ver CLAUDE.md, "Server-side
// pagination").
const COLUMNAS_FILTRO: FiltroColumna<Notificacion>[] = [
  { clave: 'tipo', tipo: 'opciones', valor: (n) => n.tipo },
  { clave: 'mensaje', tipo: 'texto', valor: (n) => n.mensaje },
  {
    clave: 'leida',
    tipo: 'opciones',
    valor: (n) => (n.leida ? 'true' : 'false'),
    opciones: [
      { valor: 'false', etiqueta: 'Nueva' },
      { valor: 'true', etiqueta: 'Leída' },
    ],
  },
]
const { busquedaGlobal, filtrosColumna, itemsFiltrados, limpiarFiltros, hayFiltrosActivos } = useFiltrosTabla(
  items,
  COLUMNAS_FILTRO,
)

const itemsOrdenados = computed(() =>
  [...itemsFiltrados.value].sort((a, b) => {
    if (a.leida !== b.leida) return a.leida ? 1 : -1
    return new Date(b.fecha).getTime() - new Date(a.fecha).getTime()
  }),
)

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

async function onGenerar() {
  if (tiendaId.value !== null) await generar(tiendaId.value)
}

onMounted(async () => {
  await cargarTiendas()
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Notificaciones</h1>
      <p class="text-sm text-mk-text/70">
        Alertas generadas a partir de cuentas vencidas, gastos programados vencidos y stock bajo.
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
        v-if="permissions.can('NOTIFICACIONES_GENERAR')"
        type="button"
        :disabled="generarLoading"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        @click="onGenerar"
      >
        {{ generarLoading ? 'Generando…' : 'Generar notificaciones' }}
      </button>
    </div>

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
            <th class="px-4 py-2 font-medium">Tipo</th>
            <th class="px-4 py-2 font-medium">Mensaje</th>
            <th class="px-4 py-2 font-medium">Fecha</th>
            <th class="px-4 py-2 font-medium">Leída</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
          <tr class="border-b border-mk-border bg-mk-surface/50">
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosColumna.tipo"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todos</option>
                <option v-for="(etiqueta, tipo) in ETIQUETAS_TIPO" :key="tipo" :value="tipo">
                  {{ etiqueta }}
                </option>
              </select>
            </th>
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.mensaje"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosColumna.leida"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todas</option>
                <option value="false">Nueva</option>
                <option value="true">Leída</option>
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
          <tr v-else-if="itemsOrdenados.length === 0">
            <td colspan="5" class="px-4 py-6 text-center text-mk-text/60">Sin notificaciones.</td>
          </tr>
          <tr
            v-for="notificacion in itemsOrdenados"
            :key="notificacion.id"
            class="border-b border-mk-border last:border-0"
            :class="{ 'text-mk-text/50': notificacion.leida }"
          >
            <td class="px-4 py-2">{{ ETIQUETAS_TIPO[notificacion.tipo] }}</td>
            <td class="px-4 py-2">{{ notificacion.mensaje }}</td>
            <td class="px-4 py-2">{{ new Date(notificacion.fecha).toLocaleString() }}</td>
            <td class="px-4 py-2">
              <EstadoBadge
                :variant="notificacion.leida ? 'neutral' : 'info'"
                :label="notificacion.leida ? 'Leída' : 'Nueva'"
              />
            </td>
            <td class="px-4 py-2">
              <div class="mk-row-actions">
                <button
                  v-if="!notificacion.leida && permissions.can('NOTIFICACIONES_MARCAR_LEIDA')"
                  type="button"
                  class="mk-row-btn mk-row-btn-success"
                  title="Marcar leída"
                  @click="tiendaId !== null && marcarLeida(tiendaId, notificacion)"
                >
                  <ActionIcon name="check" />
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
  </div>
</template>
