import { ref } from 'vue'
import { categoriasService } from '@/services/categorias.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Categoria } from '@/types/categoria'

export function useCategorias() {
  const items = ref<Categoria[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await categoriasService.listar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(nombre: string, imagen?: string): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const creada = await categoriasService.crear(nombre, imagen)
      items.value = [...items.value, creada]
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear la categoría.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(id: number, nombre: string, imagen?: string): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizada = await categoriasService.actualizar(id, nombre, imagen)
      items.value = items.value.map((c) => (c.id === id ? actualizada : c))
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo actualizar la categoría.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function alternarEstado(categoria: Categoria) {
    try {
      if (categoria.estado === 'ACTIVA') {
        await categoriasService.desactivar(categoria.id)
        categoria.estado = 'INACTIVA'
      } else {
        await categoriasService.activar(categoria.id)
        categoria.estado = 'ACTIVA'
      }
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cambiar el estado.'
    }
  }

  return { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado }
}
