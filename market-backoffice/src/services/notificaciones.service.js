import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class NotificacionesService {
    listarPorTienda(tiendaId) {
        return apiClient.get(API_ENDPOINTS.notificaciones.porTienda(tiendaId));
    }
    listarNoLeidas(tiendaId) {
        return apiClient.get(API_ENDPOINTS.notificaciones.noLeidas(tiendaId));
    }
    generar(tiendaId) {
        return apiClient.post(API_ENDPOINTS.notificaciones.generar(tiendaId));
    }
    marcarLeida(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.notificaciones.marcarLeida(tiendaId, id));
    }
}
export const notificacionesService = new NotificacionesService();
