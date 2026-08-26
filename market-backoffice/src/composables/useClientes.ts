import { ref } from 'vue'
import { clientesService, type DatosCliente } from '@/services/clientes.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Cliente } from '@/types/cliente'

/**
 * `tamano` por defecto es grande a propósito: además de `ClientesView` (que lo
 * pisa con su propio selector 10/25/50/100), este composable lo usan
 * `VentasView`/`CuentasPorCobrarView` solo como fuente de un `<select>`/lookup
 * de cliente — necesitan el catálogo completo, no una página de 10.
 */
const TAMANO_CATALOGO_COMPLETO = 5000

export function useClientes() {
  const items = ref<Cliente[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  const pagina = ref(1)
  const tamano = ref(TAMANO_CATALOGO_COMPLETO)
  const totalElementos = ref(0)
  const totalPaginas = ref(1)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      const resultado = await clientesService.listar(pagina.value - 1, tamano.value)
      items.value = resultado.contenido
      totalElementos.value = resultado.totalElementos
      totalPaginas.value = resultado.totalPaginas
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(nit: string | undefined, datos: DatosCliente): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      await clientesService.crear(nit, datos)
      await cargar()
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear el cliente.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(id: number, datos: DatosCliente): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizado = await clientesService.actualizar(id, datos)
      items.value = items.value.map((c) => (c.id === id ? actualizado : c))
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo actualizar el cliente.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function alternarEstado(cliente: Cliente) {
    try {
      if (cliente.estado === 'ACTIVO') {
        await clientesService.desactivar(cliente.id)
        cliente.estado = 'INACTIVO'
      } else {
        await clientesService.activar(cliente.id)
        cliente.estado = 'ACTIVO'
      }
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cambiar el estado.'
    }
  }

  return {
    items,
    listLoading,
    listError,
    saveLoading,
    saveError,
    pagina,
    tamano,
    totalElementos,
    totalPaginas,
    cargar,
    crear,
    actualizar,
    alternarEstado,
  }
}
