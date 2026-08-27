import { ref } from 'vue'
import { gruposTiendaService } from '@/services/gruposTienda.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { GrupoTienda } from '@/types/grupoTienda'

export function useGruposTienda() {
  const items = ref<GrupoTienda[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await gruposTiendaService.listar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(codigo: string, nombre: string): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const creado = await gruposTiendaService.crear(codigo, nombre)
      items.value = [...items.value, creado]
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear el grupo.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(id: number, nombre: string): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizado = await gruposTiendaService.actualizar(id, nombre)
      items.value = items.value.map((g) => (g.id === id ? actualizado : g))
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo actualizar el grupo.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function alternarEstado(grupo: GrupoTienda) {
    try {
      if (grupo.estado === 'ACTIVO') {
        await gruposTiendaService.desactivar(grupo.id)
        grupo.estado = 'INACTIVO'
      } else {
        await gruposTiendaService.activar(grupo.id)
        grupo.estado = 'ACTIVO'
      }
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cambiar el estado.'
    }
  }

  return { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado }
}
