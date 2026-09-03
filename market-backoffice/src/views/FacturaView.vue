<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { felService } from '@/services/fel.service'
import { ventasService } from '@/services/ventas.service'
import { useTiendas } from '@/composables/useTiendas'
import { useClientes } from '@/composables/useClientes'
import { useProductos } from '@/composables/useProductos'
import { formatCurrency, calcularSubtotal } from '@/utils/money'
import { formatFecha } from '@/utils/fecha'
import { ApiClientError } from '@/services/http/ApiClient'
import type { DocumentoFel } from '@/types/fel'
import type { Venta } from '@/types/venta'

const route = useRoute()
const router = useRouter()

const tiendaId = Number(route.params.tiendaId)
const documentoId = Number(route.params.documentoId)

const { items: tiendas, cargar: cargarTiendas } = useTiendas()
const { items: clientes, cargar: cargarClientes } = useClientes()
const { items: productos, cargar: cargarProductos } = useProductos()

const documento = ref<DocumentoFel | null>(null)
const venta = ref<Venta | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

const tienda = computed(() => tiendas.value.find((t) => t.id === tiendaId))
const cliente = computed(() => clientes.value.find((c) => c.id === venta.value?.clienteId))

function nombreProducto(productoId: number): string {
  return productos.value.find((p) => p.id === productoId)?.nombre ?? `#${productoId}`
}

function codigoProducto(productoId: number): string {
  return productos.value.find((p) => p.id === productoId)?.codigoInterno ?? '—'
}

function imprimir() {
  window.print()
}

onMounted(async () => {
  loading.value = true
  error.value = null
  try {
    await Promise.all([cargarTiendas(), cargarClientes(), cargarProductos()])
    documento.value = await felService.obtener(tiendaId, documentoId)
    venta.value = await ventasService.obtener(tiendaId, documento.value.ventaId)
  } catch (err) {
    error.value = err instanceof ApiClientError ? err.message : 'No se pudo cargar la factura.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="mk-bg min-h-screen p-6">
    <div class="no-print mx-auto mb-4 flex max-w-3xl items-center justify-between">
      <button
        type="button"
        class="text-sm font-medium text-mk-primary hover:underline"
        @click="router.push('/fel')"
      >
        ← Volver a Facturación Electrónica
      </button>
      <button
        v-if="documento && !loading && !error"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="imprimir"
      >
        Imprimir
      </button>
    </div>

    <p v-if="loading" class="mx-auto max-w-3xl text-center text-sm text-mk-text-muted">Cargando…</p>
    <p v-else-if="error" class="mx-auto max-w-3xl text-center text-sm text-mk-danger" role="alert">
      {{ error }}
    </p>

    <div
      v-else-if="documento && venta"
      class="factura-papel mx-auto max-w-3xl rounded-lg border border-mk-border bg-white p-10 text-[#1f2937] shadow-mk"
    >
      <div class="flex items-start justify-between border-b border-gray-200 pb-6">
        <div>
          <div class="flex items-center gap-2">
            <div
              class="flex h-9 items-center justify-center rounded-md bg-gradient-to-br from-[#0F4C5C] to-[#2E8B57] px-2 text-xs font-extrabold text-white"
            >
              i365
            </div>
            <p class="text-lg font-extrabold tracking-tight">Inven365</p>
          </div>
          <p class="mt-2 text-sm text-gray-500">{{ tienda?.nombre ?? `Tienda #${tiendaId}` }}</p>
          <p v-if="tienda?.direccion" class="text-sm text-gray-500">{{ tienda.direccion }}</p>
          <p v-if="tienda?.telefono" class="text-sm text-gray-500">Tel. {{ tienda.telefono }}</p>
        </div>
        <div class="text-right">
          <p class="text-sm font-semibold uppercase tracking-wide text-gray-500">
            {{ documento.estado === 'CERTIFICADO' ? 'Factura' : documento.estado }}
          </p>
          <p class="mk-num text-sm text-gray-700">{{ documento.serie }}-{{ documento.numero }}</p>
          <p v-if="documento.uuid" class="mt-1 font-mono text-xs text-gray-400">{{ documento.uuid }}</p>
        </div>
      </div>

      <div class="mt-6 flex items-start justify-between">
        <div>
          <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Cliente</p>
          <p class="mt-1 text-sm font-semibold text-gray-800">
            {{ cliente?.nombre ?? `Cliente #${venta.clienteId}` }}
          </p>
          <p class="text-sm text-gray-500">NIT: {{ cliente?.nit ?? 'C/F' }}</p>
          <p v-if="cliente?.direccion" class="text-sm text-gray-500">{{ cliente.direccion }}</p>
        </div>
        <div class="text-right text-sm text-gray-500">
          <p>
            <span class="text-gray-400">Fecha de emisión:</span> {{ formatFecha(documento.fechaEmision) }}
          </p>
          <p v-if="documento.fechaCertificacion">
            <span class="text-gray-400">Certificación:</span> {{ formatFecha(documento.fechaCertificacion) }}
          </p>
          <p><span class="text-gray-400">Venta:</span> #{{ venta.id }}</p>
        </div>
      </div>

      <table class="mk-num mt-8 w-full text-left text-sm">
        <thead>
          <tr class="border-b border-gray-200 text-xs font-semibold uppercase tracking-wide text-gray-400">
            <th class="pb-2">Código</th>
            <th class="pb-2">Descripción</th>
            <th class="pb-2 text-right">Cantidad</th>
            <th class="pb-2 text-right">Precio unitario</th>
            <th class="pb-2 text-right">Subtotal</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="linea in venta.lineas" :key="linea.id" class="border-b border-gray-100">
            <td class="py-2 text-gray-500">{{ codigoProducto(linea.productoId) }}</td>
            <td class="py-2 text-gray-800">{{ nombreProducto(linea.productoId) }}</td>
            <td class="py-2 text-right text-gray-700">{{ linea.cantidad }}</td>
            <td class="py-2 text-right text-gray-700">{{ formatCurrency(linea.precioUnitario) }}</td>
            <td class="py-2 text-right text-gray-800">
              {{ formatCurrency(calcularSubtotal(linea.cantidad, linea.precioUnitario)) }}
            </td>
          </tr>
        </tbody>
      </table>

      <div class="mt-6 flex justify-end">
        <div class="w-56 space-y-1 text-sm">
          <div class="flex justify-between border-t border-gray-200 pt-2 text-base font-bold text-gray-900">
            <span>Total</span>
            <span class="mk-num">{{ formatCurrency(venta.total) }}</span>
          </div>
        </div>
      </div>

      <p
        v-if="documento.estado === 'ANULADO'"
        class="mt-8 rounded bg-red-50 px-4 py-2 text-center text-sm font-semibold text-red-600"
      >
        DOCUMENTO ANULADO{{ documento.motivoAnulacion ? ` — ${documento.motivoAnulacion}` : '' }}
      </p>
    </div>
  </div>
</template>

<style>
@media print {
  .no-print {
    display: none !important;
  }
  .mk-bg {
    background: white !important;
    padding: 0 !important;
  }
  .factura-papel {
    border: none !important;
    box-shadow: none !important;
    border-radius: 0 !important;
    max-width: 100% !important;
  }
}
</style>
