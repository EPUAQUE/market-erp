export type EstadoVenta = 'BORRADOR' | 'COMPLETADA' | 'ANULADA'

/** MIXTO no se ofrece al crear desde este formulario — requiere un desglose de pagos por canal que `completar()` no envía aquí (ver `market-flutter` para ese flujo). */
export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'CREDITO' | 'MIXTO'

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
