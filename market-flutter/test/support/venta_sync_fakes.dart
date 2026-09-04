import 'package:decimal/decimal.dart';
import 'package:market_pos/core/db/local_store_web.dart';
import 'package:market_pos/core/network/api_client.dart';
import 'package:market_pos/core/network/api_exception.dart';
import 'package:market_pos/features/ventas/data/venta.dart';
import 'package:market_pos/features/ventas/data/venta_api.dart';
import 'package:market_pos/features/ventas/data/venta_pendiente_local.dart';
import 'package:market_pos/features/ventas/domain/carrito.dart';

/// Fila server-side, tal como quedaría el backend real — usada tanto para
/// simular una venta creada antes de que la app se mate a mitad de
/// sincronizar como el estado en curso durante un drenado.
class VentaEnServidorFake {
  VentaEnServidorFake(this.id, this.correlationId, this.estado);
  final int id;
  final String correlationId;
  String estado;
}

/// Simula el backend real lo suficiente para probar recuperación ante
/// respuesta perdida, reintentos tras matar la app, y cortes de red a
/// mitad de sincronizar (ej. cambio de Wi-Fi a datos): `crear()` es
/// idempotente por `correlationId` (nunca crea una segunda venta para la
/// misma clave), `completar()` sobre una venta ya `COMPLETADA` responde
/// `409 ESTADO_VENTA_INVALIDO` igual que el backend real (ver CLAUDE.md,
/// "Sync retry bug"), y puede forzarse a fallar por red un número
/// configurable de veces antes de dejar pasar la llamada normalmente.
class FakeVentaApiServidor extends VentaApi {
  FakeVentaApiServidor() : super(ApiClient.instance);

  final List<VentaEnServidorFake> ventas = [];
  int _siguienteId = 1;
  int llamadasCrear = 0;
  int llamadasCompletar = 0;
  int llamadasObtener = 0;

  /// Cuántas veces más `crear()`/`completar()` deben fallar con un error de
  /// red antes de procesar la llamada normalmente — simula una conexión que
  /// se cae a mitad de sincronizar, no un fallo de negocio.
  int fallosDeRedRestantesEnCrear = 0;
  int fallosDeRedRestantesEnCompletar = 0;

  /// Precarga el estado en el que había quedado el backend para esta clave
  /// de idempotencia ANTES de este intento — sin esto, el escenario por
  /// defecto es "el backend nunca vio la venta".
  void sembrarEstadoPrevio({
    required String correlationId,
    required String estado,
  }) {
    ventas.add(VentaEnServidorFake(_siguienteId++, correlationId, estado));
  }

  VentaEnServidorFake? _porCorrelationId(String? id) {
    for (final v in ventas) {
      if (v.correlationId == id) return v;
    }
    return null;
  }

  VentaEnServidorFake _porId(int id) => ventas.firstWhere((v) => v.id == id);

  Venta _comoVenta(VentaEnServidorFake v) => Venta(
    id: v.id,
    clienteId: 42,
    estado: v.estado,
    total: Decimal.parse('8.50'),
  );

  ApiException _errorDeRed() => ApiException(
    message: 'No se pudo conectar con el servidor.',
    isNetworkError: true,
  );

  @override
  Future<Venta> crear({
    required int tiendaId,
    required int clienteId,
    required List<LineaCarrito> lineas,
    required MetodoPago metodoPago,
    String? correlationId,
  }) async {
    llamadasCrear++;
    if (fallosDeRedRestantesEnCrear > 0) {
      fallosDeRedRestantesEnCrear--;
      throw _errorDeRed();
    }
    final existente = _porCorrelationId(correlationId);
    final venta =
        existente ??
        VentaEnServidorFake(_siguienteId++, correlationId!, 'BORRADOR');
    if (existente == null) ventas.add(venta);
    return _comoVenta(venta);
  }

  @override
  Future<Venta> completar({
    required int tiendaId,
    required int ventaId,
    Map<MetodoPago, Decimal>? pagosInmediatos,
  }) async {
    llamadasCompletar++;
    if (fallosDeRedRestantesEnCompletar > 0) {
      fallosDeRedRestantesEnCompletar--;
      throw _errorDeRed();
    }
    final venta = _porId(ventaId);
    if (venta.estado == 'COMPLETADA') {
      throw ApiException(
        message:
            'La operación no es válida para una venta en estado COMPLETADA',
        status: 409,
        code: 'ESTADO_VENTA_INVALIDO',
      );
    }
    venta.estado = 'COMPLETADA';
    return _comoVenta(venta);
  }

  @override
  Future<Venta> obtener({required int tiendaId, required int ventaId}) async {
    llamadasObtener++;
    return _comoVenta(_porId(ventaId));
  }
}

/// Cola offline con una única venta pendiente precargada — simula lo que
/// quedó persistido en disco (Isar en la app real).
class FakeLocalStoreConVentaPendiente extends WebLocalStore {
  FakeLocalStoreConVentaPendiente(this._venta);

  VentaPendienteLocal? _venta;
  bool marcoError = false;
  String? mensajeErrorRecibido;

  bool get resuelta => _venta == null;

  @override
  bool get disponible => true;

  @override
  Future<List<VentaPendienteLocal>> listarVentasPendientes() async =>
      _venta == null ? const [] : [_venta!];

  @override
  Future<void> marcarVentaPendienteConError(int id, String mensaje) async {
    marcoError = true;
    mensajeErrorRecibido = mensaje;
  }

  @override
  Future<void> eliminarVentaPendiente(int id) async {
    _venta = null;
  }

  @override
  Future<int> contarVentasPendientes() async => _venta == null ? 0 : 1;
}

VentaPendienteLocal ventaPendienteFake({
  required String correlationId,
  int id = 1,
}) {
  return VentaPendienteLocal(
    id: id,
    correlationId: correlationId,
    tiendaId: 1,
    clienteId: 42,
    clientePendienteLocalId: null,
    lineas: [
      LineaCarrito(
        productoId: 1,
        nombre: 'Producto 1',
        precioUnitario: Decimal.parse('8.50'),
        cantidad: 1,
      ),
    ],
    metodoPago: MetodoPago.efectivo.name,
    montoACobrar: Decimal.parse('8.50'),
    creadaEn: DateTime(2026, 1, 1),
    mensajeError: null,
  );
}
