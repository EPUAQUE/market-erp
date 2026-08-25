import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class InventarioService {
    listarPorTienda(tiendaId, pagina, tamano) {
        return apiClient.get(API_ENDPOINTS.inventario.porTienda(tiendaId), {
            params: { page: pagina, size: tamano },
        });
    }
    listarMovimientos(tiendaId, productoId, pagina, tamano) {
        return apiClient.get(API_ENDPOINTS.inventario.movimientos(tiendaId, productoId), {
            params: { page: pagina, size: tamano },
        });
    }
    registrarMovimiento(tiendaId, datos) {
        return apiClient.post(API_ENDPOINTS.inventario.registrarMovimiento(tiendaId), datos);
    }
}
export const inventarioService = new InventarioService();
