export type EstadoGrupoTienda = 'ACTIVO' | 'INACTIVO'

export interface GrupoTienda {
  id: number
  codigo: string
  nombre: string
  estado: EstadoGrupoTienda
}
