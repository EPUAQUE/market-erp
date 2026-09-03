import { computed, reactive, ref, type Ref } from 'vue'

export type TipoFiltroColumna = 'texto' | 'opciones' | 'booleano'

export interface OpcionFiltro {
  valor: string
  etiqueta: string
}

/**
 * `valor` es el texto contra el que se compara — para 'texto' hace substring
 * case-insensitive, para 'opciones' compara exacto contra `valor` de la
 * opción elegida, para 'booleano' compara contra 'true'/'false' como string.
 * `incluirEnGlobal` en false saca la columna de la búsqueda global (p. ej. una
 * columna cuyo texto ya es puro ruido para buscar, como un id crudo).
 */
export interface FiltroColumna<T> {
  clave: string
  tipo: TipoFiltroColumna
  valor: (item: T) => string
  opciones?: OpcionFiltro[]
  incluirEnGlobal?: boolean
}

/**
 * Filtro por columna + búsqueda global reutilizable en toda vista de tabla.
 * La búsqueda global usa exactamente el mismo `valor(item)` que cada filtro
 * de columna — así el pedido del cliente ("que ese mismo filtro se pueda
 * hacer de forma global") queda garantizado por construcción, no por
 * duplicar la lógica de comparación en dos lugares.
 */
export function useFiltrosTabla<T>(items: Ref<T[]>, columnas: FiltroColumna<T>[]) {
  const busquedaGlobal = ref('')
  const filtrosColumna = reactive<Record<string, string>>({})

  const columnasGlobal = columnas.filter((c) => c.incluirEnGlobal !== false)

  const itemsFiltrados = computed(() => {
    const global = busquedaGlobal.value.trim().toLowerCase()
    return items.value.filter((item) => {
      for (const columna of columnas) {
        const filtro = filtrosColumna[columna.clave]
        if (!filtro) continue
        const actual = columna.valor(item)
        if (columna.tipo === 'texto') {
          if (!actual.toLowerCase().includes(filtro.toLowerCase())) return false
        } else if (actual !== filtro) {
          return false
        }
      }
      if (global !== '' && !columnasGlobal.some((c) => c.valor(item).toLowerCase().includes(global))) {
        return false
      }
      return true
    })
  })

  function limpiarFiltros() {
    busquedaGlobal.value = ''
    for (const clave of Object.keys(filtrosColumna)) delete filtrosColumna[clave]
  }

  const hayFiltrosActivos = computed(
    () =>
      busquedaGlobal.value.trim() !== '' || Object.values(filtrosColumna).some((v) => v !== '' && v != null),
  )

  return { busquedaGlobal, filtrosColumna, itemsFiltrados, limpiarFiltros, hayFiltrosActivos }
}
