import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Cliente } from '@/types/cliente'
import type { Pagina } from '@/types/pagina'

export interface DatosCliente {
  nombre: string
  direccion?: string
  telefono?: string
  correo?: string
}

class ClientesService {
  listar(pagina: number, tamano: number) {
    return apiClient.get<Pagina<Cliente>>(API_ENDPOINTS.clientes.base, { params: { page: pagina, size: tamano } })
  }

  crear(nit: string | undefined, datos: DatosCliente) {
    return apiClient.post<Cliente>(API_ENDPOINTS.clientes.base, { nit, ...datos })
  }

  actualizar(id: number, datos: DatosCliente) {
    return apiClient.put<Cliente>(API_ENDPOINTS.clientes.porId(id), datos)
  }

  activar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.clientes.activar(id))
  }

  desactivar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.clientes.desactivar(id))
  }
}

export const clientesService = new ClientesService()
