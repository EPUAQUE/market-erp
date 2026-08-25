import { ref } from 'vue'
import { clientesService, type DatosCliente } from '@/services/clientes.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Cliente } from '@/types/cliente'

export function useClientes() {
  const items = ref<Cliente[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await clientesService.listar()
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
      const creado = await clientesService.crear(nit, datos)
      items.value = [...items.value, creado]
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

  return { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado }
}
