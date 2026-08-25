import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Tienda } from '@/types/tienda'

export interface DatosTienda {
  nombre: string
  direccion?: string
  telefono?: string
  correo?: string
}

class TiendasService {
  listar() {
    return apiClient.get<Tienda[]>(API_ENDPOINTS.tiendas.base)
  }

  crear(codigo: string, datos: DatosTienda) {
    return apiClient.post<Tienda>(API_ENDPOINTS.tiendas.base, { codigo, ...datos })
  }

  actualizar(id: number, datos: DatosTienda) {
    return apiClient.put<Tienda>(API_ENDPOINTS.tiendas.porId(id), datos)
  }

  activar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.tiendas.activar(id))
  }

  desactivar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.tiendas.desactivar(id))
  }
}

export const tiendasService = new TiendasService()
