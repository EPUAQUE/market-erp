export type EstadoProveedor = 'ACTIVO' | 'INACTIVO'

export interface Proveedor {
  id: number
  nit: string
  nombre: string
  direccion: string | null
  telefono: string | null
  correo: string | null
  estado: EstadoProveedor
}
