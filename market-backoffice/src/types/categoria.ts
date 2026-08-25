export type EstadoCategoria = 'ACTIVA' | 'INACTIVA'

export interface Categoria {
  id: number
  nombre: string
  imagen: string | null
  estado: EstadoCategoria
}
