export interface LineaReporteVenta {
  ventaId: number
  clienteId: number
  fecha: string
  total: string
}

export interface ReporteVentas {
  tiendaId: number
  desde: string
  hasta: string
  totalVentas: string
  cantidadVentas: number
  lineas: LineaReporteVenta[]
}

export interface LineaReporteCompra {
  compraId: number
  proveedorId: number
  fecha: string
  total: string
}

export interface ReporteCompras {
  tiendaId: number
  desde: string
  hasta: string
  totalCompras: string
  cantidadCompras: number
  lineas: LineaReporteCompra[]
}
