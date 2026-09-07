import 'package:decimal/decimal.dart';

/// Acepta coma o punto como separador decimal en un campo de monto.
///
/// El teclado numérico (`TextInputType.numberWithOptions(decimal: true)`)
/// en un dispositivo configurado en es-GT puede insertar coma en vez de
/// punto según el teclado del fabricante, y muchos usuarios simplemente
/// escriben coma por costumbre regional — `Decimal.tryParse` solo acepta
/// punto y devuelve `null` en silencio ante una coma, lo que en varios
/// diálogos de la app (caja, cobro, límite de crédito) se traducía en un
/// botón que no hacía absolutamente nada al tocarlo, sin ningún mensaje.
Decimal? parseDecimalInput(String texto) {
  return Decimal.tryParse(texto.trim().replaceAll(',', '.'));
}
