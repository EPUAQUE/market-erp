import { ref } from 'vue'
import { notificacionesService } from '@/services/notificaciones.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Notificacion } from '@/types/notificacion'

export function useNotificaciones() {
  const items = ref<Notificacion[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const generarLoading = ref(false)

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
      const resultado = await notificacionesService.listarPorTienda(
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

  async function generar(tiendaId: number) {
    generarLoading.value = true
    listError.value = null
    try {
      await notificacionesService.generar(tiendaId)
      await cargar(tiendaId)
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo generar notificaciones.'
    } finally {
      generarLoading.value = false
    }
  }

  async function marcarLeida(tiendaId: number, notificacion: Notificacion) {
    listError.value = null
    try {
      await notificacionesService.marcarLeida(tiendaId, notificacion.id)
      await recargar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo marcar como leída.'
    }
  }

  return {
    items,
    listLoading,
    listError,
    generarLoading,
    pagina,
    tamano,
    totalElementos,
    totalPaginas,
    cargar,
    generar,
    marcarLeida,
  }
}
