import { ref } from 'vue'
import { gastosProgramadosService, type DatosGastoProgramado } from '@/services/gastosProgramados.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { GastoProgramado } from '@/types/gastoProgramado'

export function useGastosProgramados() {
  const items = ref<GastoProgramado[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  async function cargar(tiendaId: number) {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await gastosProgramadosService.listarPorTienda(tiendaId)
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(
    tiendaId: number,
    datos: DatosGastoProgramado & { fechaInicio: string },
  ): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const creado = await gastosProgramadosService.crear(tiendaId, datos)
      items.value = [...items.value, creado]
      return true
    } catch (error) {
      saveError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo crear el gasto programado.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(
    tiendaId: number,
    gasto: GastoProgramado,
    datos: DatosGastoProgramado,
  ): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizado = await gastosProgramadosService.actualizar(tiendaId, gasto.id, datos)
      items.value = items.value.map((g) => (g.id === gasto.id ? actualizado : g))
      return true
    } catch (error) {
      saveError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo actualizar el gasto programado.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function activar(tiendaId: number, gasto: GastoProgramado) {
    listError.value = null
    try {
      const actualizado = await gastosProgramadosService.activar(tiendaId, gasto.id)
      items.value = items.value.map((g) => (g.id === gasto.id ? actualizado : g))
    } catch (error) {
      listError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo activar el gasto programado.'
    }
  }

  async function desactivar(tiendaId: number, gasto: GastoProgramado) {
    listError.value = null
    try {
      const actualizado = await gastosProgramadosService.desactivar(tiendaId, gasto.id)
      items.value = items.value.map((g) => (g.id === gasto.id ? actualizado : g))
    } catch (error) {
      listError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo desactivar el gasto programado.'
    }
  }

  async function generarPago(tiendaId: number, gasto: GastoProgramado) {
    listError.value = null
    try {
      const actualizado = await gastosProgramadosService.generarPago(tiendaId, gasto.id)
      items.value = items.value.map((g) => (g.id === gasto.id ? actualizado : g))
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo generar el pago.'
    }
  }

  return {
    items,
    listLoading,
    listError,
    saveLoading,
    saveError,
    cargar,
    crear,
    actualizar,
    activar,
    desactivar,
    generarPago,
  }
}
