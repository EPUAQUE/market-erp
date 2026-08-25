import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class TiendasService {
    listar() {
        return apiClient.get(API_ENDPOINTS.tiendas.base);
    }
    crear(codigo, datos) {
        return apiClient.post(API_ENDPOINTS.tiendas.base, { codigo, ...datos });
    }
    actualizar(id, datos) {
        return apiClient.put(API_ENDPOINTS.tiendas.porId(id), datos);
    }
    activar(id) {
        return apiClient.post(API_ENDPOINTS.tiendas.activar(id));
    }
    desactivar(id) {
        return apiClient.post(API_ENDPOINTS.tiendas.desactivar(id));
    }
}
export const tiendasService = new TiendasService();
