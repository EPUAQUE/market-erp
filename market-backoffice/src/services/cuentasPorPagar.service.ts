import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { CuentaPorPagar } from '@/types/cuentaPorPagar'

class CuentasPorPagarService {
  listarPorTienda(tiendaId: number) {
    return apiClient.get<CuentaPorPagar[]>(API_ENDPOINTS.cuentasPorPagar.porTienda(tiendaId))
  }

  registrarPago(tiendaId: number, id: number, monto: string) {
    return apiClient.post<CuentaPorPagar>(API_ENDPOINTS.cuentasPorPagar.pagos(tiendaId, id), { monto })
  }

  anular(tiendaId: number, id: number) {
    return apiClient.post<CuentaPorPagar>(API_ENDPOINTS.cuentasPorPagar.anular(tiendaId, id))
  }
}

export const cuentasPorPagarService = new CuentasPorPagarService()
