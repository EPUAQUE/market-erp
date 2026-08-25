import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class FelService {
    listarPorTienda(tiendaId) {
        return apiClient.get(API_ENDPOINTS.fel.porTienda(tiendaId));
    }
    emitir(tiendaId, ventaId) {
        return apiClient.post(API_ENDPOINTS.fel.emitir(tiendaId, ventaId));
    }
    reintentar(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.fel.reintentar(tiendaId, id));
    }
    anular(tiendaId, id, motivo) {
        return apiClient.post(API_ENDPOINTS.fel.anular(tiendaId, id), { motivo });
    }
}
export const felService = new FelService();
