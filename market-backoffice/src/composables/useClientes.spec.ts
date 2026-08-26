import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Cliente } from '@/types/cliente'

vi.mock('@/services/clientes.service', () => ({
  clientesService: {
    listar: vi.fn(),
    crear: vi.fn(),
    actualizar: vi.fn(),
    activar: vi.fn(),
    desactivar: vi.fn(),
  },
}))

const { clientesService } = await import('@/services/clientes.service')
const { useClientes } = await import('./useClientes')

function cliente(overrides: Partial<Cliente> = {}): Cliente {
  return {
    id: 1,
    nit: '12345678-9',
    nombre: 'Juan Pérez',
    direccion: null,
    telefono: null,
    correo: null,
    estado: 'ACTIVO',
    ...overrides,
  }
}

describe('useClientes', () => {
  beforeEach(() => {
    vi.mocked(clientesService.listar).mockReset()
    vi.mocked(clientesService.crear).mockReset()
    vi.mocked(clientesService.actualizar).mockReset()
    vi.mocked(clientesService.activar).mockReset()
    vi.mocked(clientesService.desactivar).mockReset()
  })

  it('cargar convierte pagina 1-based a 0-based y guarda el resultado', async () => {
    vi.mocked(clientesService.listar).mockResolvedValue({
      contenido: [cliente()],
      pagina: 2,
      tamano: 10,
      totalElementos: 25,
      totalPaginas: 3,
    })
    const { items, pagina, tamano, totalElementos, totalPaginas, cargar } = useClientes()
    pagina.value = 3
    tamano.value = 10

    await cargar()

    expect(clientesService.listar).toHaveBeenCalledWith(2, 10)
    expect(items.value).toHaveLength(1)
    expect(totalElementos.value).toBe(25)
    expect(totalPaginas.value).toBe(3)
  })

  it('cargar con error de API guarda el mensaje en listError', async () => {
    vi.mocked(clientesService.listar).mockRejectedValue(
      new ApiClientError({ message: 'Sesión expirada', status: 401, code: 'UNAUTHORIZED' }),
    )
    const { listError, listLoading, cargar } = useClientes()

    await cargar()

    expect(listError.value).toBe('Sesión expirada')
    expect(listLoading.value).toBe(false)
  })

  it('crear vuelve a cargar la lista para mantener el total correcto', async () => {
    vi.mocked(clientesService.crear).mockResolvedValue(cliente({ id: 2, nombre: 'Nuevo' }))
    vi.mocked(clientesService.listar).mockResolvedValue({
      contenido: [cliente({ id: 2, nombre: 'Nuevo' })],
      pagina: 0,
      tamano: 5000,
      totalElementos: 1,
      totalPaginas: 1,
    })
    const { crear, items } = useClientes()

    const ok = await crear(undefined, { nombre: 'Nuevo' })

    expect(ok).toBe(true)
    expect(clientesService.listar).toHaveBeenCalledOnce()
    expect(items.value).toHaveLength(1)
  })

  it('crear con error devuelve false y guarda el mensaje en saveError', async () => {
    vi.mocked(clientesService.crear).mockRejectedValue(
      new ApiClientError({ message: 'NIT duplicado', status: 409, code: 'CLIENTE_DUPLICADO' }),
    )
    const { crear, saveError } = useClientes()

    const ok = await crear('12345678-9', { nombre: 'Duplicado' })

    expect(ok).toBe(false)
    expect(saveError.value).toBe('NIT duplicado')
  })

  it('actualizar reemplaza el cliente en la lista sin recargar todo', async () => {
    const original = cliente()
    const actualizado = cliente({ nombre: 'Nombre editado' })
    vi.mocked(clientesService.actualizar).mockResolvedValue(actualizado)
    const { items, actualizar, cargar } = useClientes()
    vi.mocked(clientesService.listar).mockResolvedValue({
      contenido: [original],
      pagina: 0,
      tamano: 5000,
      totalElementos: 1,
      totalPaginas: 1,
    })
    await cargar()

    const ok = await actualizar(1, { nombre: 'Nombre editado' })

    expect(ok).toBe(true)
    expect(items.value[0].nombre).toBe('Nombre editado')
    expect(clientesService.listar).toHaveBeenCalledOnce()
  })

  it('alternarEstado activo -> desactiva y refleja el nuevo estado en el objeto', async () => {
    vi.mocked(clientesService.desactivar).mockResolvedValue(undefined)
    const { alternarEstado } = useClientes()
    const c = cliente({ estado: 'ACTIVO' })

    await alternarEstado(c)

    expect(clientesService.desactivar).toHaveBeenCalledWith(1)
    expect(c.estado).toBe('INACTIVO')
  })

  it('alternarEstado inactivo -> activa y refleja el nuevo estado en el objeto', async () => {
    vi.mocked(clientesService.activar).mockResolvedValue(undefined)
    const { alternarEstado } = useClientes()
    const c = cliente({ estado: 'INACTIVO' })

    await alternarEstado(c)

    expect(clientesService.activar).toHaveBeenCalledWith(1)
    expect(c.estado).toBe('ACTIVO')
  })
})
