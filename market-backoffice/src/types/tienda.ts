export type EstadoTienda = 'ACTIVA' | 'INACTIVA'

export interface Tienda {
  id: number
  codigo: string
  nombre: string
  direccion: string | null
  telefono: string | null
  correo: string | null
  estado: EstadoTienda
}
