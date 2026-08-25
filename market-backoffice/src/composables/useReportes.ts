import { ref } from 'vue'
import { reportesService } from '@/services/reportes.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { ReporteCompras, ReporteVentas } from '@/types/reporte'

export function useReportes() {
  const reporteVentas = ref<ReporteVentas | null>(null)
  const reporteCompras = ref<ReporteCompras | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function generarReporteVentas(tiendaId: number, desde: string, hasta: string) {
    loading.value = true
    error.value = null
    reporteCompras.value = null
    try {
      reporteVentas.value = await reportesService.reporteVentas(tiendaId, desde, hasta)
    } catch (err) {
      error.value = err instanceof ApiClientError ? err.message : 'No se pudo generar el reporte.'
    } finally {
      loading.value = false
    }
  }

  async function generarReporteCompras(tiendaId: number, desde: string, hasta: string) {
    loading.value = true
    error.value = null
    reporteVentas.value = null
    try {
      reporteCompras.value = await reportesService.reporteCompras(tiendaId, desde, hasta)
    } catch (err) {
      error.value = err instanceof ApiClientError ? err.message : 'No se pudo generar el reporte.'
    } finally {
      loading.value = false
    }
  }

  return { reporteVentas, reporteCompras, loading, error, generarReporteVentas, generarReporteCompras }
}
