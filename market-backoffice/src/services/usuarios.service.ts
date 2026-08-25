import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Usuario } from '@/types/usuario'

class UsuariosService {
  listar() {
    return apiClient.get<Usuario[]>(API_ENDPOINTS.usuarios.base)
  }

  crear(username: string, password: string) {
    return apiClient.post<Usuario>(API_ENDPOINTS.usuarios.base, { username, password })
  }

  asignarTienda(usuarioId: number, tiendaId: number, rolId: number) {
    return apiClient.post<void>(API_ENDPOINTS.usuarios.asignarTienda(usuarioId), { tiendaId, rolId })
  }
}

export const usuariosService = new UsuariosService()
