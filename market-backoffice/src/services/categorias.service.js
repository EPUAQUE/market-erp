import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class CategoriasService {
    listar() {
        return apiClient.get(API_ENDPOINTS.categorias.base);
    }
    crear(nombre, imagen) {
        return apiClient.post(API_ENDPOINTS.categorias.base, { nombre, imagen });
    }
    actualizar(id, nombre, imagen) {
        return apiClient.put(API_ENDPOINTS.categorias.porId(id), { nombre, imagen });
    }
    activar(id) {
        return apiClient.post(API_ENDPOINTS.categorias.activar(id));
    }
    desactivar(id) {
        return apiClient.post(API_ENDPOINTS.categorias.desactivar(id));
    }
}
export const categoriasService = new CategoriasService();
