import 'package:decimal/decimal.dart';

/// Espejo de `DashboardResponse` (`GET /api/v1/dashboard/tiendas/{id}`) — un
/// resumen agregado de TIENDA, no por vendedor (el backend no tiene ese corte
/// todavía, ver CLAUDE.md). `utilidadMesTotal`/`margenPromedioMes` llegan
/// `null` para cualquier usuario sin `DASHBOARD_FINANCIERO_VER` (hoy ni
/// CAJERO ni ENCARGADO_TIENDA lo tienen — intencional, ver brief).
class DashboardResumen {
  const DashboardResumen({
    required this.ventasHoyTotal,
    required this.ventasHoyCantidad,
    required this.ventasMesTotal,
    required this.ventasMesCantidad,
    required this.ventasMesAnteriorTotal,
    required this.ticketPromedioMes,
    required this.facturasEmitidasMes,
    required this.facturasFelCertificadasMes,
    required this.utilidadMesTotal,
    required this.margenPromedioMes,
    required this.inventarioValorizadoTotal,
    required this.productosAgotados,
    required this.productosBajoMinimo,
    required this.productosSinMovimiento,
    required this.saldoPendienteCuentasPorCobrar,
    required this.cuentasPorCobrarVencidas,
    required this.cxcAging0a30,
    required this.cxcAging31a60,
    required this.cxcAgingMas60,
    required this.saldoPendienteCuentasPorPagar,
    required this.cuentasPorPagarVencidas,
    required this.cxpAging0a30,
    required this.cxpAging31a60,
    required this.cxpAgingMas60,
    required this.cajaAbierta,
    required this.cajaSaldoEsperado,
    required this.ingresosHoy,
    required this.egresosHoy,
    required this.alertasCriticas,
    required this.alertasPreventivas,
    required this.topCobrosPendientes,
    required this.topPagosPendientes,
    required this.sugerenciasCompra,
    required this.sugerenciasTraslado,
  });

