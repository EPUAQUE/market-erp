export type EstadoUsuario = 'ACTIVO' | 'INACTIVO' | 'BLOQUEADO'

export interface Usuario {
  id: number
  username: string
  estado: EstadoUsuario
}
