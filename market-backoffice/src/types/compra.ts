export type EstadoCompra = 'BORRADOR' | 'RECIBIDA' | 'ANULADA'

export interface LineaCompra {
  id: number
  productoId: number
  cantidad: string
  costoUnitario: string
}

export interface Compra {
  id: number
  proveedorId: number
  tiendaId: number
  fecha: string
  estado: EstadoCompra
  lineas: LineaCompra[]
  total: string
}
