import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ApiClientError } from '@/services/http/ApiClient'
import type { CajaSesion } from '@/types/caja'

vi.mock('@/services/caja.service', () => ({
  cajaService: {
    obtenerAbierta: vi.fn(),
    listarPorTienda: vi.fn(),
    abrir: vi.fn(),
    registrarMovimiento: vi.fn(),
    cerrar: vi.fn(),
  },
}))

const { cajaService } = await import('@/services/caja.service')
const { useCaja } = await import('./useCaja')

function sesion(overrides: Partial<CajaSesion> = {}): CajaSesion {
  return {
    id: 1,
    tiendaId: 1,
    fechaApertura: '2026-01-01T08:00:00Z',
    fechaCierre: null,
    montoInicial: '500.00',
    montoFinalContado: null,
    saldoEsperado: '500.00',
    estado: 'ABIERTA',
    movimientos: [],
    ...overrides,
  }
}

describe('useCaja', () => {
  beforeEach(() => {
    vi.mocked(cajaService.obtenerAbierta).mockReset()
    vi.mocked(cajaService.listarPorTienda).mockReset()
    vi.mocked(cajaService.abrir).mockReset()
    vi.mocked(cajaService.registrarMovimiento).mockReset()
    vi.mocked(cajaService.cerrar).mockReset()
  })

  it('cargarAbierta guarda la sesión cuando hay una caja abierta', async () => {
    vi.mocked(cajaService.obtenerAbierta).mockResolvedValue(sesion())
    const { sesionAbierta, cargarAbierta } = useCaja()

    await cargarAbierta(1)

    expect(sesionAbierta.value?.id).toBe(1)
  })

  it('cargarAbierta con 404 deja sesionAbierta en null sin marcar error', async () => {
    vi.mocked(cajaService.obtenerAbierta).mockRejectedValue(
      new ApiClientError({ message: 'No hay caja abierta', status: 404, code: 'NOT_FOUND' }),
    )
    const { sesionAbierta, sesionError, cargarAbierta } = useCaja()

    await cargarAbierta(1)

    expect(sesionAbierta.value).toBeNull()
    expect(sesionError.value).toBeNull()
  })

  it('cargarAbierta con otro error sí lo guarda en sesionError', async () => {
    vi.mocked(cajaService.obtenerAbierta).mockRejectedValue(
      new ApiClientError({ message: 'Error del servidor', status: 500, code: 'INTERNAL' }),
    )
    const { sesionError, cargarAbierta } = useCaja()

    await cargarAbierta(1)

    expect(sesionError.value).toBe('Error del servidor')
  })

  it('cargarHistorial convierte pagina 1-based a 0-based', async () => {
    vi.mocked(cajaService.listarPorTienda).mockResolvedValue({
      contenido: [sesion({ estado: 'CERRADA' })],
      pagina: 1,
      tamano: 10,
      totalElementos: 12,
      totalPaginas: 2,
    })
    const { historialPagina, historial, historialTotalElementos, cargarHistorial } = useCaja()
    historialPagina.value = 2

    await cargarHistorial(1)

    expect(cajaService.listarPorTienda).toHaveBeenCalledWith(1, 1, 10, expect.any(AbortSignal))
    expect(historial.value).toHaveLength(1)
    expect(historialTotalElementos.value).toBe(12)
  })

  it('abrir guarda la sesión recién abierta y devuelve true', async () => {
    vi.mocked(cajaService.abrir).mockResolvedValue(sesion())
    const { sesionAbierta, abrir } = useCaja()

    const ok = await abrir(1, '500.00')

    expect(ok).toBe(true)
    expect(sesionAbierta.value?.estado).toBe('ABIERTA')
  })

  it('abrir con error de negocio devuelve false y guarda el mensaje', async () => {
    vi.mocked(cajaService.abrir).mockRejectedValue(
      new ApiClientError({ message: 'Ya hay una caja abierta', status: 409, code: 'CAJA_YA_ABIERTA' }),
    )
    const { actionError, abrir } = useCaja()

    const ok = await abrir(1, '500.00')

    expect(ok).toBe(false)
    expect(actionError.value).toBe('Ya hay una caja abierta')
  })

  it('registrarMovimiento actualiza sesionAbierta con la respuesta del backend', async () => {
    vi.mocked(cajaService.registrarMovimiento).mockResolvedValue(
      sesion({
        movimientos: [
          { id: 1, fecha: '2026-01-01T09:00:00Z', tipo: 'INGRESO', concepto: 'Venta', monto: '50.00' },
        ],
      }),
    )
    const { sesionAbierta, registrarMovimiento } = useCaja()

    const ok = await registrarMovimiento(1, { tipo: 'INGRESO', concepto: 'Venta', monto: '50.00' })

    expect(ok).toBe(true)
    expect(sesionAbierta.value?.movimientos).toHaveLength(1)
  })

  it('cerrar deja sesionAbierta en null cuando tiene éxito', async () => {
    vi.mocked(cajaService.cerrar).mockResolvedValue(sesion({ estado: 'CERRADA' }))
    const { sesionAbierta, cerrar } = useCaja()

    const ok = await cerrar(1, '545.00')

    expect(ok).toBe(true)
    expect(sesionAbierta.value).toBeNull()
  })

  it('cerrar con error de negocio devuelve false y no toca sesionAbierta', async () => {
    vi.mocked(cajaService.abrir).mockResolvedValue(sesion())
    vi.mocked(cajaService.cerrar).mockRejectedValue(
      new ApiClientError({
        message: 'No hay una caja abierta para cerrar',
        status: 409,
        code: 'CAJA_NO_ABIERTA',
      }),
    )
    const { sesionAbierta, actionError, abrir, cerrar } = useCaja()
    await abrir(1, '500.00')

    const ok = await cerrar(1, '545.00')

    expect(ok).toBe(false)
    expect(actionError.value).toBe('No hay una caja abierta para cerrar')
    expect(sesionAbierta.value).not.toBeNull()
  })
})
