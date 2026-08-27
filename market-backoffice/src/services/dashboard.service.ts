import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { DashboardGrupoResumen, DashboardResumen } from '@/types/dashboard'

class DashboardService {
  obtenerResumen(tiendaId: number) {
    return apiClient.get<DashboardResumen>(API_ENDPOINTS.dashboard.porTienda(tiendaId))
  }

  obtenerResumenGrupo(grupoId: number) {
    return apiClient.get<DashboardGrupoResumen>(API_ENDPOINTS.dashboard.porGrupo(grupoId))
  }
}

export const dashboardService = new DashboardService()
