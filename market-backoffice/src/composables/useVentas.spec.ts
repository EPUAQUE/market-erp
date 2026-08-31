import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Venta } from '@/types/venta'

vi.mock('@/services/ventas.service', () => ({
  ventasService: {
    listarPorTienda: vi.fn(),
    crear: vi.fn(),
    completar: vi.fn(),
    anular: vi.fn(),
  },
}))

const { ventasService } = await import('@/services/ventas.service')
const { useVentas } = await import('./useVentas')

function venta(overrides: Partial<Venta> = {}): Venta {
  return {
    id: 1,
    clienteId: 1,
    tiendaId: 1,
    vendedorId: 1,
    fecha: '2026-01-01T00:00:00Z',
    estado: 'BORRADOR',
    lineas: [{ id: 1, productoId: 10, cantidad: '2', precioUnitario: '8.50' }],
    total: '17.00',
    ...overrides,
  }
}

describe('useVentas', () => {
  beforeEach(() => {
    vi.mocked(ventasService.listarPorTienda).mockReset()
    vi.mocked(ventasService.crear).mockReset()
    vi.mocked(ventasService.completar).mockReset()
    vi.mocked(ventasService.anular).mockReset()
  })

  it('cargar convierte pagina 1-based a 0-based y guarda el resultado', async () => {
    vi.mocked(ventasService.listarPorTienda).mockResolvedValue({
      contenido: [venta()],
      pagina: 1,
      tamano: 10,
      totalElementos: 15,
      totalPaginas: 2,
    })
    const { pagina, totalElementos, totalPaginas, items, cargar } = useVentas()
    pagina.value = 2

    await cargar(1)

    expect(ventasService.listarPorTienda).toHaveBeenCalledWith(1, 1, 10, expect.any(AbortSignal))
    expect(items.value).toHaveLength(1)
    expect(totalElementos.value).toBe(15)
    expect(totalPaginas.value).toBe(2)
  })

  it('cargar con error de API guarda el mensaje en listError', async () => {
    vi.mocked(ventasService.listarPorTienda).mockRejectedValue(
      new ApiClientError({ message: 'No autorizado', status: 403, code: 'FORBIDDEN' }),
    )
    const { listError, cargar } = useVentas()

    await cargar(1)

    expect(listError.value).toBe('No autorizado')
  })

  it('crear recarga la lista de la misma tienda tras crear con éxito', async () => {
    vi.mocked(ventasService.crear).mockResolvedValue(venta())
    vi.mocked(ventasService.listarPorTienda).mockResolvedValue({
      contenido: [venta()],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    })
    const { cargar, crear, items } = useVentas()
    // recargar() (usado por crear/completar/anular) solo actúa sobre la tienda de
    // la última cargar() — replica el flujo real: la vista siempre carga antes de crear.
    await cargar(1)

    const ok = await crear(1, 2, [{ productoId: 10, cantidad: '2', precioUnitario: '8.50' }], 'EFECTIVO')

    expect(ok).toBe(true)
    expect(ventasService.listarPorTienda).toHaveBeenCalledWith(1, 0, 10, expect.any(AbortSignal))
    expect(items.value).toHaveLength(1)
  })

  it('crear con error de negocio devuelve false y guarda el mensaje en saveError', async () => {
    vi.mocked(ventasService.crear).mockRejectedValue(
      new ApiClientError({
        message: 'Límite de crédito excedido',
        status: 409,
        code: 'LIMITE_CREDITO_EXCEDIDO',
      }),
    )
    const { crear, saveError } = useVentas()

    const ok = await crear(1, 2, [], 'CREDITO')

    expect(ok).toBe(false)
    expect(saveError.value).toBe('Límite de crédito excedido')
  })

  it('completar recarga la lista tras completar con éxito', async () => {
    const borrador = venta({ estado: 'BORRADOR' })
    const completada = venta({ estado: 'COMPLETADA' })
    vi.mocked(ventasService.completar).mockResolvedValue(completada)
    vi.mocked(ventasService.listarPorTienda).mockResolvedValue({
      contenido: [completada],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    })
    const { cargar, completar, items } = useVentas()
    await cargar(1)

    await completar(1, borrador)

    expect(ventasService.completar).toHaveBeenCalledWith(1, 1)
    expect(items.value[0].estado).toBe('COMPLETADA')
  })

  it('anular con error de negocio guarda el mensaje sin tocar la lista', async () => {
    const original = venta({ estado: 'COMPLETADA' })
    vi.mocked(ventasService.anular).mockRejectedValue(
      new ApiClientError({
        message: 'Estado inválido para anular',
        status: 400,
        code: 'ESTADO_VENTA_INVALIDO',
      }),
    )
    vi.mocked(ventasService.listarPorTienda).mockResolvedValue({
      contenido: [original],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    })
    const { cargar, anular, items, listError } = useVentas()
    await cargar(1)

    await anular(1, original)

    expect(listError.value).toBe('Estado inválido para anular')
    expect(items.value[0].estado).toBe('COMPLETADA')
  })
})
