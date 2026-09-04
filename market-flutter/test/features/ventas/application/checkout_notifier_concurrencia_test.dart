import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/connectivity/backend_reachability_provider.dart';
import 'package:market_pos/features/productos/data/producto_catalogo.dart';
import 'package:market_pos/features/ventas/application/carrito_notifier.dart';
import 'package:market_pos/features/ventas/application/checkout_notifier.dart';
import 'package:market_pos/features/ventas/data/venta_api.dart';
import 'package:decimal/decimal.dart';

import '../../../support/venta_sync_fakes.dart';

/// "Dos dispositivos generan operaciones simultáneamente" (Fase 2,
/// PLAN_MEJORAS.md). Cada dispositivo real es su propio proceso de app —
/// acá cada uno es su propio `ProviderContainer` (carrito y estado de
/// checkout completamente aislados, igual que dos tablets distintas), pero
/// ambos apuntan a la MISMA instancia de `FakeVentaApiServidor`: un solo
/// backend compartido, como en la realidad. Las llamadas se disparan con
/// `Future.wait` (no una tras otra) para que de verdad se entrelacen en el
/// event loop en vez de ejecutarse en serie.
///
/// La concurrencia de stock/saldos ya está probada exhaustivamente del lado
/// del backend real contra Postgres (Fase 3, `market-backend/docs/plan-mejoras.md`
/// — `VentaCreditoConcurrenciaIT`, `CajaConcurrenciaIT`, etc., y 8 `POST
/// /ventas` concurrentes reales con el mismo `correlationId` devolviendo la
/// misma venta). Lo que le toca a este cliente es más acotado: que dos
/// dispositivos con `correlationId` distintos nunca se pisen entre sí, y
/// que una coincidencia de `correlationId` (cada dispositivo genera el suyo
/// con UUID v4 — astronómicamente improbable, pero no imposible) no deje a
/// ninguno de los dos viendo un error de venta fallida.
ProductoCatalogo _producto() => ProductoCatalogo(
  productoId: 1,
  codigoInterno: 'P001',
  codigoBarras: null,
  nombre: 'Producto 1',
  descripcionCorta: null,
  imagenUrl: null,
  precioVenta: Decimal.parse('8.50'),
  existenciaActual: Decimal.parse('10'),
  permitirVenta: true,
  categoriaId: 1,
);

ProviderContainer _dispositivo(FakeVentaApiServidor apiCompartida) {
  return ProviderContainer(
    overrides: [
      backendAlcanzableProvider.overrideWith((ref) => Stream.value(true)),
      ventaApiProvider.overrideWithValue(apiCompartida),
    ],
  );
}

void main() {
  group('Dos dispositivos simultáneos (Fase 2, PLAN_MEJORAS.md)', () {
    test('con correlationId distintos: cada venta se crea independiente, '
        'ninguna interfiere con la otra', () async {
      final apiCompartida = FakeVentaApiServidor();
      final dispositivo1 = _dispositivo(apiCompartida);
      final dispositivo2 = _dispositivo(apiCompartida);
      addTearDown(dispositivo1.dispose);
      addTearDown(dispositivo2.dispose);

      dispositivo1.read(carritoProvider.notifier).agregarProducto(_producto());
      dispositivo2.read(carritoProvider.notifier).agregarProducto(_producto());

      await Future.wait([
        dispositivo1
            .read(checkoutProvider.notifier)
            .confirmar(
              tiendaId: 1,
              metodo: MetodoPago.efectivo,
              clienteId: 42,
              correlationId: 'dispositivo-1-venta',
            ),
        dispositivo2
            .read(checkoutProvider.notifier)
            .confirmar(
              tiendaId: 1,
              metodo: MetodoPago.efectivo,
              clienteId: 42,
              correlationId: 'dispositivo-2-venta',
            ),
      ]);

      expect(apiCompartida.ventas, hasLength(2));
      expect(
        apiCompartida.ventas.every((v) => v.estado == 'COMPLETADA'),
        isTrue,
      );
      expect(apiCompartida.ventas.map((v) => v.correlationId).toSet(), {
        'dispositivo-1-venta',
        'dispositivo-2-venta',
      });

      expect(dispositivo1.read(checkoutProvider).error, isNull);
      expect(dispositivo1.read(checkoutProvider).ventaCompletada, isTrue);
      expect(dispositivo2.read(checkoutProvider).error, isNull);
      expect(dispositivo2.read(checkoutProvider).ventaCompletada, isTrue);
    });

    test('coincidencia de correlationId entre los dos dispositivos: ambos '
        'terminan apuntando a la misma venta, ninguno ve error', () async {
      final apiCompartida = FakeVentaApiServidor();
      final dispositivo1 = _dispositivo(apiCompartida);
      final dispositivo2 = _dispositivo(apiCompartida);
      addTearDown(dispositivo1.dispose);
      addTearDown(dispositivo2.dispose);

      dispositivo1.read(carritoProvider.notifier).agregarProducto(_producto());
      dispositivo2.read(carritoProvider.notifier).agregarProducto(_producto());

      const claveColisionada = 'coincidencia-uuid-improbable';
      await Future.wait([
        dispositivo1
            .read(checkoutProvider.notifier)
            .confirmar(
              tiendaId: 1,
              metodo: MetodoPago.efectivo,
              clienteId: 42,
              correlationId: claveColisionada,
            ),
        dispositivo2
            .read(checkoutProvider.notifier)
            .confirmar(
              tiendaId: 1,
              metodo: MetodoPago.efectivo,
              clienteId: 42,
              correlationId: claveColisionada,
            ),
      ]);

      // Una sola venta real — la segunda llamada a completar() sobre la
      // misma venta se resuelve como éxito vía `ventaYaQuedoCompletada`
      // (la misma reconciliación de la respuesta perdida), no como error.
      expect(apiCompartida.ventas, hasLength(1));
      expect(apiCompartida.ventas.single.estado, 'COMPLETADA');
      expect(apiCompartida.llamadasObtener, greaterThanOrEqualTo(1));

      expect(dispositivo1.read(checkoutProvider).error, isNull);
      expect(dispositivo1.read(checkoutProvider).ventaCompletada, isTrue);
      expect(dispositivo2.read(checkoutProvider).error, isNull);
      expect(dispositivo2.read(checkoutProvider).ventaCompletada, isTrue);
    });
  });
}
