export type EstadoCuentaPorPagar = 'PENDIENTE' | 'PAGADA' | 'ANULADA'

export interface Pago {
  id: number
  fecha: string
  monto: string
}

export interface CuentaPorPagar {
  id: number
  compraId: number
  proveedorId: number
  tiendaId: number
  fechaEmision: string
  fechaVencimiento: string
  montoOriginal: string
  saldoPendiente: string
  estado: EstadoCuentaPorPagar
  pagos: Pago[]
}
