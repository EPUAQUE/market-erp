import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../productos/data/producto_catalogo.dart';
import '../domain/carrito.dart';

class CarritoNotifier extends Notifier<CarritoState> {
  @override
  CarritoState build() => const CarritoState();

  void agregarProducto(ProductoCatalogo producto, {int? cantidad}) {
    state = state.agregar(
      LineaCarrito(
        productoId: producto.productoId,
        nombre: producto.nombre,
        precioUnitario: producto.precioVenta,
        cantidad: cantidad ?? 1,
      ),
    );
  }

  void actualizarCantidad(int productoId, int cantidad) {
    state = state.actualizarCantidad(productoId, cantidad);
  }

  void incrementar(int productoId) {
    for (final linea in state.lineas) {
      if (linea.productoId == productoId) {
        state = state.actualizarCantidad(productoId, linea.cantidad + 1);
        return;
      }
    }
  }

  void decrementar(int productoId) {
    for (final linea in state.lineas) {
      if (linea.productoId == productoId) {
        state = state.actualizarCantidad(productoId, linea.cantidad - 1);
        return;
      }
    }
  }

  void quitar(int productoId) => state = state.quitar(productoId);

  void vaciar() => state = state.vaciar();
}

final carritoProvider = NotifierProvider<CarritoNotifier, CarritoState>(
  CarritoNotifier.new,
);
