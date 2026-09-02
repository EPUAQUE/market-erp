<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useReportes } from '@/composables/useReportes'
import { useTiendas } from '@/composables/useTiendas'
import { formatCurrency } from '@/utils/money'

const { reporteVentas, reporteCompras, loading, error, generarReporteVentas, generarReporteCompras } =
  useReportes()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()

const tiendaId = ref<number | null>(null)
const tipo = ref<'VENTAS' | 'COMPRAS'>('VENTAS')
const desde = ref('')
const hasta = ref('')

function inicioDeHoy(): string {
  const hoy = new Date()
  hoy.setHours(0, 0, 0, 0)
  return toDatetimeLocal(hoy)
}

function ahora(): string {
  return toDatetimeLocal(new Date())
}

function toDatetimeLocal(fecha: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${fecha.getFullYear()}-${pad(fecha.getMonth() + 1)}-${pad(fecha.getDate())}T${pad(fecha.getHours())}:${pad(fecha.getMinutes())}`
}

async function onGenerar() {
  if (tiendaId.value === null || !desde.value || !hasta.value) return
  const desdeIso = new Date(desde.value).toISOString()
  const hastaIso = new Date(hasta.value).toISOString()
  if (tipo.value === 'VENTAS') {
    await generarReporteVentas(tiendaId.value, desdeIso, hastaIso)
  } else {
    await generarReporteCompras(tiendaId.value, desdeIso, hastaIso)
  }
}

function csvEscape(valor: string | number): string {
  const texto = String(valor)
  return /[",\n]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto
}

function onExportarCsv() {
  let filas: string[]
  let nombreArchivo: string
  if (reporteVentas.value) {
    filas = ['Venta,Cliente,Fecha,Total']
    for (const linea of reporteVentas.value.lineas) {
      filas.push([linea.ventaId, linea.clienteId, linea.fecha, linea.total].map(csvEscape).join(','))
    }
    nombreArchivo = 'reporte-ventas.csv'
  } else if (reporteCompras.value) {
    filas = ['Compra,Proveedor,Fecha,Total']
    for (const linea of reporteCompras.value.lineas) {
      filas.push([linea.compraId, linea.proveedorId, linea.fecha, linea.total].map(csvEscape).join(','))
    }
    nombreArchivo = 'reporte-compras.csv'
  } else {
    return
  }

  const blob = new Blob([filas.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombreArchivo
  enlace.click()
  URL.revokeObjectURL(url)
}

const hayResultado = computed(() => reporteVentas.value !== null || reporteCompras.value !== null)

onMounted(async () => {
  await cargarTiendas()
  if (tiendas.value.length > 0) tiendaId.value = tiendas.value[0].id
  desde.value = inicioDeHoy()
  hasta.value = ahora()
})
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Reportes</h1>
      <p class="text-sm text-mk-text/70">Reporte de ventas o compras completadas en un rango de fechas.</p>
    </header>

    <form class="grid gap-3 rounded border border-mk-border p-4 sm:grid-cols-2" @submit.prevent="onGenerar">
      <div class="space-y-1">
        <label class="text-sm font-medium">Tienda</label>
        <select
          v-model="tiendaId"
          class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
        >
          <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
        </select>
      </div>
      <div class="space-y-1">
        <label class="text-sm font-medium">Tipo de reporte</label>
        <select
          v-model="tipo"
          class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
        >
          <option value="VENTAS">Ventas</option>
          <option value="COMPRAS">Compras</option>
        </select>
      </div>
      <div class="space-y-1">
        <label class="text-sm font-medium">Desde</label>
        <input
          v-model="desde"
          type="datetime-local"
          required
          class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
        />
      </div>
      <div class="space-y-1">
        <label class="text-sm font-medium">Hasta</label>
        <input
          v-model="hasta"
          type="datetime-local"
          required
          class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
        />
      </div>
      <div class="sm:col-span-2">
        <button
          type="submit"
          :disabled="loading"
          class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {{ loading ? 'Generando…' : 'Generar reporte' }}
        </button>
      </div>
    </form>

    <p v-if="error" class="text-sm text-mk-danger" role="alert">{{ error }}</p>

    <div v-if="reporteVentas" class="space-y-3">
      <div class="flex items-center justify-between">
        <p class="text-sm text-mk-text/70">
          {{ reporteVentas.cantidadVentas }} venta(s) — total
          <span class="mk-num font-semibold">{{ formatCurrency(reporteVentas.totalVentas) }}</span>
        </p>
        <button type="button" class="text-sm text-mk-primary hover:underline" @click="onExportarCsv">
          Exportar CSV
        </button>
      </div>
      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Venta</th>
              <th class="px-4 py-2 font-medium">Cliente</th>
              <th class="px-4 py-2 font-medium">Fecha</th>
              <th class="mk-num px-4 py-2 font-medium">Total</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="reporteVentas.lineas.length === 0">
              <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Sin ventas en el rango.</td>
            </tr>
            <tr
              v-for="linea in reporteVentas.lineas"
              :key="linea.ventaId"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">#{{ linea.ventaId }}</td>
              <td class="px-4 py-2">#{{ linea.clienteId }}</td>
              <td class="px-4 py-2">{{ new Date(linea.fecha).toLocaleString() }}</td>
              <td class="mk-num px-4 py-2">{{ formatCurrency(linea.total) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="reporteCompras" class="space-y-3">
      <div class="flex items-center justify-between">
        <p class="text-sm text-mk-text/70">
          {{ reporteCompras.cantidadCompras }} compra(s) — total
          <span class="mk-num font-semibold">{{ formatCurrency(reporteCompras.totalCompras) }}</span>
        </p>
        <button type="button" class="text-sm text-mk-primary hover:underline" @click="onExportarCsv">
          Exportar CSV
        </button>
      </div>
      <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
        <table class="w-full text-left text-sm">
          <thead class="border-b border-mk-border bg-mk-surface">
            <tr>
              <th class="px-4 py-2 font-medium">Compra</th>
              <th class="px-4 py-2 font-medium">Proveedor</th>
              <th class="px-4 py-2 font-medium">Fecha</th>
              <th class="mk-num px-4 py-2 font-medium">Total</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="reporteCompras.lineas.length === 0">
              <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Sin compras en el rango.</td>
            </tr>
            <tr
              v-for="linea in reporteCompras.lineas"
              :key="linea.compraId"
              class="border-b border-mk-border last:border-0"
            >
              <td class="px-4 py-2">#{{ linea.compraId }}</td>
              <td class="px-4 py-2">#{{ linea.proveedorId }}</td>
              <td class="px-4 py-2">{{ new Date(linea.fecha).toLocaleString() }}</td>
              <td class="mk-num px-4 py-2">{{ formatCurrency(linea.total) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <p v-else-if="!hayResultado && !loading" class="text-sm text-mk-text/60">
      Seleccione un rango y presione "Generar reporte".
    </p>
  </div>
</template>
