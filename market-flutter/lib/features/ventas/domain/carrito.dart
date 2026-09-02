import 'package:decimal/decimal.dart';

/// Una línea del carrito. Cantidad es `int`: los productos se venden por
/// unidades enteras, nunca fraccionadas (decisión del cliente — el backend
/// también exige entero, ver `market-backend` Fase de cantidades enteras).
class LineaCarrito {
  const LineaCarrito({
    required this.productoId,
    required this.nombre,
    required this.precioUnitario,
    required this.cantidad,
  });

  final int productoId;
  final String nombre;
  final Decimal precioUnitario;
  final int cantidad;

  Decimal get subtotal => precioUnitario * cantidad.toDecimal();

  LineaCarrito conCantidad(int nuevaCantidad) {
    return LineaCarrito(
      productoId: productoId,
      nombre: nombre,
      precioUnitario: precioUnitario,
      cantidad: nuevaCantidad,
    );
  }
}

/// Estado puro del carrito — sin Flutter ni Riverpod, solo las reglas de
/// negocio de cliente (sumar/restar líneas, calcular total y cambio).
class CarritoState {
  const CarritoState({this.lineas = const []});

  final List<LineaCarrito> lineas;

  Decimal get total => lineas.fold(Decimal.zero, (acc, l) => acc + l.subtotal);

  bool get estaVacio => lineas.isEmpty;

  CarritoState agregar(LineaCarrito nueva) {
    final index = lineas.indexWhere((l) => l.productoId == nueva.productoId);
    if (index == -1) {
      return CarritoState(lineas: [...lineas, nueva]);
    }
    final actualizada = lineas[index].conCantidad(
      lineas[index].cantidad + nueva.cantidad,
    );
    final copia = [...lineas]..[index] = actualizada;
    return CarritoState(lineas: copia);
  }

  CarritoState actualizarCantidad(int productoId, int cantidad) {
    if (cantidad <= 0) return quitar(productoId);
    final copia = lineas
        .map((l) => l.productoId == productoId ? l.conCantidad(cantidad) : l)
        .toList();
    return CarritoState(lineas: copia);
  }

  CarritoState quitar(int productoId) {
    return CarritoState(
      lineas: lineas.where((l) => l.productoId != productoId).toList(),
    );
  }

  CarritoState vaciar() => const CarritoState();
}

/// Cambio a entregar en un pago en efectivo. `null` si el monto recibido no
/// alcanza — nunca se muestra un cambio negativo.
Decimal? calcularCambio({
  required Decimal total,
  required Decimal montoRecibido,
}) {
  if (montoRecibido < total) return null;
  return montoRecibido - total;
}
