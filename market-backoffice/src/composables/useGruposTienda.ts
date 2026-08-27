import { ref } from 'vue'
import { gruposTiendaService } from '@/services/gruposTienda.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { GrupoTienda } from '@/types/grupoTienda'

// Solo lectura por ahora: el CRUD completo de grupos llega en una fase aparte
// (ver market-backend/docs/plan-mejoras.md). Esto alcanza para el selector de
// grupo del formulario de Tiendas.
export function useGruposTienda() {
  const items = ref<GrupoTienda[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)

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

  return { items, listLoading, listError, cargar }
}
