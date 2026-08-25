import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class UsuariosService {
    listar() {
        return apiClient.get(API_ENDPOINTS.usuarios.base);
    }
    crear(username, password) {
        return apiClient.post(API_ENDPOINTS.usuarios.base, { username, password });
    }
    asignarTienda(usuarioId, tiendaId, rolId) {
        return apiClient.post(API_ENDPOINTS.usuarios.asignarTienda(usuarioId), { tiendaId, rolId });
    }
}
export const usuariosService = new UsuariosService();
