<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useGastosProgramados } from '@/composables/useGastosProgramados'
import { useTiendas } from '@/composables/useTiendas'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import { formatCurrency } from '@/utils/money'
import { formatFecha, formatFechaHora } from '@/utils/fecha'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import ModalDialog from '@/components/common/ModalDialog.vue'
import ActionIcon from '@/components/common/ActionIcon.vue'
import type { FrecuenciaGasto, GastoProgramado } from '@/types/gastoProgramado'

const {
  items,
  listLoading,
  listError,
  saveLoading,
  saveError,
  cargar,
  crear,
  actualizar,
  activar,
  desactivar,
  generarPago,
} = useGastosProgramados()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const permissions = usePermissionsStore()

const FRECUENCIAS: FrecuenciaGasto[] = ['SEMANAL', 'QUINCENAL', 'MENSUAL', 'ANUAL']

const tiendaId = ref<number | null>(null)
const showForm = ref(false)
const editandoId = ref<number | null>(null)
const detalleAbiertoId = ref<number | null>(null)

const form = ref({
  concepto: '',
  monto: '',
  frecuencia: 'MENSUAL' as FrecuenciaGasto,
  fechaInicio: '',
})

function abrirCrear() {
  editandoId.value = null
  form.value = { concepto: '', monto: '', frecuencia: 'MENSUAL', fechaInicio: '' }
  showForm.value = true
}

function abrirEditar(gasto: GastoProgramado) {
  editandoId.value = gasto.id
  form.value = { concepto: gasto.concepto, monto: gasto.monto, frecuencia: gasto.frecuencia, fechaInicio: '' }
  showForm.value = true
}

async function onSubmit() {
  if (tiendaId.value === null) return
  let ok: boolean
  if (editandoId.value !== null) {
    const gasto = items.value.find((g) => g.id === editandoId.value)
    if (!gasto) return
    ok = await actualizar(tiendaId.value, gasto, {
      concepto: form.value.concepto,
      monto: form.value.monto,
      frecuencia: form.value.frecuencia,
    })
  } else {
    ok = await crear(tiendaId.value, {
      concepto: form.value.concepto,
      monto: form.value.monto,
      frecuencia: form.value.frecuencia,
      fechaInicio: new Date(form.value.fechaInicio).toISOString(),
    })
  }
  if (ok) showForm.value = false
}

function toggleDetalle(gasto: GastoProgramado) {
  detalleAbiertoId.value = detalleAbiertoId.value === gasto.id ? null : gasto.id
}

function puedeGenerarPago(gasto: GastoProgramado): boolean {
  return gasto.activo && new Date(gasto.proximaFecha).getTime() <= Date.now()
}

const modalTitle = computed(() => (editandoId.value !== null ? 'Editar gasto' : 'Nuevo gasto'))

const COLUMNAS_FILTRO: FiltroColumna<GastoProgramado>[] = [
  { clave: 'concepto', tipo: 'texto', valor: (g) => g.concepto },
  {
    clave: 'frecuencia',
    tipo: 'opciones',
    valor: (g) => g.frecuencia,
    opciones: FRECUENCIAS.map((f) => ({ valor: f, etiqueta: f })),
  },
  {
    clave: 'activo',
    tipo: 'opciones',
    valor: (g) => (g.activo ? 'true' : 'false'),
    opciones: [
      { valor: 'true', etiqueta: 'Activo' },
      { valor: 'false', etiqueta: 'Inactivo' },
    ],
  },
]
const {
  busquedaGlobal,
  filtrosColumna,
  itemsFiltrados: gastosFiltrados,
  limpiarFiltros,
  hayFiltrosActivos,
} = useFiltrosTabla(items, COLUMNAS_FILTRO)

const gastoEnDetalle = computed(() => items.value.find((g) => g.id === detalleAbiertoId.value) ?? null)

watch(tiendaId, (id) => {
  detalleAbiertoId.value = null
  showForm.value = false
  if (id !== null) cargar(id)
})

