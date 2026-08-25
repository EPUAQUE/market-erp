export type TipoNotificacion =
  | 'CUENTA_POR_PAGAR_VENCIDA'
  | 'CUENTA_POR_COBRAR_VENCIDA'
  | 'GASTO_PROGRAMADO_VENCIDO'
  | 'STOCK_BAJO'

export interface Notificacion {
  id: number
  tiendaId: number
  tipo: TipoNotificacion
  referenciaId: number
  mensaje: string
  fecha: string
  leida: boolean
  fechaLectura: string | null
}
