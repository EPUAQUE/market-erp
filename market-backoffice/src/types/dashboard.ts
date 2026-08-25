export interface VencimientoResumen {
  tipo: 'CUENTA_POR_COBRAR' | 'CUENTA_POR_PAGAR'
  referenciaId: number
  monto: string
  fechaVencimiento: string
}

export interface CuentaPendienteResumen {
  id: number
  contraparteId: number
  monto: string
  fechaVencimiento: string
}

export interface RecordatorioResumen {
  gastoProgramadoId: number
  concepto: string
  monto: string
  proximaFecha: string
}

export interface SugerenciaCompraResumen {
  productoId: number
  existenciaActual: string
  stockMinimo: string
  cantidadSugerida: string
}

export interface SugerenciaTrasladoResumen {
  productoId: number
  tiendaOrigenId: number
  existenciaOrigen: string
  cantidadSugerida: string
}

export interface DashboardResumen {
  tiendaId: number

  ventasHoyTotal: string
  ventasHoyCantidad: number
  ventasMesTotal: string
  ventasMesCantidad: number
  ventasMesAnteriorTotal: string
  ticketPromedioMes: string
  facturasEmitidasMes: number
  facturasFelCertificadasMes: number

  utilidadMesTotal: string | null
  margenPromedioMes: string | null

  inventarioValorizadoTotal: string
  productosAgotados: number
  productosBajoMinimo: number
  productosSinMovimiento: number

  saldoPendienteCuentasPorCobrar: string
  cuentasPorCobrarVencidas: number
  cxcAging0a30: string
  cxcAging31a60: string
  cxcAgingMas60: string

  saldoPendienteCuentasPorPagar: string
  cuentasPorPagarVencidas: number
  cxpAging0a30: string
  cxpAging31a60: string
  cxpAgingMas60: string

  cajaAbierta: boolean
  cajaSaldoEsperado: string | null
  ingresosHoy: string
  egresosHoy: string

  alertasCriticas: number
  alertasPreventivas: number

  proximosVencimientos: VencimientoResumen[]
  topCobrosPendientes: CuentaPendienteResumen[]
  topPagosPendientes: CuentaPendienteResumen[]
  recordatorios: RecordatorioResumen[]
  sugerenciasCompra: SugerenciaCompraResumen[]
  sugerenciasTraslado: SugerenciaTrasladoResumen[]
}
