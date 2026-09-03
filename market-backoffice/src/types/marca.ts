export type EstadoMarca = 'ACTIVA' | 'INACTIVA'

export interface Marca {
  id: number
  nombre: string
  estado: EstadoMarca
}
