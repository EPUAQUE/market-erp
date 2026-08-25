export type EstadoCajaSesion = 'ABIERTA' | 'CERRADA'
export type TipoMovimientoCaja = 'INGRESO' | 'EGRESO'

export interface MovimientoCaja {
  id: number
  fecha: string
  tipo: TipoMovimientoCaja
  concepto: string
  monto: string
}

export interface CajaSesion {
  id: number
  tiendaId: number
  fechaApertura: string
  fechaCierre: string | null
  montoInicial: string
  montoFinalContado: string | null
  saldoEsperado: string
  estado: EstadoCajaSesion
  movimientos: MovimientoCaja[]
}
