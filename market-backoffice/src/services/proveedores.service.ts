import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Proveedor } from '@/types/proveedor'

export interface DatosProveedor {
  nombre: string
  direccion?: string
  telefono?: string
  correo?: string
}

class ProveedoresService {
  listar() {
    return apiClient.get<Proveedor[]>(API_ENDPOINTS.proveedores.base)
  }

  crear(nit: string, datos: DatosProveedor) {
    return apiClient.post<Proveedor>(API_ENDPOINTS.proveedores.base, { nit, ...datos })
  }

  actualizar(id: number, datos: DatosProveedor) {
    return apiClient.put<Proveedor>(API_ENDPOINTS.proveedores.porId(id), datos)
  }

  activar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.proveedores.activar(id))
  }

  desactivar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.proveedores.desactivar(id))
  }
}

export const proveedoresService = new ProveedoresService()
