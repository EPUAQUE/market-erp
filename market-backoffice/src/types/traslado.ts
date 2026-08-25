export type EstadoTraslado = 'BORRADOR' | 'COMPLETADO' | 'ANULADO'

export interface LineaTraslado {
  id: number
  productoId: number
  cantidad: string
}

export interface Traslado {
  id: number
  tiendaOrigenId: number
  tiendaDestinoId: number
  fecha: string
  estado: EstadoTraslado
  lineas: LineaTraslado[]
}
