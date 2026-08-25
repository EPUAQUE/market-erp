export type FrecuenciaGasto = 'SEMANAL' | 'QUINCENAL' | 'MENSUAL' | 'ANUAL'

export interface PagoGasto {
  id: number
  fecha: string
  monto: string
}

export interface GastoProgramado {
  id: number
  tiendaId: number
  concepto: string
  monto: string
  frecuencia: FrecuenciaGasto
  proximaFecha: string
  activo: boolean
  pagos: PagoGasto[]
}
