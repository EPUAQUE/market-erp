import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Compra } from '@/types/compra'

vi.mock('@/services/compras.service', () => ({
  comprasService: {
    listarPorTienda: vi.fn(),
    crear: vi.fn(),
    recibir: vi.fn(),
    anular: vi.fn(),
  },
}))

const { comprasService } = await import('@/services/compras.service')
const { useCompras } = await import('./useCompras')

function compra(overrides: Partial<Compra> = {}): Compra {
  return {
    id: 1,
    proveedorId: 2,
    tiendaId: 1,
    fecha: '2026-01-01T00:00:00Z',
    estado: 'BORRADOR',
    lineas: [{ id: 1, productoId: 10, cantidad: '10', costoUnitario: '5.00' }],
    total: '50.00',
    ...overrides,
  }
}

describe('useCompras', () => {
  beforeEach(() => {
    vi.mocked(comprasService.listarPorTienda).mockReset()
    vi.mocked(comprasService.crear).mockReset()
    vi.mocked(comprasService.recibir).mockReset()
    vi.mocked(comprasService.anular).mockReset()
  })

  it('cargar convierte pagina 1-based a 0-based y pasa la tienda', async () => {
    vi.mocked(comprasService.listarPorTienda).mockResolvedValue({
      contenido: [compra()],
      pagina: 1,
      tamano: 10,
      totalElementos: 15,
      totalPaginas: 2,
    })
    const { pagina, totalElementos, totalPaginas, items, cargar } = useCompras()
    pagina.value = 2

    await cargar(1)

    expect(comprasService.listarPorTienda).toHaveBeenCalledWith(1, 1, 10, expect.any(AbortSignal))
    expect(items.value).toHaveLength(1)
    expect(totalElementos.value).toBe(15)
    expect(totalPaginas.value).toBe(2)
  })

  it('cargar con error de API guarda el mensaje en listError', async () => {
    vi.mocked(comprasService.listarPorTienda).mockRejectedValue(
      new ApiClientError({ message: 'No autorizado', status: 403, code: 'FORBIDDEN' }),
    )
    const { listError, cargar } = useCompras()

    await cargar(1)

    expect(listError.value).toBe('No autorizado')
  })

  it('crear vuelve a cargar la lista de la misma tienda', async () => {
    vi.mocked(comprasService.crear).mockResolvedValue(compra())
    vi.mocked(comprasService.listarPorTienda).mockResolvedValue({
      contenido: [compra()],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    })
    const { crear, items } = useCompras()

    const ok = await crear(1, 2, [{ productoId: 10, cantidad: '10', costoUnitario: '5.00' }])

    expect(ok).toBe(true)
    expect(comprasService.listarPorTienda).toHaveBeenCalledWith(1, 0, 10, expect.any(AbortSignal))
    expect(items.value).toHaveLength(1)
  })

  it('crear con error devuelve false y guarda el mensaje en saveError', async () => {
    vi.mocked(comprasService.crear).mockRejectedValue(
      new ApiClientError({ message: 'Referencia inválida', status: 400, code: 'REFERENCIA_INVALIDA' }),
    )
    const { crear, saveError } = useCompras()

    const ok = await crear(1, 2, [])

    expect(ok).toBe(false)
    expect(saveError.value).toBe('Referencia inválida')
  })

  it('recibir reemplaza la compra en la lista con la respuesta del backend', async () => {
    const original = compra({ estado: 'BORRADOR' })
    const recibida = compra({ estado: 'RECIBIDA' })
    vi.mocked(comprasService.recibir).mockResolvedValue(recibida)
    const { items, recibir, cargar } = useCompras()
    vi.mocked(comprasService.listarPorTienda).mockResolvedValue({
      contenido: [original],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    })
    await cargar(1)

    await recibir(1, original)

    expect(comprasService.recibir).toHaveBeenCalledWith(1, 1)
    expect(items.value[0].estado).toBe('RECIBIDA')
  })

  it('anular con error de negocio guarda el mensaje sin tocar la lista', async () => {
    const original = compra({ estado: 'RECIBIDA' })
    vi.mocked(comprasService.anular).mockRejectedValue(
      new ApiClientError({
        message: 'Estado inválido para anular',
        status: 400,
        code: 'ESTADO_COMPRA_INVALIDO',
      }),
    )
    const { items, listError, anular, cargar } = useCompras()
    vi.mocked(comprasService.listarPorTienda).mockResolvedValue({
      contenido: [original],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    })
    await cargar(1)

    await anular(1, original)

    expect(listError.value).toBe('Estado inválido para anular')
    expect(items.value[0].estado).toBe('RECIBIDA')
  })
})
