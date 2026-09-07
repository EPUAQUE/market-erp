import 'package:decimal/decimal.dart';
import '../data/caja.dart';

/// De dónde vino un [MovimientoCaja] — el backend no lo guarda como campo
/// estructurado, solo como texto libre en `concepto`
/// (`VentaServiceImpl.completar()`/`CuentaPorCobrarServiceImpl.registrarCobro()`,
/// market-backend), así que se reconstruye acá parseando ese texto. Un pago
/// MIXTO genera un [MovimientoCaja] separado por cada canal — cada uno ya
/// llega con su propio método, no hay que desglosar nada acá.
enum OrigenMovimientoCaja {
  ventaEfectivo,
  ventaOtroMetodo,
  cobroEfectivo,
  cobroOtroMetodo,
  manual,
}

class MovimientoCajaClasificado {
  const MovimientoCajaClasificado({
    required this.movimiento,
    required this.origen,
  });

  final MovimientoCaja movimiento;
  final OrigenMovimientoCaja origen;

  /// Venta o cobro de cuenta por cobrar cobrados en efectivo — lo único que
  /// de verdad hay que contar/reconciliar en la gaveta al cerrar caja. Un
  /// egreso nunca cae acá (el `tipo` se revisa antes de intentar el patrón).
  bool get esVentaOCobroEnEfectivo =>
      origen == OrigenMovimientoCaja.ventaEfectivo ||
      origen == OrigenMovimientoCaja.cobroEfectivo;
}

final _regexVenta = RegExp(r'^Venta #\d+ \((\w+)\)$');
final _regexCobro = RegExp(r'^Cobro cuenta por cobrar #\d+ \((\w+)\)$');

OrigenMovimientoCaja _clasificarOrigen(MovimientoCaja movimiento) {
  if (movimiento.tipo != TipoMovimientoCaja.ingreso) {
    return OrigenMovimientoCaja.manual;
  }

  final matchVenta = _regexVenta.firstMatch(movimiento.concepto);
  if (matchVenta != null) {
    return matchVenta.group(1) == 'EFECTIVO'
        ? OrigenMovimientoCaja.ventaEfectivo
        : OrigenMovimientoCaja.ventaOtroMetodo;
  }

  final matchCobro = _regexCobro.firstMatch(movimiento.concepto);
  if (matchCobro != null) {
    return matchCobro.group(1) == 'EFECTIVO'
        ? OrigenMovimientoCaja.cobroEfectivo
        : OrigenMovimientoCaja.cobroOtroMetodo;
  }

  return OrigenMovimientoCaja.manual;
}

List<MovimientoCajaClasificado> clasificarMovimientos(
  List<MovimientoCaja> movimientos,
) {
  return movimientos
      .map(
        (m) => MovimientoCajaClasificado(
          movimiento: m,
          origen: _clasificarOrigen(m),
        ),
      )
      .toList();
}

/// Ventas/cobros en efectivo de la sesión — lo que hay que ver reflejado en
/// la gaveta física al cerrar. Tarjeta/transferencia sí suman al saldo de
/// caja (ver `VentaServiceImpl.completar()`) pero no son efectivo, así que
/// no cuentan para esta lista aunque el backend los trate igual como
/// "ingreso".
List<MovimientoCajaClasificado> ventasYCobrosEnEfectivo(
  List<MovimientoCaja> movimientos,
) {
  return clasificarMovimientos(
    movimientos,
  ).where((c) => c.esVentaOCobroEnEfectivo).toList();
}

Decimal totalVentasYCobrosEnEfectivo(List<MovimientoCaja> movimientos) {
  return ventasYCobrosEnEfectivo(
    movimientos,
  ).fold(Decimal.zero, (acc, c) => acc + c.movimiento.monto);
}
