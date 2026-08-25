import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class ReportesService {
    reporteVentas(tiendaId, desde, hasta) {
        return apiClient.get(API_ENDPOINTS.reportes.ventas(tiendaId), { params: { desde, hasta } });
    }
    reporteCompras(tiendaId, desde, hasta) {
        return apiClient.get(API_ENDPOINTS.reportes.compras(tiendaId), { params: { desde, hasta } });
    }
}
export const reportesService = new ReportesService();
