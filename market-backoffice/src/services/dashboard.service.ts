import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { DashboardResumen } from '@/types/dashboard'

class DashboardService {
  obtenerResumen(tiendaId: number) {
    return apiClient.get<DashboardResumen>(API_ENDPOINTS.dashboard.porTienda(tiendaId))
  }
}

export const dashboardService = new DashboardService()
