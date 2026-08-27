<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js'
import { Bar } from 'vue-chartjs'
import { useDashboard } from '@/composables/useDashboard'
import { useDashboardGrupo } from '@/composables/useDashboardGrupo'
import { useTiendas } from '@/composables/useTiendas'
import { useGruposTienda } from '@/composables/useGruposTienda'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend)

interface OpcionTienda {
  id: number
  nombre: string
}

const { resumen, loading, error, cargar } = useDashboard()
const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const { items: grupos, cargar: cargarGrupos } = useGruposTienda()
const {
  resumen: resumenGrupo,
  loading: loadingGrupo,
  error: errorGrupo,
  cargar: cargarResumenGrupo,
} = useDashboardGrupo()
const permissions = usePermissionsStore()

const tiendaId = ref<number | null>(null)
const modo = ref<'tienda' | 'grupo'>('tienda')
const grupoId = ref<number | null>(null)

// GET /tiendas exige TIENDAS_VER (permiso de administración de catálogo), que un
// Encargado no necesariamente tiene. Si la lista queda vacía por eso, se cae a los
// tiendaIds ya conocidos por sesión (sin nombre bonito) para no dejar el dashboard en blanco.
const tiendasVisibles = computed<OpcionTienda[]>(() => {
  if (permissions.alcanceGlobal) return tiendas.value
  const propias = tiendas.value.filter((t) => permissions.tiendaIds.has(t.id))
  if (propias.length > 0) return propias
  return Array.from(permissions.tiendaIds).map((id) => ({ id, nombre: `Tienda #${id}` }))
})

// GET /grupos-tienda exige GRUPOS_TIENDA_VER (solo ADMIN) — un ADMIN_GRUPO no lo
// tiene, así que cae al mismo fallback "Grupo #id" que tiendasVisibles usa arriba.
const gruposVisibles = computed<OpcionTienda[]>(() => {
  return Array.from(permissions.grupoIds).map((id) => {
    const conocido = grupos.value.find((g) => g.id === id)
    return { id, nombre: conocido?.nombre ?? `Grupo #${id}` }
  })
})

watch(tiendaId, (id) => {
  if (id !== null) cargar(id)
})

watch(grupoId, (id) => {
  if (id !== null) cargarResumenGrupo(id)
})

onMounted(async () => {
  await Promise.all([cargarTiendas(), cargarGrupos()])
  if (tiendasVisibles.value.length > 0) tiendaId.value = tiendasVisibles.value[0].id
  if (gruposVisibles.value.length > 0) grupoId.value = gruposVisibles.value[0].id
})

function num(valor: string | null | undefined): number {
  if (!valor) return 0
  const parsed = Number.parseFloat(valor)
  return Number.isFinite(parsed) ? parsed : 0
}

