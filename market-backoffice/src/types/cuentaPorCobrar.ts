export type EstadoCuentaPorCobrar = 'PENDIENTE' | 'COBRADA' | 'ANULADA'

export interface Cobro {
  id: number
  fecha: string
  monto: string
}

export interface CuentaPorCobrar {
  id: number
  ventaId: number
  clienteId: number
  tiendaId: number
  fechaEmision: string
  fechaVencimiento: string
  montoOriginal: string
  saldoPendiente: string
  estado: EstadoCuentaPorCobrar
  cobros: Cobro[]
}
