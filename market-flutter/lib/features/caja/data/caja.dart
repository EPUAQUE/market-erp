import 'package:decimal/decimal.dart';

enum TipoMovimientoCaja { ingreso, egreso }

TipoMovimientoCaja tipoMovimientoCajaFromJson(String value) {
  return value == 'INGRESO'
      ? TipoMovimientoCaja.ingreso
      : TipoMovimientoCaja.egreso;
}

String tipoMovimientoCajaToJson(TipoMovimientoCaja tipo) {
  return tipo == TipoMovimientoCaja.ingreso ? 'INGRESO' : 'EGRESO';
}

class MovimientoCaja {
  const MovimientoCaja({
    required this.id,
    required this.fecha,
    required this.tipo,
    required this.concepto,
    required this.monto,
  });

  factory MovimientoCaja.fromJson(Map<String, dynamic> json) {
    return MovimientoCaja(
      id: json['id'] as int,
      fecha: DateTime.parse(json['fecha'] as String),
      tipo: tipoMovimientoCajaFromJson(json['tipo'] as String),
      concepto: json['concepto'] as String,
      monto: Decimal.parse(json['monto'] as String),
    );
  }

  final int id;
  final DateTime fecha;
  final TipoMovimientoCaja tipo;
  final String concepto;
  final Decimal monto;
}

class CajaSesion {
  const CajaSesion({
    required this.id,
    required this.montoInicial,
    required this.montoFinalContado,
    required this.saldoEsperado,
    required this.estado,
    required this.movimientos,
  });

  factory CajaSesion.fromJson(Map<String, dynamic> json) {
    return CajaSesion(
      id: json['id'] as int,
      montoInicial: Decimal.parse(json['montoInicial'] as String),
      montoFinalContado: json['montoFinalContado'] != null
          ? Decimal.parse(json['montoFinalContado'] as String)
          : null,
      saldoEsperado: Decimal.parse(json['saldoEsperado'] as String),
      estado: json['estado'] as String,
      movimientos: (json['movimientos'] as List<dynamic>)
          .map((m) => MovimientoCaja.fromJson(m as Map<String, dynamic>))
          .toList(),
    );
  }

  final int id;
  final Decimal montoInicial;
  final Decimal? montoFinalContado;
  final Decimal saldoEsperado;
  final String estado;
  final List<MovimientoCaja> movimientos;

  bool get abierta => estado == 'ABIERTA';
}
