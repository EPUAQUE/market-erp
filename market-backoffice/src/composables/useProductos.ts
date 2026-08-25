import { ref } from 'vue'
import { productosService, type DatosProducto } from '@/services/productos.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Producto } from '@/types/producto'

/**
 * `tamano` por defecto es grande a propósito: además de `ProductosView` (que
 * lo pisa con su propio selector 10/25/50/100), este composable lo usan
 * `VentasView`/`TrasladosView` solo como fuente de un `<select>` de producto —
 * necesitan el catálogo completo, no una página de 10.
 */
const TAMANO_CATALOGO_COMPLETO = 5000

export function useProductos() {
  const items = ref<Producto[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const saveLoading = ref(false)
  const saveError = ref<string | null>(null)

  const pagina = ref(1)
  const tamano = ref(TAMANO_CATALOGO_COMPLETO)
  const totalElementos = ref(0)
  const totalPaginas = ref(1)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      const resultado = await productosService.listar(pagina.value - 1, tamano.value)
      items.value = resultado.contenido
      totalElementos.value = resultado.totalElementos
      totalPaginas.value = resultado.totalPaginas
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  async function crear(codigoInterno: string, datos: DatosProducto): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      await productosService.crear(codigoInterno, datos)
      await cargar()
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear el producto.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function actualizar(id: number, datos: DatosProducto): Promise<boolean> {
    saveLoading.value = true
    saveError.value = null
    try {
      const actualizado = await productosService.actualizar(id, datos)
      items.value = items.value.map((p) => (p.id === id ? actualizado : p))
      return true
    } catch (error) {
      saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo actualizar el producto.'
      return false
    } finally {
      saveLoading.value = false
    }
  }

  async function alternarEstado(producto: Producto) {
    try {
      if (producto.activo) {
        await productosService.desactivar(producto.id)
        producto.activo = false
      } else {
        await productosService.activar(producto.id)
        producto.activo = true
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
    pagina,
    tamano,
    totalElementos,
    totalPaginas,
    cargar,
    crear,
    actualizar,
    alternarEstado,
  }
}
