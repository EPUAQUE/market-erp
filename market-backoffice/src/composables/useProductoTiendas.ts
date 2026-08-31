import { ref } from 'vue'
import { productosService, type DatosProductoTienda } from '@/services/productos.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { ProductoTienda } from '@/types/producto'

export function useProductoTiendas(productoId: number) {
  const items = ref<ProductoTienda[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await productosService.listarTiendas(productoId)
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function asignar(tiendaId: number, datos: DatosProductoTienda): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const creado = await productosService.asignarTienda(productoId, tiendaId, datos)
      items.value = [...items.value, creado]
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo asignar la tienda.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(id: number, datos: DatosProductoTienda): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizado = await productosService.actualizarTienda(productoId, id, datos)
      items.value = items.value.map((pt) => (pt.id === id ? actualizado : pt))
      return true
    } catch (error) {
      saveError.value =
        error instanceof ApiClientError ? error.message : 'No se pudo actualizar la configuración.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function alternarEstado(productoTienda: ProductoTienda) {
    try {
      if (productoTienda.activo) {
        await productosService.desactivarTienda(productoId, productoTienda.id)
        productoTienda.activo = false
      } else {
        await productosService.activarTienda(productoId, productoTienda.id)
        productoTienda.activo = true
      }
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cambiar el estado.'
    }
  }

  return {
    items,
    listLoading,
    listError,
    saveLoading,
    saveError,
    cargar,
    asignar,
    actualizar,
    alternarEstado,
  }
}
