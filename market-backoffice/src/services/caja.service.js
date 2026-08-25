import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class CajaService {
    listarPorTienda(tiendaId, pagina, tamano) {
        return apiClient.get(API_ENDPOINTS.caja.porTienda(tiendaId), {
            params: { page: pagina, size: tamano },
        });
    }
    obtenerAbierta(tiendaId) {
        return apiClient.get(API_ENDPOINTS.caja.abierta(tiendaId));
    }
    abrir(tiendaId, montoInicial) {
        return apiClient.post(API_ENDPOINTS.caja.abrir(tiendaId), { montoInicial });
    }
    registrarMovimiento(tiendaId, datos) {
        return apiClient.post(API_ENDPOINTS.caja.movimientos(tiendaId), datos);
    }
    cerrar(tiendaId, montoFinalContado) {
        return apiClient.post(API_ENDPOINTS.caja.cerrar(tiendaId), { montoFinalContado });
    }
}
export const cajaService = new CajaService();
