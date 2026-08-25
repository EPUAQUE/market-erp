import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class GastosProgramadosService {
    listarPorTienda(tiendaId) {
        return apiClient.get(API_ENDPOINTS.gastosProgramados.porTienda(tiendaId));
    }
    crear(tiendaId, datos) {
        return apiClient.post(API_ENDPOINTS.gastosProgramados.porTienda(tiendaId), datos);
    }
    actualizar(tiendaId, id, datos) {
        return apiClient.put(API_ENDPOINTS.gastosProgramados.porId(tiendaId, id), datos);
    }
    activar(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.gastosProgramados.activar(tiendaId, id));
    }
    desactivar(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.gastosProgramados.desactivar(tiendaId, id));
    }
    generarPago(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.gastosProgramados.generarPago(tiendaId, id));
    }
}
export const gastosProgramadosService = new GastosProgramadosService();
