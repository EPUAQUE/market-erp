import { describe, it, expect, vi, beforeEach } from 'vitest'
import { API_ENDPOINTS } from '@/config/endpoints'

vi.mock('@/services/http/ApiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const { apiClient } = await import('@/services/http/ApiClient')
const { comprasService } = await import('./compras.service')

describe('comprasService', () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset()
    vi.mocked(apiClient.post).mockReset()
  })

  it('listarPorTienda pide page/size 0-based en la ruta de la tienda', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      contenido: [],
      pagina: 0,
      tamano: 10,
      totalElementos: 0,
      totalPaginas: 0,
    })

    await comprasService.listarPorTienda(1, 0, 10)

    expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.compras.porTienda(1), {
      params: { page: 0, size: 10 },
    })
  })

  it('crear envía proveedorId y líneas a la ruta de la tienda', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({})
    const lineas = [{ productoId: 1, cantidad: '10', costoUnitario: '5.00' }]

    await comprasService.crear(1, 2, lineas)

    expect(apiClient.post).toHaveBeenCalledWith(API_ENDPOINTS.compras.porTienda(1), {
      proveedorId: 2,
      lineas,
    })
  })

  it('recibir y anular pegan a sus endpoints propios de tienda+id', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({})

    await comprasService.recibir(1, 5)
    expect(apiClient.post).toHaveBeenCalledWith(API_ENDPOINTS.compras.recibir(1, 5))

    await comprasService.anular(1, 5)
    expect(apiClient.post).toHaveBeenCalledWith(API_ENDPOINTS.compras.anular(1, 5))
  })
})
