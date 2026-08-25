import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class ComprasService {
    listarPorTienda(tiendaId) {
        return apiClient.get(API_ENDPOINTS.compras.porTienda(tiendaId));
    }
    crear(tiendaId, proveedorId, lineas) {
        return apiClient.post(API_ENDPOINTS.compras.porTienda(tiendaId), { proveedorId, lineas });
    }
    recibir(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.compras.recibir(tiendaId, id));
    }
    anular(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.compras.anular(tiendaId, id));
    }
}
export const comprasService = new ComprasService();
