export interface Producto {
  id: number
  codigoInterno: string
  codigoBarras: string | null
  nombre: string
  descripcion: string | null
  descripcionCorta: string | null
  categoriaId: number
  marcaId: number
  unidadMedidaId: number
  imagenUrl: string | null
  activo: boolean
}

export interface ImportacionProductosFilaError {
  fila: number
  codigoInterno: string
  motivo: string
}

export interface ImportacionProductosResultado {
  totalFilas: number
  creados: number
  omitidos: number
  errores: ImportacionProductosFilaError[]
}

export interface ProductoTienda {
  id: number
  productoId: number
  tiendaId: number
  precioVenta: string
  stockMinimo: string
  stockMaximo: string
  permitirVenta: boolean
  permitirIngreso: boolean
  activo: boolean
}
