import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Producto, ProductoTienda } from '@/types/producto'
import type { Pagina } from '@/types/pagina'

export interface DatosProducto {
  codigoBarras?: string
  nombre: string
  descripcion?: string
  categoriaId: number
  marcaId: number
  unidadMedidaId: number
  imagenUrl?: string
}

export interface DatosProductoTienda {
  precioVenta: string
  stockMinimo: string
  stockMaximo: string
  permitirVenta: boolean
  permitirIngreso: boolean
}

class ProductosService {
  listar(pagina: number, tamano: number) {
    return apiClient.get<Pagina<Producto>>(API_ENDPOINTS.productos.base, { params: { page: pagina, size: tamano } })
  }

  crear(codigoInterno: string, datos: DatosProducto) {
    return apiClient.post<Producto>(API_ENDPOINTS.productos.base, { codigoInterno, ...datos })
  }

  actualizar(id: number, datos: DatosProducto) {
    return apiClient.put<Producto>(API_ENDPOINTS.productos.porId(id), datos)
  }

  subirImagen(id: number, archivo: File) {
    const formData = new FormData()
    formData.append('archivo', archivo)
    return apiClient.post<Producto>(API_ENDPOINTS.productos.imagen(id), formData)
  }

  activar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.productos.activar(id))
  }

  desactivar(id: number) {
    return apiClient.post<void>(API_ENDPOINTS.productos.desactivar(id))
  }

  listarTiendas(productoId: number) {
    return apiClient.get<ProductoTienda[]>(API_ENDPOINTS.productos.tiendas(productoId))
  }

  listarPorTienda(tiendaId: number, tamano = 5000) {
    return apiClient.get<Pagina<ProductoTienda>>(API_ENDPOINTS.productos.porTienda(tiendaId), {
      params: { page: 0, size: tamano },
    })
  }

  asignarTienda(productoId: number, tiendaId: number, datos: DatosProductoTienda) {
    return apiClient.post<ProductoTienda>(API_ENDPOINTS.productos.tiendas(productoId), { tiendaId, ...datos })
  }

  actualizarTienda(productoId: number, id: number, datos: DatosProductoTienda) {
    return apiClient.put<ProductoTienda>(API_ENDPOINTS.productos.tiendaPorId(productoId, id), datos)
  }

  activarTienda(productoId: number, id: number) {
    return apiClient.post<void>(API_ENDPOINTS.productos.tiendaActivar(productoId, id))
  }

  desactivarTienda(productoId: number, id: number) {
    return apiClient.post<void>(API_ENDPOINTS.productos.tiendaDesactivar(productoId, id))
  }
}

export const productosService = new ProductosService()
