import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class DashboardService {
    obtenerResumen(tiendaId) {
        return apiClient.get(API_ENDPOINTS.dashboard.porTienda(tiendaId));
    }
}
export const dashboardService = new DashboardService();
