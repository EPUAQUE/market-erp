import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class TrasladosService {
    listar(pagina, tamano) {
        return apiClient.get(API_ENDPOINTS.traslados.base, { params: { page: pagina, size: tamano } });
    }
    crear(tiendaOrigenId, tiendaDestinoId, lineas) {
        return apiClient.post(API_ENDPOINTS.traslados.base, { tiendaOrigenId, tiendaDestinoId, lineas });
    }
    completar(id) {
        return apiClient.post(API_ENDPOINTS.traslados.completar(id));
    }
    anular(id) {
        return apiClient.post(API_ENDPOINTS.traslados.anular(id));
    }
}
export const trasladosService = new TrasladosService();
