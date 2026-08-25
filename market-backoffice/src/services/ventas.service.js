import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class VentasService {
    listarPorTienda(tiendaId, pagina, tamano) {
        return apiClient.get(API_ENDPOINTS.ventas.porTienda(tiendaId), {
            params: { page: pagina, size: tamano },
        });
    }
    crear(tiendaId, clienteId, lineas) {
        return apiClient.post(API_ENDPOINTS.ventas.porTienda(tiendaId), { clienteId, lineas });
    }
    completar(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.ventas.completar(tiendaId, id));
    }
    anular(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.ventas.anular(tiendaId, id));
    }
}
export const ventasService = new VentasService();
