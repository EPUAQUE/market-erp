import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Rol } from '@/types/rol'

class RolesService {
  listar() {
    return apiClient.get<Rol[]>(API_ENDPOINTS.roles.base)
  }
}

export const rolesService = new RolesService()
