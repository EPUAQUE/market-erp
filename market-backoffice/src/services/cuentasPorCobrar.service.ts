import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { CuentaPorCobrar } from '@/types/cuentaPorCobrar'
import type { Pagina } from '@/types/pagina'

class CuentasPorCobrarService {
  listarPorTienda(tiendaId: number, pagina: number, tamano: number, signal?: AbortSignal) {
    return apiClient.get<Pagina<CuentaPorCobrar>>(API_ENDPOINTS.cuentasPorCobrar.porTienda(tiendaId), {
      params: { page: pagina, size: tamano },
      signal,
    })
  }

  registrarCobro(tiendaId: number, id: number, monto: string) {
    return apiClient.post<CuentaPorCobrar>(API_ENDPOINTS.cuentasPorCobrar.cobros(tiendaId, id), { monto })
  }

  anular(tiendaId: number, id: number) {
    return apiClient.post<CuentaPorCobrar>(API_ENDPOINTS.cuentasPorCobrar.anular(tiendaId, id))
  }
}

export const cuentasPorCobrarService = new CuentasPorCobrarService()
