import { ref } from 'vue'
import { inventarioService, type DatosMovimiento } from '@/services/inventario.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Inventario, MovimientoInventario } from '@/types/inventario'

export function useInventario() {
  const items = ref<Inventario[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const pagina = ref(1)
  const tamano = ref(10)
  const totalElementos = ref(0)
  const totalPaginas = ref(1)

  const movimientos = ref<MovimientoInventario[]>([])
  const movimientosLoading = ref(false)
  const movimientosError = ref<string | null>(null)
  const movimientosPagina = ref(1)
  const movimientosTamano = ref(10)
  const movimientosTotalElementos = ref(0)
  const movimientosTotalPaginas = ref(1)

  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  let tiendaActual: number | null = null
  let productoKardexActual: number | null = null
  let cargarController: AbortController | null = null
  let movimientosController: AbortController | null = null

  async function cargar(tiendaId: number) {
    tiendaActual = tiendaId
    cargarController?.abort()
    const controller = new AbortController()
    cargarController = controller
    listLoading.value = true
    listError.value = null
    try {
      const resultado = await inventarioService.listarPorTienda(
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
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar el inventario.'
    } finally {
      if (cargarController === controller) listLoading.value = false
    }
  }

  async function cargarMovimientos(tiendaId: number, productoId: number) {
    tiendaActual = tiendaId
    productoKardexActual = productoId
    movimientosController?.abort()
    const controller = new AbortController()
    movimientosController = controller
    movimientosLoading.value = true
    movimientosError.value = null
    try {
      const resultado = await inventarioService.listarMovimientos(
        tiendaId,
        productoId,
        movimientosPagina.value - 1,
        movimientosTamano.value,
        controller.signal,
      )
      if (controller.signal.aborted) return
      movimientos.value = resultado.contenido
      movimientosTotalElementos.value = resultado.totalElementos
      movimientosTotalPaginas.value = resultado.totalPaginas
    } catch (error) {
      if (error instanceof ApiClientError && error.isCanceled) return
      movimientosError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo cargar el kardex.'
    } finally {
      if (movimientosController === controller) movimientosLoading.value = false
    }
  }

  async function registrarMovimiento(tiendaId: number, datos: DatosMovimiento): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      await inventarioService.registrarMovimiento(tiendaId, datos)
      if (tiendaActual !== null) await cargar(tiendaActual)
      if (productoKardexActual === datos.productoId && tiendaActual !== null) {
        await cargarMovimientos(tiendaActual, productoKardexActual)
      }
      return true
    } catch (error) {
      saveError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo registrar el movimiento.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  return {
    items,
    listLoading,
    listError,
    pagina,
    tamano,
    totalElementos,
    totalPaginas,
    movimientos,
    movimientosLoading,
    movimientosError,
    movimientosPagina,
    movimientosTamano,
    movimientosTotalElementos,
    movimientosTotalPaginas,
    saveLoading,
    saveError,
    cargar,
    cargarMovimientos,
    registrarMovimiento,
  }
}
