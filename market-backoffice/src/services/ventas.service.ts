import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { MetodoPago, Venta } from '@/types/venta'
import type { Pagina } from '@/types/pagina'

export interface DatosLineaVenta {
  productoId: number
  cantidad: string
  precioUnitario: string
}

class VentasService {
  listarPorTienda(tiendaId: number, pagina: number, tamano: number, signal?: AbortSignal) {
    return apiClient.get<Pagina<Venta>>(API_ENDPOINTS.ventas.porTienda(tiendaId), {
      params: { page: pagina, size: tamano },
      signal,
    })
  }

  crear(
    tiendaId: number,
    clienteId: number,
    lineas: DatosLineaVenta[],
    metodoPago: MetodoPago,
    correlationId: string,
  ) {
    return apiClient.post<Venta>(API_ENDPOINTS.ventas.porTienda(tiendaId), {
      clienteId,
      lineas,
      metodoPago,
      correlationId,
    })
  }

  completar(tiendaId: number, id: number) {
    return apiClient.post<Venta>(API_ENDPOINTS.ventas.completar(tiendaId, id))
  }

  anular(tiendaId: number, id: number) {
    return apiClient.post<Venta>(API_ENDPOINTS.ventas.anular(tiendaId, id))
  }
}

export const ventasService = new VentasService()
