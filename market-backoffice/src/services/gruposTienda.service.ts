import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { GrupoTienda } from '@/types/grupoTienda'

class GruposTiendaService {
  listar() {
    return apiClient.get<GrupoTienda[]>(API_ENDPOINTS.gruposTienda.base)
  }

  crear(codigo: string, nombre: string) {
    return apiClient.post<GrupoTienda>(API_ENDPOINTS.gruposTienda.base, { codigo, nombre })
  }

  actualizar(id: number, nombre: string) {
    return apiClient.put<GrupoTienda>(API_ENDPOINTS.gruposTienda.porId(id), { nombre })
  }

  activar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.gruposTienda.activar(id))
  }

  desactivar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.gruposTienda.desactivar(id))
  }
}

export const gruposTiendaService = new GruposTiendaService()