  factory DashboardResumen.fromJson(Map<String, dynamic> json) {
    Decimal dec(String key) => Decimal.parse(json[key] as String);
    Decimal? decOrNull(String key) {
      final value = json[key] as String?;
      return value == null ? null : Decimal.parse(value);
    }

    return DashboardResumen(
      ventasHoyTotal: dec('ventasHoyTotal'),
      ventasHoyCantidad: json['ventasHoyCantidad'] as int,
      ventasMesTotal: dec('ventasMesTotal'),
      ventasMesCantidad: json['ventasMesCantidad'] as int,
      ventasMesAnteriorTotal: dec('ventasMesAnteriorTotal'),
      ticketPromedioMes: dec('ticketPromedioMes'),
      facturasEmitidasMes: json['facturasEmitidasMes'] as int,
      facturasFelCertificadasMes: json['facturasFelCertificadasMes'] as int,
      utilidadMesTotal: decOrNull('utilidadMesTotal'),
      margenPromedioMes: decOrNull('margenPromedioMes'),
      inventarioValorizadoTotal: dec('inventarioValorizadoTotal'),
      productosAgotados: json['productosAgotados'] as int,
      productosBajoMinimo: json['productosBajoMinimo'] as int,
      productosSinMovimiento: json['productosSinMovimiento'] as int,
      saldoPendienteCuentasPorCobrar: dec('saldoPendienteCuentasPorCobrar'),
      cuentasPorCobrarVencidas: json['cuentasPorCobrarVencidas'] as int,
      cxcAging0a30: dec('cxcAging0a30'),
      cxcAging31a60: dec('cxcAging31a60'),
      cxcAgingMas60: dec('cxcAgingMas60'),
      saldoPendienteCuentasPorPagar: dec('saldoPendienteCuentasPorPagar'),
      cuentasPorPagarVencidas: json['cuentasPorPagarVencidas'] as int,
      cxpAging0a30: dec('cxpAging0a30'),
      cxpAging31a60: dec('cxpAging31a60'),
      cxpAgingMas60: dec('cxpAgingMas60'),
      cajaAbierta: json['cajaAbierta'] as bool,
      cajaSaldoEsperado: decOrNull('cajaSaldoEsperado'),
      ingresosHoy: dec('ingresosHoy'),
      egresosHoy: dec('egresosHoy'),
      alertasCriticas: json['alertasCriticas'] as int,
      alertasPreventivas: json['alertasPreventivas'] as int,
      topCobrosPendientes: (json['topCobrosPendientes'] as List<dynamic>)
          .map((e) => CuentaPendiente.fromJson(e as Map<String, dynamic>))
          .toList(),
      topPagosPendientes: (json['topPagosPendientes'] as List<dynamic>)
          .map((e) => CuentaPendiente.fromJson(e as Map<String, dynamic>))
          .toList(),
      sugerenciasCompra: (json['sugerenciasCompra'] as List<dynamic>)
          .map((e) => SugerenciaCompra.fromJson(e as Map<String, dynamic>))
          .toList(),
      sugerenciasTraslado: (json['sugerenciasTraslado'] as List<dynamic>)
          .map((e) => SugerenciaTraslado.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  final Decimal ventasHoyTotal;
  final int ventasHoyCantidad;
  final Decimal ventasMesTotal;
  final int ventasMesCantidad;
  final Decimal ventasMesAnteriorTotal;
  final Decimal ticketPromedioMes;
  final int facturasEmitidasMes;
  final int facturasFelCertificadasMes;

  final Decimal? utilidadMesTotal;
  final Decimal? margenPromedioMes;

  final Decimal inventarioValorizadoTotal;
  final int productosAgotados;
  final int productosBajoMinimo;
  final int productosSinMovimiento;

  final Decimal saldoPendienteCuentasPorCobrar;
  final int cuentasPorCobrarVencidas;
  final Decimal cxcAging0a30;
  final Decimal cxcAging31a60;
  final Decimal cxcAgingMas60;

  final Decimal saldoPendienteCuentasPorPagar;
  final int cuentasPorPagarVencidas;
  final Decimal cxpAging0a30;
  final Decimal cxpAging31a60;
  final Decimal cxpAgingMas60;

  final bool cajaAbierta;
  final Decimal? cajaSaldoEsperado;
  final Decimal ingresosHoy;
  final Decimal egresosHoy;

  final int alertasCriticas;
  final int alertasPreventivas;

  final List<CuentaPendiente> topCobrosPendientes;
  final List<CuentaPendiente> topPagosPendientes;
  final List<SugerenciaCompra> sugerenciasCompra;
  final List<SugerenciaTraslado> sugerenciasTraslado;
}

class CuentaPendiente {
  const CuentaPendiente({
    required this.id,
    required this.contraparteId,
    required this.monto,
    required this.fechaVencimiento,
  });

  factory CuentaPendiente.fromJson(Map<String, dynamic> json) {
    return CuentaPendiente(
      id: json['id'] as int,
      contraparteId: json['contraparteId'] as int,
      monto: Decimal.parse(json['monto'] as String),
      fechaVencimiento: DateTime.parse(json['fechaVencimiento'] as String),
    );
  }

  final int id;
  final int contraparteId;
  final Decimal monto;
  final DateTime fechaVencimiento;
}

class SugerenciaCompra {
  const SugerenciaCompra({
    required this.productoId,
    required this.existenciaActual,
    required this.stockMinimo,
    required this.cantidadSugerida,
  });

  factory SugerenciaCompra.fromJson(Map<String, dynamic> json) {
    return SugerenciaCompra(
      productoId: json['productoId'] as int,
      existenciaActual: Decimal.parse(json['existenciaActual'] as String),
      stockMinimo: Decimal.parse(json['stockMinimo'] as String),
      cantidadSugerida: Decimal.parse(json['cantidadSugerida'] as String),
    );
  }

  final int productoId;
  final Decimal existenciaActual;
  final Decimal stockMinimo;
  final Decimal cantidadSugerida;
}

class SugerenciaTraslado {
  const SugerenciaTraslado({
    required this.productoId,
    required this.tiendaOrigenId,
    required this.existenciaOrigen,
    required this.cantidadSugerida,
  });

  factory SugerenciaTraslado.fromJson(Map<String, dynamic> json) {
    return SugerenciaTraslado(
      productoId: json['productoId'] as int,
      tiendaOrigenId: json['tiendaOrigenId'] as int,
      existenciaOrigen: Decimal.parse(json['existenciaOrigen'] as String),
      cantidadSugerida: Decimal.parse(json['cantidadSugerida'] as String),
    );
  }

  final int productoId;
  final int tiendaOrigenId;
  final Decimal existenciaOrigen;
  final Decimal cantidadSugerida;
}
