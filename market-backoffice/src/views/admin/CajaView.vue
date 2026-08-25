<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useCaja } from '@/composables/useCaja'
import { useTiendas } from '@/composables/useTiendas'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { TipoMovimientoCaja } from '@/types/caja'

const {
  sesionAbierta,
  sesionLoading,
  sesionError,
  historial,
  historialLoading,
  historialPagina,
  historialTamano,
  historialTotalElementos,
  historialTotalPaginas,
  actionLoading,
  actionError,
  cargarAbierta,
  cargarHistorial,
  abrir,
  registrarMovimiento,
  cerrar,
} = useCaja()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)
const showHistorial = ref(false)

const montoInicial = ref('')
const movimiento = ref({ tipo: 'INGRESO' as TipoMovimientoCaja, concepto: '', monto: '' })
const montoFinalContado = ref('')

watch(tiendaId, (id) => {
  showHistorial.value = false
  if (id !== null) {
    cargarAbierta(id)
    montoInicial.value = ''
    movimiento.value = { tipo: 'INGRESO', concepto: '', monto: '' }
    montoFinalContado.value = ''
  }
})

async function onAbrir() {
  if (tiendaId.value === null) return
  await abrir(tiendaId.value, montoInicial.value)
}

async function onRegistrarMovimiento() {
  if (tiendaId.value === null) return
  const ok = await registrarMovimiento(tiendaId.value, { ...movimiento.value })
  if (ok) movimiento.value = { tipo: 'INGRESO', concepto: '', monto: '' }
}

async function onCerrar() {
  if (tiendaId.value === null) return
  const ok = await cerrar(tiendaId.value, montoFinalContado.value)
  if (ok) montoFinalContado.value = ''
}

async function onToggleHistorial() {
  showHistorial.value = !showHistorial.value
  historialPagina.value = 1
  if (showHistorial.value && tiendaId.value !== null) await cargarHistorial(tiendaId.value)
}

watch(historialTamano, () => {
  historialPagina.value = 1
})
watch([historialPagina, historialTamano], () => {
  if (showHistorial.value && tiendaId.value !== null) cargarHistorial(tiendaId.value)
})

