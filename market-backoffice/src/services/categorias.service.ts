import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Categoria } from '@/types/categoria'

class CategoriasService {
  listar() {
    return apiClient.get<Categoria[]>(API_ENDPOINTS.categorias.base)
  }

  crear(nombre: string, imagen?: string) {
    return apiClient.post<Categoria>(API_ENDPOINTS.categorias.base, { nombre, imagen })
  }

  actualizar(id: number, nombre: string, imagen?: string) {
    return apiClient.put<Categoria>(API_ENDPOINTS.categorias.porId(id), { nombre, imagen })
  }

  activar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.categorias.activar(id))
  }

  desactivar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.categorias.desactivar(id))
  }
}

export const categoriasService = new CategoriasService()
