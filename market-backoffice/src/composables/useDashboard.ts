import { ref } from 'vue'
import { dashboardService } from '@/services/dashboard.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { DashboardResumen } from '@/types/dashboard'

export function useDashboard() {
  const resumen = ref<DashboardResumen | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function cargar(tiendaId: number) {
    loading.value = true
    error.value = null
    try {
      resumen.value = await dashboardService.obtenerResumen(tiendaId)
    } catch (err) {
      error.value = err instanceof ApiClientError ? err.message : 'No se pudo cargar el resumen.'
    } finally {
      loading.value = false
    }
  }

  return { resumen, loading, error, cargar }
}
