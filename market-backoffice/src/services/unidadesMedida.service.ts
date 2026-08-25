import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { UnidadMedida } from '@/types/unidadMedida'

class UnidadesMedidaService {
  listar() {
    return apiClient.get<UnidadMedida[]>(API_ENDPOINTS.unidadesMedida.base)
  }

  crear(nombre: string, abreviacion: string) {
    return apiClient.post<UnidadMedida>(API_ENDPOINTS.unidadesMedida.base, { nombre, abreviacion })
  }

  actualizar(id: number, nombre: string, abreviacion: string) {
    return apiClient.put<UnidadMedida>(API_ENDPOINTS.unidadesMedida.porId(id), { nombre, abreviacion })
  }
}

export const unidadesMedidaService = new UnidadesMedidaService()
