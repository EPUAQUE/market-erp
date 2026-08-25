import { ref } from 'vue'
import { marcasService } from '@/services/marcas.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Marca } from '@/types/marca'

export function useMarcas() {
  const items = ref<Marca[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await marcasService.listar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(nombre: string): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const creada = await marcasService.crear(nombre)
      items.value = [...items.value, creada]
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear la marca.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(id: number, nombre: string): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizada = await marcasService.actualizar(id, nombre)
      items.value = items.value.map((m) => (m.id === id ? actualizada : m))
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo actualizar la marca.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  return { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar }
}
