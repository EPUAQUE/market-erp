import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Usuario, UsuarioGrupo, UsuarioTienda } from '@/types/usuario'

class UsuariosService {
  listar() {
    return apiClient.get<Usuario[]>(API_ENDPOINTS.usuarios.base)
  }

  crear(username: string, password: string, nombre: string, telefono: string, correo: string) {
    return apiClient.post<Usuario>(API_ENDPOINTS.usuarios.base, {
      username,
      password,
      nombre,
      telefono,
      correo,
    })
  }

  listarTiendas(usuarioId: number) {
    return apiClient.get<UsuarioTienda[]>(API_ENDPOINTS.usuarios.tiendas(usuarioId))
  }

  asignarTienda(usuarioId: number, tiendaId: number, rolId: number) {
    return apiClient.post<void>(API_ENDPOINTS.usuarios.tiendas(usuarioId), { tiendaId, rolId })
  }

  listarGrupos(usuarioId: number) {
    return apiClient.get<UsuarioGrupo[]>(API_ENDPOINTS.usuarios.grupos(usuarioId))
  }

  asignarGrupo(usuarioId: number, grupoTiendaId: number, rolId: number) {
    return apiClient.post<void>(API_ENDPOINTS.usuarios.grupos(usuarioId), { grupoTiendaId, rolId })
  }
}

export const usuariosService = new UsuariosService()
