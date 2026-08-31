import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { ReporteCompras, ReporteVentas } from '@/types/reporte'

class ReportesService {
  reporteVentas(tiendaId: number, desde: string, hasta: string) {
    return apiClient.get<ReporteVentas>(API_ENDPOINTS.reportes.ventas(tiendaId), { params: { desde, hasta } })
  }

  reporteCompras(tiendaId: number, desde: string, hasta: string) {
    return apiClient.get<ReporteCompras>(API_ENDPOINTS.reportes.compras(tiendaId), {
      params: { desde, hasta },
    })
  }
}

export const reportesService = new ReportesService()
