import { ref } from 'vue'
import { usuariosService } from '@/services/usuarios.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Usuario } from '@/types/usuario'

export function useUsuarios() {
  const items = ref<Usuario[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const createLoading = ref(false)
  const createError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await usuariosService.listar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(username: string, password: string): Promise<boolean> {
    createLoading.value = true
    createError.value = null
    try {
      const creado = await usuariosService.crear(username, password)
      items.value = [...items.value, creado]
      return true
    } catch (error) {
      createError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear el usuario.'
      return false
    } finally {
      createLoading.value = false
    }
  }

  return { items, listLoading, listError, createLoading, createError, cargar, crear }
}
