import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class UnidadesMedidaService {
    listar() {
        return apiClient.get(API_ENDPOINTS.unidadesMedida.base);
    }
    crear(nombre, abreviacion) {
        return apiClient.post(API_ENDPOINTS.unidadesMedida.base, { nombre, abreviacion });
    }
    actualizar(id, nombre, abreviacion) {
        return apiClient.put(API_ENDPOINTS.unidadesMedida.porId(id), { nombre, abreviacion });
    }
}
export const unidadesMedidaService = new UnidadesMedidaService();
