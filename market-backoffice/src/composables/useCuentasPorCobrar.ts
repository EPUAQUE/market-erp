import { ref } from 'vue'
import { cuentasPorCobrarService } from '@/services/cuentasPorCobrar.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { CuentaPorCobrar } from '@/types/cuentaPorCobrar'

export function useCuentasPorCobrar() {
  const items = ref<CuentaPorCobrar[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)

  const pagina = ref(1)
  const tamano = ref(10)
  const totalElementos = ref(0)
  const totalPaginas = ref(1)

  let tiendaActual: number | null = null

  async function cargar(tiendaId: number) {
    tiendaActual = tiendaId
    listLoading.value = true
    listError.value = null
    try {
      const resultado = await cuentasPorCobrarService.listarPorTienda(tiendaId, pagina.value - 1, tamano.value)
      items.value = resultado.contenido
      totalElementos.value = resultado.totalElementos
      totalPaginas.value = resultado.totalPaginas
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function recargar() {
    if (tiendaActual !== null) await cargar(tiendaActual)
  }

  async function registrarCobro(tiendaId: number, cuenta: CuentaPorCobrar, monto: string): Promise<boolean> {
    listError.value = null
    try {
      await cuentasPorCobrarService.registrarCobro(tiendaId, cuenta.id, monto)
      await recargar()
      return true
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo registrar el cobro.'
      return false
    }
  }

  async function anular(tiendaId: number, cuenta: CuentaPorCobrar) {
    listError.value = null
    try {
      await cuentasPorCobrarService.anular(tiendaId, cuenta.id)
      await recargar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular la cuenta.'
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
    cargar,
    registrarCobro,
    anular,
  }
}
