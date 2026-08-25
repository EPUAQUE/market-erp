import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class MarcasService {
    listar() {
        return apiClient.get(API_ENDPOINTS.marcas.base);
    }
    crear(nombre) {
        return apiClient.post(API_ENDPOINTS.marcas.base, { nombre });
    }
    actualizar(id, nombre) {
        return apiClient.put(API_ENDPOINTS.marcas.porId(id), { nombre });
    }
}
export const marcasService = new MarcasService();
