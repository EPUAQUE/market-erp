import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class ProductosService {
    listar(pagina, tamano) {
        return apiClient.get(API_ENDPOINTS.productos.base, { params: { page: pagina, size: tamano } });
    }
    crear(codigoInterno, datos) {
        return apiClient.post(API_ENDPOINTS.productos.base, { codigoInterno, ...datos });
    }
    actualizar(id, datos) {
        return apiClient.put(API_ENDPOINTS.productos.porId(id), datos);
    }
    activar(id) {
        return apiClient.post(API_ENDPOINTS.productos.activar(id));
    }
    desactivar(id) {
        return apiClient.post(API_ENDPOINTS.productos.desactivar(id));
    }
    listarTiendas(productoId) {
        return apiClient.get(API_ENDPOINTS.productos.tiendas(productoId));
    }
    asignarTienda(productoId, tiendaId, datos) {
        return apiClient.post(API_ENDPOINTS.productos.tiendas(productoId), { tiendaId, ...datos });
    }
    actualizarTienda(productoId, id, datos) {
        return apiClient.put(API_ENDPOINTS.productos.tiendaPorId(productoId, id), datos);
    }
    activarTienda(productoId, id) {
        return apiClient.post(API_ENDPOINTS.productos.tiendaActivar(productoId, id));
    }
    desactivarTienda(productoId, id) {
        return apiClient.post(API_ENDPOINTS.productos.tiendaDesactivar(productoId, id));
    }
}
export const productosService = new ProductosService();