const comparativoVentasPct = computed(() => {
  if (!resumen.value) return null
  const anterior = num(resumen.value.ventasMesAnteriorTotal)
  if (anterior === 0) return null
  const actual = num(resumen.value.ventasMesTotal)
  return ((actual - anterior) / anterior) * 100
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: { y: { beginAtZero: true } },
}

const chartOptionsConLeyenda = {
  ...chartOptions,
  plugins: { legend: { display: true, position: 'bottom' as const } },
}

const ventasChartData = computed(() => ({
  labels: ['Mes anterior', 'Mes actual'],
  datasets: [
    {
      label: 'Ventas',
      backgroundColor: ['rgb(100 116 139)', 'rgb(46 139 87)'],
      data: [num(resumen.value?.ventasMesAnteriorTotal), num(resumen.value?.ventasMesTotal)],
    },
  ],
}))

const agingChartData = computed(() => ({
  labels: ['0-30 días', '31-60 días', 'Más de 60 días'],
  datasets: [
    {
      label: 'Por cobrar',
      backgroundColor: 'rgb(46 139 87)',
      data: [
        num(resumen.value?.cxcAging0a30),
        num(resumen.value?.cxcAging31a60),
        num(resumen.value?.cxcAgingMas60),
      ],
    },
    {
      label: 'Por pagar',
      backgroundColor: 'rgb(224 138 63)',
      data: [
        num(resumen.value?.cxpAging0a30),
        num(resumen.value?.cxpAging31a60),
        num(resumen.value?.cxpAgingMas60),
      ],
    },
  ],
}))

function formatFecha(iso: string): string {
  return new Date(iso).toLocaleDateString('es-GT', { day: '2-digit', month: 'short', year: 'numeric' })
}

function tipoVencimientoLabel(tipo: string): string {
  return tipo === 'CUENTA_POR_COBRAR' ? 'Por cobrar' : 'Por pagar'
}
</script>

<template>
  <div class="mx-auto max-w-7xl space-y-6 p-6">
    <header class="flex flex-wrap items-center justify-between gap-3">
      <div class="space-y-1">
        <h1 class="text-xl font-semibold text-mk-text">Dashboard</h1>
        <p class="text-sm text-mk-text-muted">
          {{ modo === 'tienda' ? 'Resumen ejecutivo de indicadores de la tienda.' : 'Resumen agregado del grupo de tiendas.' }}
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <div v-if="gruposVisibles.length > 0" class="mk-btn-group flex overflow-hidden rounded border border-mk-border text-sm">
          <button
            type="button"
            class="px-3 py-1.5"
            :class="modo === 'tienda' ? 'bg-mk-primary text-white' : 'bg-transparent text-mk-text'"
            @click="modo = 'tienda'"
          >
            Por tienda
          </button>
          <button
            type="button"
            class="px-3 py-1.5"
            :class="modo === 'grupo' ? 'bg-mk-primary text-white' : 'bg-transparent text-mk-text'"
            @click="modo = 'grupo'"
          >
            Por grupo
          </button>
        </div>

        <template v-if="modo === 'tienda'">
          <select v-if="tiendasVisibles.length > 1" v-model="tiendaId" class="mk-input w-56">
            <option v-for="tienda in tiendasVisibles" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
          </select>
          <p v-else-if="tiendasVisibles.length === 1" class="text-sm font-medium text-mk-text">
            {{ tiendasVisibles[0].nombre }}
          </p>
        </template>
        <template v-else>
          <select v-if="gruposVisibles.length > 1" v-model="grupoId" class="mk-input w-56">
            <option v-for="grupo in gruposVisibles" :key="grupo.id" :value="grupo.id">{{ grupo.nombre }}</option>
          </select>
          <p v-else-if="gruposVisibles.length === 1" class="text-sm font-medium text-mk-text">
            {{ gruposVisibles[0].nombre }}
          </p>
        </template>
      </div>
    </header>

    <template v-if="modo === 'grupo'">
      <p v-if="loadingGrupo" class="text-sm text-mk-text-muted">Cargando…</p>
      <p v-else-if="errorGrupo" class="text-sm text-mk-danger" role="alert">{{ errorGrupo }}</p>

      <div v-else-if="resumenGrupo" class="space-y-6">
        <p class="text-sm text-mk-text-muted">
          {{ resumenGrupo.tiendaIds.length }} tienda(s) en el grupo.
        </p>

        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Ventas de hoy</p>
            <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumenGrupo.ventasHoyTotal }}</p>
            <p class="text-sm text-mk-text-muted">{{ resumenGrupo.ventasHoyCantidad }} venta(s)</p>
          </div>
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Ventas del mes</p>
            <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumenGrupo.ventasMesTotal }}</p>
            <p class="text-sm text-mk-text-muted">{{ resumenGrupo.ventasMesCantidad }} venta(s)</p>
          </div>
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Ticket promedio</p>
            <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumenGrupo.ticketPromedioMes }}</p>
            <p class="text-sm text-mk-text-muted">
              {{ resumenGrupo.facturasEmitidasMes }} factura(s), {{ resumenGrupo.facturasFelCertificadasMes }} certificada(s) FEL
            </p>
          </div>
          <div v-if="resumenGrupo.utilidadMesTotal !== null" class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Utilidad del mes</p>
            <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumenGrupo.utilidadMesTotal }}</p>
            <p class="text-sm text-mk-text-muted">
              Margen: {{ resumenGrupo.margenPromedioMes ?? '—' }}<span v-if="resumenGrupo.margenPromedioMes">%</span>
            </p>
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Inventario valorizado</p>
            <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumenGrupo.inventarioValorizadoTotal }}</p>
          </div>
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Productos agotados</p>
            <p class="mk-num text-2xl font-semibold" :class="resumenGrupo.productosAgotados > 0 ? 'text-mk-danger' : 'text-mk-text'">
              {{ resumenGrupo.productosAgotados }}
            </p>
          </div>
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Bajo stock mínimo</p>
            <p class="mk-num text-2xl font-semibold" :class="resumenGrupo.productosBajoMinimo > 0 ? 'text-mk-pending' : 'text-mk-text'">
              {{ resumenGrupo.productosBajoMinimo }}
            </p>
          </div>
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Sin movimiento (60+ días)</p>
            <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumenGrupo.productosSinMovimiento }}</p>
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Caja</p>
            <p class="text-2xl font-semibold text-mk-text">
              {{ resumenGrupo.tiendasConCajaAbierta }} / {{ resumenGrupo.totalTiendas }} abierta(s)
            </p>
            <p class="mk-num text-sm text-mk-text-muted">Saldo esperado: {{ resumenGrupo.cajaSaldoEsperadoTotal }}</p>
          </div>
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Ingresos de hoy</p>
            <p class="mk-num text-2xl font-semibold text-mk-success">{{ resumenGrupo.ingresosHoy }}</p>
          </div>
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Egresos de hoy</p>
            <p class="mk-num text-2xl font-semibold text-mk-danger">{{ resumenGrupo.egresosHoy }}</p>
          </div>
          <div class="mk-card space-y-2 p-4">
            <p class="text-sm text-mk-text-muted">Alertas activas</p>
            <div class="flex items-center gap-2">
              <EstadoBadge variant="danger" :label="`${resumenGrupo.alertasCriticas} crítica(s)`" />
              <EstadoBadge variant="pending" :label="`${resumenGrupo.alertasPreventivas} preventiva(s)`" />
            </div>
          </div>
        </div>

        <div class="grid gap-4 lg:grid-cols-2">
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Saldo pendiente por cobrar</p>
            <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumenGrupo.saldoPendienteCuentasPorCobrar }}</p>
            <p class="text-sm" :class="resumenGrupo.cuentasPorCobrarVencidas > 0 ? 'text-mk-danger' : 'text-mk-text-muted'">
              {{ resumenGrupo.cuentasPorCobrarVencidas }} vencida(s)
            </p>
          </div>
          <div class="mk-card space-y-1 p-4">
            <p class="text-sm text-mk-text-muted">Saldo pendiente por pagar</p>
            <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumenGrupo.saldoPendienteCuentasPorPagar }}</p>
            <p class="text-sm" :class="resumenGrupo.cuentasPorPagarVencidas > 0 ? 'text-mk-danger' : 'text-mk-text-muted'">
              {{ resumenGrupo.cuentasPorPagarVencidas }} vencida(s)
            </p>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
    <p v-if="loading" class="text-sm text-mk-text-muted">Cargando…</p>
    <p v-else-if="error" class="text-sm text-mk-danger" role="alert">{{ error }}</p>

    <div v-else-if="resumen" class="space-y-6">
      <!-- KPIs de ventas -->
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Ventas de hoy</p>
          <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumen.ventasHoyTotal }}</p>
          <p class="text-sm text-mk-text-muted">{{ resumen.ventasHoyCantidad }} venta(s)</p>
        </div>

        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Ventas del mes</p>
          <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumen.ventasMesTotal }}</p>
          <p
            v-if="comparativoVentasPct !== null"
            class="text-sm"
            :class="comparativoVentasPct >= 0 ? 'text-mk-success' : 'text-mk-danger'"
          >
            {{ comparativoVentasPct >= 0 ? '+' : '' }}{{ comparativoVentasPct.toFixed(1) }}% vs mes anterior
          </p>
          <p v-else class="text-sm text-mk-text-muted">Sin datos del mes anterior</p>
        </div>

        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Ticket promedio</p>
          <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumen.ticketPromedioMes }}</p>
          <p class="text-sm text-mk-text-muted">
            {{ resumen.facturasEmitidasMes }} factura(s), {{ resumen.facturasFelCertificadasMes }} certificada(s) FEL
          </p>
        </div>

        <div v-if="resumen.utilidadMesTotal !== null" class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Utilidad del mes</p>
          <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumen.utilidadMesTotal }}</p>
          <p class="text-sm text-mk-text-muted">
            Margen: {{ resumen.margenPromedioMes ?? '—' }}<span v-if="resumen.margenPromedioMes">%</span>
          </p>
        </div>
      </div>

      <!-- KPIs de inventario -->
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Inventario valorizado</p>
          <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumen.inventarioValorizadoTotal }}</p>
        </div>
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Productos agotados</p>
          <p class="mk-num text-2xl font-semibold" :class="resumen.productosAgotados > 0 ? 'text-mk-danger' : 'text-mk-text'">
            {{ resumen.productosAgotados }}
          </p>
        </div>
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Bajo stock mínimo</p>
          <p class="mk-num text-2xl font-semibold" :class="resumen.productosBajoMinimo > 0 ? 'text-mk-pending' : 'text-mk-text'">
            {{ resumen.productosBajoMinimo }}
          </p>
        </div>
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Sin movimiento (60+ días)</p>
          <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumen.productosSinMovimiento }}</p>
        </div>
      </div>

      <!-- Caja y alertas -->
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Caja</p>
          <p class="text-2xl font-semibold text-mk-text">{{ resumen.cajaAbierta ? 'Abierta' : 'Cerrada' }}</p>
          <p v-if="resumen.cajaAbierta" class="mk-num text-sm text-mk-text-muted">
            Saldo esperado: {{ resumen.cajaSaldoEsperado }}
          </p>
        </div>
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Ingresos de hoy</p>
          <p class="mk-num text-2xl font-semibold text-mk-success">{{ resumen.ingresosHoy }}</p>
        </div>
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Egresos de hoy</p>
          <p class="mk-num text-2xl font-semibold text-mk-danger">{{ resumen.egresosHoy }}</p>
        </div>
        <div class="mk-card space-y-2 p-4">
          <p class="text-sm text-mk-text-muted">Alertas activas</p>
          <div class="flex items-center gap-2">
            <EstadoBadge variant="danger" :label="`${resumen.alertasCriticas} crítica(s)`" />
            <EstadoBadge variant="pending" :label="`${resumen.alertasPreventivas} preventiva(s)`" />
          </div>
        </div>
      </div>

      <!-- Gráficas -->
      <div class="grid gap-4 lg:grid-cols-2">
        <div class="mk-card space-y-3 p-4">
          <p class="text-sm font-medium text-mk-text">Ventas: mes actual vs. mes anterior</p>
          <div class="h-64"><Bar :data="ventasChartData" :options="chartOptions" /></div>
        </div>
        <div class="mk-card space-y-3 p-4">
          <p class="text-sm font-medium text-mk-text">Antigüedad de saldos (aging)</p>
          <div class="h-64"><Bar :data="agingChartData" :options="chartOptionsConLeyenda" /></div>
        </div>
      </div>

      <!-- Cuentas por cobrar / pagar -->
      <div class="grid gap-4 lg:grid-cols-2">
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Saldo pendiente por cobrar</p>
          <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumen.saldoPendienteCuentasPorCobrar }}</p>
          <p class="text-sm" :class="resumen.cuentasPorCobrarVencidas > 0 ? 'text-mk-danger' : 'text-mk-text-muted'">
            {{ resumen.cuentasPorCobrarVencidas }} vencida(s)
          </p>
        </div>
        <div class="mk-card space-y-1 p-4">
          <p class="text-sm text-mk-text-muted">Saldo pendiente por pagar</p>
          <p class="mk-num text-2xl font-semibold text-mk-text">{{ resumen.saldoPendienteCuentasPorPagar }}</p>
          <p class="text-sm" :class="resumen.cuentasPorPagarVencidas > 0 ? 'text-mk-danger' : 'text-mk-text-muted'">
            {{ resumen.cuentasPorPagarVencidas }} vencida(s)
          </p>
        </div>
      </div>

      <!-- Listas de acción -->
      <div class="grid gap-4 lg:grid-cols-2">
        <div class="mk-card space-y-3 p-4">
          <p class="text-sm font-medium text-mk-text">Próximos vencimientos</p>
          <p v-if="resumen.proximosVencimientos.length === 0" class="text-sm text-mk-text-muted">Sin vencimientos próximos.</p>
          <table v-else class="mk-scroll-x w-full text-sm">
            <tbody>
              <tr v-for="v in resumen.proximosVencimientos" :key="`${v.tipo}-${v.referenciaId}`" class="border-t border-mk-border">
                <td class="py-2 pr-2">{{ tipoVencimientoLabel(v.tipo) }} #{{ v.referenciaId }}</td>
                <td class="mk-num py-2 pr-2">{{ v.monto }}</td>
                <td class="py-2 text-mk-text-muted">{{ formatFecha(v.fechaVencimiento) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mk-card space-y-3 p-4">
          <p class="text-sm font-medium text-mk-text">Recordatorios de gastos programados</p>
          <p v-if="resumen.recordatorios.length === 0" class="text-sm text-mk-text-muted">Sin recordatorios próximos.</p>
          <table v-else class="mk-scroll-x w-full text-sm">
            <tbody>
              <tr v-for="r in resumen.recordatorios" :key="r.gastoProgramadoId" class="border-t border-mk-border">
                <td class="py-2 pr-2">{{ r.concepto }}</td>
                <td class="mk-num py-2 pr-2">{{ r.monto }}</td>
                <td class="py-2 text-mk-text-muted">{{ formatFecha(r.proximaFecha) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mk-card space-y-3 p-4">
          <p class="text-sm font-medium text-mk-text">Top cobros pendientes</p>
          <p v-if="resumen.topCobrosPendientes.length === 0" class="text-sm text-mk-text-muted">Sin cuentas pendientes.</p>
          <table v-else class="mk-scroll-x w-full text-sm">
            <tbody>
              <tr v-for="c in resumen.topCobrosPendientes" :key="c.id" class="border-t border-mk-border">
                <td class="py-2 pr-2">Cliente #{{ c.contraparteId }}</td>
                <td class="mk-num py-2 pr-2">{{ c.monto }}</td>
                <td class="py-2 text-mk-text-muted">{{ formatFecha(c.fechaVencimiento) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mk-card space-y-3 p-4">
          <p class="text-sm font-medium text-mk-text">Top pagos pendientes</p>
          <p v-if="resumen.topPagosPendientes.length === 0" class="text-sm text-mk-text-muted">Sin cuentas pendientes.</p>
          <table v-else class="mk-scroll-x w-full text-sm">
            <tbody>
              <tr v-for="c in resumen.topPagosPendientes" :key="c.id" class="border-t border-mk-border">
                <td class="py-2 pr-2">Proveedor #{{ c.contraparteId }}</td>
                <td class="mk-num py-2 pr-2">{{ c.monto }}</td>
                <td class="py-2 text-mk-text-muted">{{ formatFecha(c.fechaVencimiento) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mk-card space-y-3 p-4">
          <p class="text-sm font-medium text-mk-text">Sugerencias de compra</p>
          <p v-if="resumen.sugerenciasCompra.length === 0" class="text-sm text-mk-text-muted">Sin sugerencias por ahora.</p>
          <table v-else class="mk-scroll-x w-full text-sm">
            <thead>
              <tr class="text-left text-mk-text-muted">
                <th class="pb-2 font-normal">Producto</th>
                <th class="pb-2 font-normal">Existencia</th>
                <th class="pb-2 font-normal">Sugerido</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in resumen.sugerenciasCompra" :key="s.productoId" class="border-t border-mk-border">
                <td class="py-2 pr-2">Producto #{{ s.productoId }}</td>
                <td class="mk-num py-2 pr-2">{{ s.existenciaActual }}</td>
                <td class="mk-num py-2 text-mk-success">{{ s.cantidadSugerida }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mk-card space-y-3 p-4">
          <p class="text-sm font-medium text-mk-text">Sugerencias de traslado</p>
          <p v-if="resumen.sugerenciasTraslado.length === 0" class="text-sm text-mk-text-muted">Sin sugerencias por ahora.</p>
          <table v-else class="mk-scroll-x w-full text-sm">
            <thead>
              <tr class="text-left text-mk-text-muted">
                <th class="pb-2 font-normal">Producto</th>
                <th class="pb-2 font-normal">Desde tienda</th>
                <th class="pb-2 font-normal">Cantidad</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in resumen.sugerenciasTraslado" :key="s.productoId" class="border-t border-mk-border">
                <td class="py-2 pr-2">Producto #{{ s.productoId }}</td>
                <td class="py-2 pr-2">Tienda #{{ s.tiendaOrigenId }}</td>
                <td class="mk-num py-2 text-mk-success">{{ s.cantidadSugerida }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>
