export type EstadoUsuario = 'ACTIVO' | 'INACTIVO' | 'BLOQUEADO'

export interface Usuario {
  id: number
  username: string
  estado: EstadoUsuario
  nombre: string | null
  telefono: string | null
  correo: string | null
}

export interface UsuarioTienda {
  id: number
  tiendaId: number
  rolId: number
  rolNombre: string
}

export interface UsuarioGrupo {
  id: number
  grupoTiendaId: number
  rolId: number
  rolNombre: string
}
