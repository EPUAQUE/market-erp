import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { CuentaPorPagar } from '@/types/cuentaPorPagar'
import type { Pagina } from '@/types/pagina'

class CuentasPorPagarService {
  listarPorTienda(tiendaId: number, pagina: number, tamano: number, signal?: AbortSignal) {
    return apiClient.get<Pagina<CuentaPorPagar>>(API_ENDPOINTS.cuentasPorPagar.porTienda(tiendaId), {
      params: { page: pagina, size: tamano },
      signal,
    })
  }

  registrarPago(tiendaId: number, id: number, monto: string) {
    return apiClient.post<CuentaPorPagar>(API_ENDPOINTS.cuentasPorPagar.pagos(tiendaId, id), { monto })
  }

  anular(tiendaId: number, id: number) {
    return apiClient.post<CuentaPorPagar>(API_ENDPOINTS.cuentasPorPagar.anular(tiendaId, id))
  }
}

export const cuentasPorPagarService = new CuentasPorPagarService()