onMounted(async () => {
  await cargarTiendas()
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
})
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Caja</h1>
      <p class="text-sm text-mk-text/70">
        Apertura, movimientos y cierre de turno de caja por tienda.
      </p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <select v-model="tiendaId" class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm">
        <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
      </select>
      <button type="button" class="text-sm text-mk-primary hover:underline" @click="onToggleHistorial">
        {{ showHistorial ? 'Ocultar historial' : 'Ver historial' }}
      </button>
    </div>

    <p v-if="sesionLoading" class="text-sm text-mk-text/60">Cargando…</p>
    <p v-else-if="sesionError" class="text-sm text-mk-danger">{{ sesionError }}</p>

    <div v-else-if="!sesionAbierta" class="rounded border border-mk-border p-4">
      <p class="mb-3 text-sm text-mk-text/70">No hay una caja abierta para esta tienda.</p>
      <form v-if="permissions.can('CAJA_ABRIR')" class="flex items-end gap-3" @submit.prevent="onAbrir">
        <div class="space-y-1">
          <label class="text-sm font-medium">Monto inicial</label>
          <input
            v-model="montoInicial"
            type="number"
            step="0.0001"
            min="0"
            required
            class="mk-input w-40 rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <button
          type="submit"
          :disabled="actionLoading"
          class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {{ actionLoading ? 'Abriendo…' : 'Abrir caja' }}
        </button>
      </form>
      <p v-if="actionError" class="mt-2 text-sm text-mk-danger" role="alert">{{ actionError }}</p>
    </div>

    <div v-else class="space-y-4">
      <div class="rounded border border-mk-border p-4">
        <dl class="grid grid-cols-2 gap-2 text-sm sm:grid-cols-3">
          <div>
            <dt class="text-mk-text/60">Apertura</dt>
            <dd>{{ new Date(sesionAbierta.fechaApertura).toLocaleString() }}</dd>
          </div>
          <div>
            <dt class="text-mk-text/60">Monto inicial</dt>
            <dd class="mk-num">{{ sesionAbierta.montoInicial }}</dd>
          </div>
          <div>
            <dt class="text-mk-text/60">Saldo esperado</dt>
            <dd class="mk-num font-medium">{{ sesionAbierta.saldoEsperado }}</dd>
          </div>
        </dl>
      </div>

      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Fecha</th>
              <th class="px-4 py-2 font-medium">Tipo</th>
              <th class="px-4 py-2 font-medium">Concepto</th>
              <th class="mk-num px-4 py-2 font-medium">Monto</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="sesionAbierta.movimientos.length === 0">
              <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Sin movimientos registrados.</td>
            </tr>
            <tr
              v-for="mov in sesionAbierta.movimientos"
              :key="mov.id"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">{{ new Date(mov.fecha).toLocaleString() }}</td>
              <td class="px-4 py-2">{{ mov.tipo }}</td>
              <td class="px-4 py-2">{{ mov.concepto }}</td>
              <td class="mk-num px-4 py-2">{{ mov.monto }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <form
        v-if="permissions.can('CAJA_REGISTRAR_MOVIMIENTO')"
        class="grid gap-3 rounded border border-mk-border p-4 sm:grid-cols-4"
        @submit.prevent="onRegistrarMovimiento"
      >
        <select
          v-model="movimiento.tipo"
          class="mk-input rounded border border-mk-border bg-transparent px-3 py-2"
        >
          <option value="INGRESO">INGRESO</option>
          <option value="EGRESO">EGRESO</option>
        </select>
        <input
          v-model="movimiento.concepto"
          type="text"
          required
          placeholder="Concepto"
          class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 sm:col-span-2"
        />
        <input
          v-model="movimiento.monto"
          type="number"
          step="0.0001"
          min="0"
          required
          placeholder="Monto"
          class="mk-input rounded border border-mk-border bg-transparent px-3 py-2"
        />
        <button
          type="submit"
          :disabled="actionLoading"
          class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50 sm:col-span-4"
        >
          {{ actionLoading ? 'Registrando…' : 'Registrar movimiento' }}
        </button>
      </form>

      <form
        v-if="permissions.can('CAJA_CERRAR')"
        class="flex items-end gap-3 rounded border border-mk-border p-4"
        @submit.prevent="onCerrar"
      >
        <div class="space-y-1">
          <label class="text-sm font-medium">Monto contado al cierre</label>
          <input
            v-model="montoFinalContado"
            type="number"
            step="0.0001"
            min="0"
            required
            class="mk-input w-40 rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <button
          type="submit"
          :disabled="actionLoading"
          class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {{ actionLoading ? 'Cerrando…' : 'Cerrar caja' }}
        </button>
      </form>

      <p v-if="actionError" class="text-sm text-mk-danger" role="alert">{{ actionError }}</p>
    </div>

    <div v-if="showHistorial" class="space-y-2">
      <h2 class="text-sm font-medium">Historial de sesiones</h2>
      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Apertura</th>
              <th class="px-4 py-2 font-medium">Cierre</th>
              <th class="mk-num px-4 py-2 font-medium">Monto inicial</th>
              <th class="mk-num px-4 py-2 font-medium">Saldo esperado</th>
              <th class="mk-num px-4 py-2 font-medium">Monto contado</th>
              <th class="px-4 py-2 font-medium">Estado</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="historialLoading">
              <td colspan="6" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
            </tr>
            <tr v-else-if="historial.length === 0">
              <td colspan="6" class="px-4 py-6 text-center text-mk-text/60">Sin sesiones registradas.</td>
            </tr>
            <tr v-for="sesion in historial" :key="sesion.id" class="border-b border-mk-border last:border-0">
              <td class="px-4 py-2">{{ new Date(sesion.fechaApertura).toLocaleString() }}</td>
              <td class="px-4 py-2">{{ sesion.fechaCierre ? new Date(sesion.fechaCierre).toLocaleString() : '—' }}</td>
              <td class="mk-num px-4 py-2">{{ sesion.montoInicial }}</td>
              <td class="mk-num px-4 py-2">{{ sesion.saldoEsperado }}</td>
              <td class="mk-num px-4 py-2">{{ sesion.montoFinalContado ?? '—' }}</td>
              <td class="px-4 py-2">
                <EstadoBadge
                  :variant="sesion.estado === 'ABIERTA' ? 'success' : 'neutral'"
                  :label="sesion.estado === 'ABIERTA' ? 'Abierta' : 'Cerrada'"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between text-sm text-mk-text/70">
        <select v-model.number="historialTamano" class="rounded border border-mk-border bg-transparent px-2 py-1">
          <option :value="10">10 / página</option>
          <option :value="25">25 / página</option>
          <option :value="50">50 / página</option>
          <option :value="100">100 / página</option>
        </select>
        <div class="flex items-center gap-2">
          <button
            type="button"
            :disabled="historialPagina <= 1"
            class="disabled:opacity-40"
            @click="historialPagina--"
          >
            Anterior
          </button>
          <span>Página {{ historialPagina }} de {{ historialTotalPaginas }} ({{ historialTotalElementos }} en total)</span>
          <button
            type="button"
            :disabled="historialPagina >= historialTotalPaginas"
            class="disabled:opacity-40"
            @click="historialPagina++"
          >
            Siguiente
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
