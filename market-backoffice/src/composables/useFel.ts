import { ref } from 'vue'
import { felService } from '@/services/fel.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { DocumentoFel } from '@/types/fel'

export function useFel() {
  const items = ref<DocumentoFel[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const emitirLoading = ref(false)
  const emitirError = ref<string | null>(null)

  const pagina = ref(1)
  const tamano = ref(10)
  const totalElementos = ref(0)
  const totalPaginas = ref(1)

  let tiendaActual: number | null = null
  let cargarController: AbortController | null = null

  async function cargar(tiendaId: number) {
    tiendaActual = tiendaId
    cargarController?.abort()
    const controller = new AbortController()
    cargarController = controller
    listLoading.value = true
    listError.value = null
    try {
      const resultado = await felService.listarPorTienda(
        tiendaId,
        pagina.value - 1,
        tamano.value,
        controller.signal,
      )
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

  async function recargar() {
    if (tiendaActual !== null) await cargar(tiendaActual)
  }

  async function emitir(tiendaId: number, ventaId: number): Promise<boolean> {
    emitirLoading.value = true
    emitirError.value = null
    try {
      await felService.emitir(tiendaId, ventaId)
      await recargar()
      return true
    } catch (error) {
      emitirError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo emitir el documento FEL.'
      return false
    } finally {
      emitirLoading.value = false
    }
  }

  async function reintentar(tiendaId: number, documento: DocumentoFel) {
    listError.value = null
    try {
      await felService.reintentar(tiendaId, documento.id)
      await recargar()
    } catch (error) {
      listError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo reintentar la certificación.'
    }
  }

  async function anular(tiendaId: number, documento: DocumentoFel, motivo: string) {
    listError.value = null
    try {
      await felService.anular(tiendaId, documento.id, motivo)
      await recargar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular el documento.'
    }
  }

  return {
    items,
    listLoading,
    listError,
    emitirLoading,
    emitirError,
    pagina,
    tamano,
    totalElementos,
    totalPaginas,
    cargar,
    emitir,
    reintentar,
    anular,
  }
}
