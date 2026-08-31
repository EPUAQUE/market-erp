import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { CajaSesion, TipoMovimientoCaja } from '@/types/caja'
import type { Pagina } from '@/types/pagina'

export interface DatosMovimientoCaja {
  tipo: TipoMovimientoCaja
  concepto: string
  monto: string
}

class CajaService {
  listarPorTienda(tiendaId: number, pagina: number, tamano: number, signal?: AbortSignal) {
    return apiClient.get<Pagina<CajaSesion>>(API_ENDPOINTS.caja.porTienda(tiendaId), {
      params: { page: pagina, size: tamano },
      signal,
    })
  }

  obtenerAbierta(tiendaId: number) {
    return apiClient.get<CajaSesion>(API_ENDPOINTS.caja.abierta(tiendaId))
  }

  abrir(tiendaId: number, montoInicial: string) {
    return apiClient.post<CajaSesion>(API_ENDPOINTS.caja.abrir(tiendaId), { montoInicial })
  }

  registrarMovimiento(tiendaId: number, datos: DatosMovimientoCaja) {
    return apiClient.post<CajaSesion>(API_ENDPOINTS.caja.movimientos(tiendaId), datos)
  }

  cerrar(tiendaId: number, montoFinalContado: string) {
    return apiClient.post<CajaSesion>(API_ENDPOINTS.caja.cerrar(tiendaId), { montoFinalContado })
  }
}

export const cajaService = new CajaService()
