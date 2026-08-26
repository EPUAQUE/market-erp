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
const { clientesService } = await import('./clientes.service')

describe('clientesService', () => {
  beforeEach(() => {
    vi.mocked(apiClient.get).mockReset()
    vi.mocked(apiClient.post).mockReset()
    vi.mocked(apiClient.put).mockReset()
  })

  it('listar pide page/size 0-based al backend', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      contenido: [],
      pagina: 0,
      tamano: 10,
      totalElementos: 0,
      totalPaginas: 0,
    })

    await clientesService.listar(0, 10)

    expect(apiClient.get).toHaveBeenCalledWith(API_ENDPOINTS.clientes.base, {
      params: { page: 0, size: 10 },
    })
  })

  it('crear envía el nit junto con los datos del cliente', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({})

    await clientesService.crear('12345678-9', { nombre: 'Juan Pérez' })

    expect(apiClient.post).toHaveBeenCalledWith(API_ENDPOINTS.clientes.base, {
      nit: '12345678-9',
      nombre: 'Juan Pérez',
    })
  })

  it('crear sin nit lo envía como undefined (Consumidor Final)', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({})

    await clientesService.crear(undefined, { nombre: 'Consumidor Final' })

    expect(apiClient.post).toHaveBeenCalledWith(API_ENDPOINTS.clientes.base, {
      nit: undefined,
      nombre: 'Consumidor Final',
    })
  })

  it('actualizar pega al endpoint del id', async () => {
    vi.mocked(apiClient.put).mockResolvedValue({})

    await clientesService.actualizar(5, { nombre: 'Nuevo nombre' })

    expect(apiClient.put).toHaveBeenCalledWith(API_ENDPOINTS.clientes.porId(5), { nombre: 'Nuevo nombre' })
  })

  it('activar y desactivar pegan a sus endpoints propios', async () => {
    vi.mocked(apiClient.post).mockResolvedValue(undefined)

    await clientesService.activar(7)
    expect(apiClient.post).toHaveBeenCalledWith(API_ENDPOINTS.clientes.activar(7))

    await clientesService.desactivar(7)
    expect(apiClient.post).toHaveBeenCalledWith(API_ENDPOINTS.clientes.desactivar(7))
  })
})