onMounted(async () => {
  await cargarTiendas()
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Gastos programados</h1>
      <p class="text-sm text-mk-text/70">
        Gastos recurrentes de la tienda (renta, servicios, planilla). Generar pago registra el ciclo vencido
        y, si hay una caja abierta, lo refleja como egreso.
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
        v-if="permissions.can('GASTOS_PROGRAMADOS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="abrirCrear()"
      >
        Nuevo gasto
      </button>
    </div>

    <ModalDialog v-model="showForm" :title="modalTitle">
      <form class="space-y-3" @submit.prevent="onSubmit">
        <div class="grid gap-3 sm:grid-cols-2">
          <div class="space-y-1">
            <label class="text-sm font-medium">Concepto</label>
            <input
              v-model="form.concepto"
              required
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
            />
          </div>
          <div class="space-y-1">
            <label class="text-sm font-medium">Monto</label>
            <input
              v-model="form.monto"
              type="number"
              step="0.01"
              min="0"
              required
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
            />
          </div>
          <div class="space-y-1">
            <label class="text-sm font-medium">Frecuencia</label>
            <select
              v-model="form.frecuencia"
              required
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
            >
              <option v-for="f in FRECUENCIAS" :key="f" :value="f">{{ f }}</option>
            </select>
          </div>
          <div v-if="editandoId === null" class="space-y-1">
            <label class="text-sm font-medium">Fecha de inicio</label>
            <input
              v-model="form.fechaInicio"
              type="datetime-local"
              required
              class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
            />
          </div>
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
            {{ saveLoading ? 'Guardando…' : editandoId !== null ? 'Guardar cambios' : 'Crear' }}
          </button>
        </div>
      </form>
    </ModalDialog>

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
            <th class="px-4 py-2 font-medium">Concepto</th>
            <th class="mk-num px-4 py-2 font-medium">Monto</th>
            <th class="px-4 py-2 font-medium">Frecuencia</th>
            <th class="px-4 py-2 font-medium">Próxima fecha</th>
            <th class="px-4 py-2 font-medium">Activo</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
          <tr class="border-b border-mk-border bg-mk-surface/50">
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.concepto"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosColumna.frecuencia"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todas</option>
                <option v-for="f in FRECUENCIAS" :key="f" :value="f">{{ f }}</option>
              </select>
            </th>
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosColumna.activo"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todos</option>
                <option value="true">Activo</option>
                <option value="false">Inactivo</option>
              </select>
            </th>
            <th class="px-4 py-1.5"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="6" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="6" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="gastosFiltrados.length === 0">
            <td colspan="6" class="px-4 py-6 text-center text-mk-text/60">Sin gastos programados.</td>
          </tr>
          <tr
            v-for="gasto in gastosFiltrados"
            :key="gasto.id"
            class="border-b border-mk-border last:border-0"
          >
            <td class="px-4 py-2">{{ gasto.concepto }}</td>
            <td class="mk-num px-4 py-2">{{ formatCurrency(gasto.monto) }}</td>
            <td class="px-4 py-2">{{ gasto.frecuencia }}</td>
            <td class="px-4 py-2">{{ formatFecha(gasto.proximaFecha) }}</td>
            <td class="px-4 py-2">
              <EstadoBadge
                :variant="gasto.activo ? 'success' : 'neutral'"
                :label="gasto.activo ? 'Activo' : 'Inactivo'"
              />
            </td>
            <td class="px-4 py-2">
              <div class="mk-row-actions">
                <button
                  type="button"
                  class="mk-row-btn mk-row-btn-neutral"
                  :title="detalleAbiertoId === gasto.id ? 'Ocultar' : 'Ver pagos'"
                  @click="toggleDetalle(gasto)"
                >
                  <ActionIcon name="eye" />
                </button>
                <button
                  v-if="permissions.can('GASTOS_PROGRAMADOS_EDITAR')"
                  type="button"
                  class="mk-row-btn"
                  title="Editar"
                  @click="abrirEditar(gasto)"
                >
                  <ActionIcon name="edit" />
                </button>
                <button
                  v-if="puedeGenerarPago(gasto) && permissions.can('GASTOS_PROGRAMADOS_GENERAR_PAGO')"
                  type="button"
                  class="mk-row-btn mk-row-btn-success"
                  title="Generar pago"
                  @click="tiendaId !== null && generarPago(tiendaId, gasto)"
                >
                  <ActionIcon name="cash" />
                </button>
                <button
                  v-if="gasto.activo && permissions.can('GASTOS_PROGRAMADOS_EDITAR')"
                  type="button"
                  class="mk-row-btn mk-row-btn-danger"
                  title="Desactivar"
                  @click="tiendaId !== null && desactivar(tiendaId, gasto)"
                >
                  <ActionIcon name="power" />
                </button>
                <button
                  v-else-if="permissions.can('GASTOS_PROGRAMADOS_EDITAR')"
                  type="button"
                  class="mk-row-btn mk-row-btn-success"
                  title="Activar"
                  @click="tiendaId !== null && activar(tiendaId, gasto)"
                >
                  <ActionIcon name="power" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="gastoEnDetalle" class="space-y-2">
      <h2 class="text-sm font-medium">Pagos generados de "{{ gastoEnDetalle.concepto }}"</h2>
      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Fecha</th>
              <th class="mk-num px-4 py-2 font-medium">Monto</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="gastoEnDetalle.pagos.length === 0">
              <td colspan="2" class="px-4 py-6 text-center text-mk-text/60">Sin pagos generados.</td>
            </tr>
            <tr
              v-for="pago in gastoEnDetalle.pagos"
              :key="pago.id"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">{{ formatFechaHora(pago.fecha) }}</td>
              <td class="mk-num px-4 py-2">{{ formatCurrency(pago.monto) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
