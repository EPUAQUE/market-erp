import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class CuentasPorCobrarService {
    listarPorTienda(tiendaId, pagina, tamano) {
        return apiClient.get(API_ENDPOINTS.cuentasPorCobrar.porTienda(tiendaId), {
            params: { page: pagina, size: tamano },
        });
    }
    registrarCobro(tiendaId, id, monto) {
        return apiClient.post(API_ENDPOINTS.cuentasPorCobrar.cobros(tiendaId, id), { monto });
    }
    anular(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.cuentasPorCobrar.anular(tiendaId, id));
    }
}
export const cuentasPorCobrarService = new CuentasPorCobrarService();
