import { ref } from 'vue'
import { dashboardService } from '@/services/dashboard.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { DashboardGrupoResumen } from '@/types/dashboard'

export function useDashboardGrupo() {
  const resumen = ref<DashboardGrupoResumen | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function cargar(grupoId: number) {
    loading.value = true
    error.value = null
    try {
      resumen.value = await dashboardService.obtenerResumenGrupo(grupoId)
    } catch (err) {
      error.value = err instanceof ApiClientError ? err.message : 'No se pudo cargar el resumen del grupo.'
    } finally {
      loading.value = false
    }
  }

  return { resumen, loading, error, cargar }
}
