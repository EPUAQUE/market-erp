import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Compra } from '@/types/compra'

export interface DatosLineaCompra {
  productoId: number
  cantidad: string
  costoUnitario: string
}

class ComprasService {
  listarPorTienda(tiendaId: number) {
    return apiClient.get<Compra[]>(API_ENDPOINTS.compras.porTienda(tiendaId))
  }

  crear(tiendaId: number, proveedorId: number, lineas: DatosLineaCompra[]) {
    return apiClient.post<Compra>(API_ENDPOINTS.compras.porTienda(tiendaId), { proveedorId, lineas })
  }

  recibir(tiendaId: number, id: number) {
    return apiClient.post<Compra>(API_ENDPOINTS.compras.recibir(tiendaId, id))
  }

  anular(tiendaId: number, id: number) {
    return apiClient.post<Compra>(API_ENDPOINTS.compras.anular(tiendaId, id))
  }
}

export const comprasService = new ComprasService()
