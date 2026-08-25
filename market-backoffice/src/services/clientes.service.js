import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class ClientesService {
    listar() {
        return apiClient.get(API_ENDPOINTS.clientes.base);
    }
    crear(nit, datos) {
        return apiClient.post(API_ENDPOINTS.clientes.base, { nit, ...datos });
    }
    actualizar(id, datos) {
        return apiClient.put(API_ENDPOINTS.clientes.porId(id), datos);
    }
    activar(id) {
        return apiClient.post(API_ENDPOINTS.clientes.activar(id));
    }
    desactivar(id) {
        return apiClient.post(API_ENDPOINTS.clientes.desactivar(id));
    }
}
export const clientesService = new ClientesService();
