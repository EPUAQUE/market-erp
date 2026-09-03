import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Marca } from '@/types/marca'

class MarcasService {
  listar() {
    return apiClient.get<Marca[]>(API_ENDPOINTS.marcas.base)
  }

  crear(nombre: string) {
    return apiClient.post<Marca>(API_ENDPOINTS.marcas.base, { nombre })
  }

  actualizar(id: number, nombre: string) {
    return apiClient.put<Marca>(API_ENDPOINTS.marcas.porId(id), { nombre })
  }

  activar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.marcas.activar(id))
  }

  desactivar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.marcas.desactivar(id))
  }
}

export const marcasService = new MarcasService()
