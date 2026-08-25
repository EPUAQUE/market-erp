import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Traslado } from '@/types/traslado'
import type { Pagina } from '@/types/pagina'

export interface DatosLineaTraslado {
  productoId: number
  cantidad: string
}

class TrasladosService {
  listar(pagina: number, tamano: number) {
    return apiClient.get<Pagina<Traslado>>(API_ENDPOINTS.traslados.base, { params: { page: pagina, size: tamano } })
  }

  crear(tiendaOrigenId: number, tiendaDestinoId: number, lineas: DatosLineaTraslado[]) {
    return apiClient.post<Traslado>(API_ENDPOINTS.traslados.base, { tiendaOrigenId, tiendaDestinoId, lineas })
  }

  completar(id: number) {
    return apiClient.post<Traslado>(API_ENDPOINTS.traslados.completar(id))
  }

  anular(id: number) {
    return apiClient.post<Traslado>(API_ENDPOINTS.traslados.anular(id))
  }
}

export const trasladosService = new TrasladosService()
