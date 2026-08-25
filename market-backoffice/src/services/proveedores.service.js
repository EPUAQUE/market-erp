import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class ProveedoresService {
    listar() {
        return apiClient.get(API_ENDPOINTS.proveedores.base);
    }
    crear(nit, datos) {
        return apiClient.post(API_ENDPOINTS.proveedores.base, { nit, ...datos });
    }
    actualizar(id, datos) {
        return apiClient.put(API_ENDPOINTS.proveedores.porId(id), datos);
    }
    activar(id) {
        return apiClient.post(API_ENDPOINTS.proveedores.activar(id));
    }
    desactivar(id) {
        return apiClient.post(API_ENDPOINTS.proveedores.desactivar(id));
    }
}
export const proveedoresService = new ProveedoresService();
