import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class CuentasPorPagarService {
    listarPorTienda(tiendaId) {
        return apiClient.get(API_ENDPOINTS.cuentasPorPagar.porTienda(tiendaId));
    }
    registrarPago(tiendaId, id, monto) {
        return apiClient.post(API_ENDPOINTS.cuentasPorPagar.pagos(tiendaId, id), { monto });
    }
    anular(tiendaId, id) {
        return apiClient.post(API_ENDPOINTS.cuentasPorPagar.anular(tiendaId, id));
    }
}
export const cuentasPorPagarService = new CuentasPorPagarService();
