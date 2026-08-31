import { ref } from 'vue'
import { trasladosService, type DatosLineaTraslado } from '@/services/traslados.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Traslado } from '@/types/traslado'

export function useTraslados() {
  const items = ref<Traslado[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  const pagina = ref(1)
  const tamano = ref(10)
  const totalElementos = ref(0)
  const totalPaginas = ref(1)

  let cargarController: AbortController | null = null

  async function cargar() {
    cargarController?.abort()
    const controller = new AbortController()
    cargarController = controller
    listLoading.value = true
    listError.value = null
    try {
      const resultado = await trasladosService.listar(pagina.value - 1, tamano.value, controller.signal)
      if (controller.signal.aborted) return
      items.value = resultado.contenido
      totalElementos.value = resultado.totalElementos
      totalPaginas.value = resultado.totalPaginas
    } catch (error) {
      if (error instanceof ApiClientError && error.isCanceled) return
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      if (cargarController === controller) listLoading.value = false
    }
  }

  async function crear(
    tiendaOrigenId: number,
    tiendaDestinoId: number,
    lineas: DatosLineaTraslado[],
  ): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      await trasladosService.crear(tiendaOrigenId, tiendaDestinoId, lineas)
      await cargar()
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear el traslado.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function completar(traslado: Traslado) {
    listError.value = null
    try {
      await trasladosService.completar(traslado.id)
      await cargar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo completar el traslado.'
    }
  }

  async function anular(traslado: Traslado) {
    listError.value = null
    try {
      await trasladosService.anular(traslado.id)
      await cargar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular el traslado.'
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
    completar,
    anular,
  }
}
