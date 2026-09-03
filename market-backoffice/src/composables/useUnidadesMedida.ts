import { ref } from 'vue'
import { unidadesMedidaService } from '@/services/unidadesMedida.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { UnidadMedida } from '@/types/unidadMedida'

export function useUnidadesMedida() {
  const items = ref<UnidadMedida[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await unidadesMedidaService.listar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(nombre: string, abreviacion: string): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const creada = await unidadesMedidaService.crear(nombre, abreviacion)
      items.value = [...items.value, creada]
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear la unidad.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(id: number, nombre: string, abreviacion: string): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizada = await unidadesMedidaService.actualizar(id, nombre, abreviacion)
      items.value = items.value.map((u) => (u.id === id ? actualizada : u))
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo actualizar la unidad.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function alternarEstado(unidad: UnidadMedida) {
    try {
      if (unidad.estado === 'ACTIVA') {
        await unidadesMedidaService.desactivar(unidad.id)
        unidad.estado = 'INACTIVA'
      } else {
        await unidadesMedidaService.activar(unidad.id)
        unidad.estado = 'ACTIVA'
      }
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cambiar el estado.'
    }
  }

  return { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado }
}
