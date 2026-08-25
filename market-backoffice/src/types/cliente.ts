export type EstadoCliente = 'ACTIVO' | 'INACTIVO'

export interface Cliente {
  id: number
  nit: string | null
  nombre: string
  direccion: string | null
  telefono: string | null
  correo: string | null
  estado: EstadoCliente
}
