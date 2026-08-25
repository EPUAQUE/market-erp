import { ref } from 'vue'
import { proveedoresService, type DatosProveedor } from '@/services/proveedores.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Proveedor } from '@/types/proveedor'

export function useProveedores() {
  const items = ref<Proveedor[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await proveedoresService.listar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(nit: string, datos: DatosProveedor): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const creado = await proveedoresService.crear(nit, datos)
      items.value = [...items.value, creado]
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear el proveedor.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(id: number, datos: DatosProveedor): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizado = await proveedoresService.actualizar(id, datos)
      items.value = items.value.map((p) => (p.id === id ? actualizado : p))
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo actualizar el proveedor.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function alternarEstado(proveedor: Proveedor) {
    try {
      if (proveedor.estado === 'ACTIVO') {
        await proveedoresService.desactivar(proveedor.id)
        proveedor.estado = 'INACTIVO'
      } else {
        await proveedoresService.activar(proveedor.id)
        proveedor.estado = 'ACTIVO'
      }
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cambiar el estado.'
    }
  }

  return { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado }
}
