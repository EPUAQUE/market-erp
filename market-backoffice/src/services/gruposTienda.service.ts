import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { GrupoTienda } from '@/types/grupoTienda'

class GruposTiendaService {
  listar() {
    return apiClient.get<GrupoTienda[]>(API_ENDPOINTS.gruposTienda.base)
  }
}

export const gruposTiendaService = new GruposTiendaService()
