import { ref } from 'vue'
import { comprasService, type DatosLineaCompra } from '@/services/compras.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Compra } from '@/types/compra'

export function useCompras() {
  const items = ref<Compra[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  const pagina = ref(1)
  const tamano = ref(10)
  const totalElementos = ref(0)
  const totalPaginas = ref(1)

  async function cargar(tiendaId: number) {
    listLoading.value = true
    listError.value = null
    try {
      const resultado = await comprasService.listarPorTienda(tiendaId, pagina.value - 1, tamano.value)
      items.value = resultado.contenido
      totalElementos.value = resultado.totalElementos
      totalPaginas.value = resultado.totalPaginas
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(tiendaId: number, proveedorId: number, lineas: DatosLineaCompra[]): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      await comprasService.crear(tiendaId, proveedorId, lineas)
      await cargar(tiendaId)
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear la compra.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function recibir(tiendaId: number, compra: Compra) {
    listError.value = null
    try {
      const actualizada = await comprasService.recibir(tiendaId, compra.id)
      items.value = items.value.map((c) => (c.id === compra.id ? actualizada : c))
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo recibir la compra.'
    }
  }

  async function anular(tiendaId: number, compra: Compra) {
    listError.value = null
    try {
      const actualizada = await comprasService.anular(tiendaId, compra.id)
      items.value = items.value.map((c) => (c.id === compra.id ? actualizada : c))
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular la compra.'
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
    recibir,
    anular,
  }
}
