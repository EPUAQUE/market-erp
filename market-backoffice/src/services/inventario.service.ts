import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Inventario, MovimientoInventario, TipoMovimiento } from '@/types/inventario'
import type { Pagina } from '@/types/pagina'

export interface DatosMovimiento {
  productoId: number
  cantidad: string
  costoUnitario: string
  tipoMovimiento: TipoMovimiento
}

class InventarioService {
  listarPorTienda(tiendaId: number, pagina: number, tamano: number) {
    return apiClient.get<Pagina<Inventario>>(API_ENDPOINTS.inventario.porTienda(tiendaId), {
      params: { page: pagina, size: tamano },
    })
  }

  listarMovimientos(tiendaId: number, productoId: number, pagina: number, tamano: number) {
    return apiClient.get<Pagina<MovimientoInventario>>(API_ENDPOINTS.inventario.movimientos(tiendaId, productoId), {
      params: { page: pagina, size: tamano },
    })
  }

  registrarMovimiento(tiendaId: number, datos: DatosMovimiento) {
    return apiClient.post<Inventario>(API_ENDPOINTS.inventario.registrarMovimiento(tiendaId), datos)
  }
}

export const inventarioService = new InventarioService()
