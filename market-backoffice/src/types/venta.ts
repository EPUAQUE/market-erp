export type EstadoVenta = 'BORRADOR' | 'COMPLETADA' | 'ANULADA'

export interface LineaVenta {
  id: number
  productoId: number
  cantidad: string
  precioUnitario: string
}

export interface Venta {
  id: number
  clienteId: number
  tiendaId: number
  vendedorId: number
  fecha: string
  estado: EstadoVenta
  lineas: LineaVenta[]
  total: string
}
