export interface Inventario {
  id: number | null
  tiendaId: number
  productoId: number
  existenciaActual: string
  costoPromedioActual: string
}

export type TipoMovimiento =
  | 'COMPRA'
  | 'VENTA'
  | 'AJUSTE_POSITIVO'
  | 'AJUSTE_NEGATIVO'
  | 'TRASLADO_ENTRADA'
  | 'TRASLADO_SALIDA'
  | 'DEVOLUCION_CLIENTE'
  | 'DEVOLUCION_PROVEEDOR'

export interface MovimientoInventario {
  id: number
  fecha: string
  tiendaId: number
  productoId: number
  cantidad: string
  costoUnitario: string
  tipoMovimiento: TipoMovimiento
}
