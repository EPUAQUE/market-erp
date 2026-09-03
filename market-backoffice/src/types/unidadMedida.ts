export type EstadoUnidadMedida = 'ACTIVA' | 'INACTIVA'

export interface UnidadMedida {
  id: number
  nombre: string
  abreviacion: string
  estado: EstadoUnidadMedida
}
