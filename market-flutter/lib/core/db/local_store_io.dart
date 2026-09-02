import 'package:decimal/decimal.dart';
import 'package:isar_community/isar.dart';
import 'package:path_provider/path_provider.dart';
import '../../features/caja/data/caja.dart';
import '../../features/caja/data/movimiento_caja_pendiente_isar.dart';
import '../../features/caja/data/movimiento_caja_pendiente_local.dart';
import '../../features/clientes/data/cliente_pendiente_isar.dart';
import '../../features/clientes/data/cliente_pendiente_local.dart';
import '../../features/productos/data/producto_catalogo.dart';
import '../../features/productos/data/producto_catalogo_isar.dart';
import '../../features/ventas/data/venta_pendiente_isar.dart';
import '../../features/ventas/data/venta_pendiente_local.dart';
import '../../features/ventas/domain/carrito.dart';
import 'local_schema_version.dart';
import 'local_store.dart';
import 'metadato_local_isar.dart';

Future<LocalStore> crearLocalStore() async {
  final directorio = await getApplicationDocumentsDirectory();
  final isar = await Isar.open([
    ProductoCatalogoIsarSchema,
    VentaPendienteIsarSchema,
    MovimientoCajaPendienteIsarSchema,
    ClientePendienteIsarSchema,
    MetadatoLocalIsarSchema,
  ], directory: directorio.path);
  await _aplicarMigracionSiHaceFalta(isar);
  return IsarLocalStore(isar);
}

/// Ver `local_schema_version.dart` para la política completa. Corre una vez
/// por apertura de Isar, antes de que cualquier otra parte de la app lea o
/// escriba — así una limpieza (cuando es segura) nunca compite con una
/// escritura real a mitad de vuelo.
Future<void> _aplicarMigracionSiHaceFalta(Isar isar) async {
  final metadato = await isar.metadatoLocalIsars.get(0);
  if (metadato != null &&
      metadato.esquemaVersion == esquemaLocalVersionActual) {
    return;
  }

  if (metadato != null) {
    // Hay una versión anterior registrada y no coincide — solo es seguro
    // limpiar el mirror local si no hay ninguna venta/cliente/movimiento
    // real todavía sin sincronizar (ver local_schema_version.dart).
    final hayPendientes = await _hayAlgoPendiente(isar);
    if (!hayPendientes) {
      await isar.writeTxn(() async {
        await isar.productoCatalogoIsars.clear();
        await isar.ventaPendienteIsars.clear();
        await isar.movimientoCajaPendienteIsars.clear();
        await isar.clientePendienteIsars.clear();
      });
    }
  }

  await isar.writeTxn(
    () => isar.metadatoLocalIsars.put(
      MetadatoLocalIsar()..esquemaVersion = esquemaLocalVersionActual,
    ),
  );
}

Future<bool> _hayAlgoPendiente(Isar isar) async {
  final ventas = await isar.ventaPendienteIsars.count();
  final movimientos = await isar.movimientoCajaPendienteIsars.count();
  // clienteServidorIdIsNull(): un cliente ya sincronizado se conserva
  // solo como mapeo (ver ClientePendienteIsar) — no cuenta como pendiente.
  final clientes = await isar.clientePendienteIsars
      .filter()
      .clienteServidorIdIsNull()
      .count();
  return ventas > 0 || movimientos > 0 || clientes > 0;
}

class IsarLocalStore implements LocalStore {
  IsarLocalStore(this._isar);

  final Isar _isar;

  @override
  bool get disponible => true;

  @override
  Future<void> guardarCatalogo(
    int tiendaId,
    List<ProductoCatalogo> productos,
  ) async {
    await _isar.writeTxn(() async {
      await _isar.productoCatalogoIsars
          .filter()
          .tiendaIdEqualTo(tiendaId)
          .deleteAll();
      await _isar.productoCatalogoIsars.putAll(
        productos
            .map(
              (p) => ProductoCatalogoIsar()
                ..productoId = p.productoId
                ..tiendaId = tiendaId
                ..codigoInterno = p.codigoInterno
                ..codigoBarras = p.codigoBarras
                ..nombre = p.nombre
                ..descripcionCorta = p.descripcionCorta
                ..imagenUrl = p.imagenUrl
                ..precioVenta = p.precioVenta.toString()
                ..existenciaActual = p.existenciaActual.toString()
                ..permitirVenta = p.permitirVenta
                ..categoriaId = p.categoriaId,
            )
            .toList(),
      );
    });
  }

  @override
  Future<List<ProductoCatalogo>> leerCatalogo(int tiendaId) async {
    final locales = await _isar.productoCatalogoIsars
        .filter()
        .tiendaIdEqualTo(tiendaId)
        .findAll();
    return locales
        .map(
          (l) => ProductoCatalogo(
            productoId: l.productoId,
            codigoInterno: l.codigoInterno,
            codigoBarras: l.codigoBarras,
            nombre: l.nombre,
            descripcionCorta: l.descripcionCorta,
            imagenUrl: l.imagenUrl,
            precioVenta: Decimal.parse(l.precioVenta),
            existenciaActual: Decimal.parse(l.existenciaActual),
            permitirVenta: l.permitirVenta,
            categoriaId: l.categoriaId,
          ),
        )
        .toList();
  }

  @override
  Future<void> encolarVentaPendiente(NuevaVentaPendiente venta) async {
    final pendiente = VentaPendienteIsar()
      ..correlationId = venta.correlationId
      ..tiendaId = venta.tiendaId
      ..clienteId = venta.clienteId
      ..clientePendienteLocalId = venta.clientePendienteLocalId
      ..lineas = venta.lineas
          .map(
            (l) => LineaCarritoIsar()
              ..productoId = l.productoId
              ..nombre = l.nombre
              ..precioUnitario = l.precioUnitario.toString()
              ..cantidad = l.cantidad.toString(),
          )
          .toList()
      ..metodoPago = venta.metodoPago
      ..montoACobrar = venta.montoACobrar?.toString()
      ..creadaEn = venta.creadaEn;
    await _isar.writeTxn(() => _isar.ventaPendienteIsars.put(pendiente));
  }

  @override
  Future<List<VentaPendienteLocal>> listarVentasPendientes() async {
    final pendientes = await _isar.ventaPendienteIsars
        .where()
        .filter()
        .mensajeErrorIsNull()
        .sortByCreadaEn()
        .findAll();
    return pendientes.map(_aPlano).toList();
  }

  @override
  Future<void> marcarVentaPendienteConError(int id, String mensaje) async {
    final venta = await _isar.ventaPendienteIsars.get(id);
    if (venta == null) return;
    venta.mensajeError = mensaje;
    await _isar.writeTxn(() => _isar.ventaPendienteIsars.put(venta));
  }

  @override
  Future<List<VentaPendienteLocal>> listarVentasPendientesConError() async {
    final pendientes = await _isar.ventaPendienteIsars
        .where()
        .filter()
        .mensajeErrorIsNotNull()
        .sortByCreadaEn()
        .findAll();
    return pendientes.map(_aPlano).toList();
  }

  @override
  Future<void> reintentarVentaPendiente(int id) async {
    final venta = await _isar.ventaPendienteIsars.get(id);
    if (venta == null) return;
    venta.mensajeError = null;
    await _isar.writeTxn(() => _isar.ventaPendienteIsars.put(venta));
  }

  @override
  Future<void> eliminarVentaPendiente(int id) async {
    await _isar.writeTxn(() => _isar.ventaPendienteIsars.delete(id));
  }

  @override
  Future<int> contarVentasPendientes() => _isar.ventaPendienteIsars.count();

  @override
  Future<void> encolarMovimientoCajaPendiente(
    NuevoMovimientoCajaPendiente movimiento,
  ) async {
    final pendiente = MovimientoCajaPendienteIsar()
      ..tiendaId = movimiento.tiendaId
      ..tipo = movimiento.tipo.name
      ..concepto = movimiento.concepto
      ..monto = movimiento.monto.toString()
      ..creadaEn = movimiento.creadaEn;
    await _isar.writeTxn(
      () => _isar.movimientoCajaPendienteIsars.put(pendiente),
    );
  }

  @override
  Future<List<MovimientoCajaPendienteLocal>>
  listarMovimientosCajaPendientes() async {
    final pendientes = await _isar.movimientoCajaPendienteIsars
        .where()
        .filter()
        .mensajeErrorIsNull()
        .sortByCreadaEn()
        .findAll();
    return pendientes.map(_aPlanoMovimiento).toList();
  }

  @override
  Future<void> marcarMovimientoCajaPendienteConError(
    int id,
    String mensaje,
  ) async {
    final movimiento = await _isar.movimientoCajaPendienteIsars.get(id);
    if (movimiento == null) return;
    movimiento.mensajeError = mensaje;
    await _isar.writeTxn(
      () => _isar.movimientoCajaPendienteIsars.put(movimiento),
    );
  }

  @override
  Future<List<MovimientoCajaPendienteLocal>>
  listarMovimientosCajaPendientesConError() async {
    final pendientes = await _isar.movimientoCajaPendienteIsars
        .where()
        .filter()
        .mensajeErrorIsNotNull()
        .sortByCreadaEn()
        .findAll();
    return pendientes.map(_aPlanoMovimiento).toList();
  }

  @override
  Future<void> reintentarMovimientoCajaPendiente(int id) async {
    final movimiento = await _isar.movimientoCajaPendienteIsars.get(id);
    if (movimiento == null) return;
    movimiento.mensajeError = null;
    await _isar.writeTxn(
      () => _isar.movimientoCajaPendienteIsars.put(movimiento),
    );
  }

  @override
  Future<void> eliminarMovimientoCajaPendiente(int id) async {
    await _isar.writeTxn(() => _isar.movimientoCajaPendienteIsars.delete(id));
  }

  @override
  Future<int> contarMovimientosCajaPendientes() =>
      _isar.movimientoCajaPendienteIsars.count();

  MovimientoCajaPendienteLocal _aPlanoMovimiento(
    MovimientoCajaPendienteIsar m,
  ) {
    return MovimientoCajaPendienteLocal(
      id: m.id,
      tiendaId: m.tiendaId,
      tipo: TipoMovimientoCaja.values.byName(m.tipo),
      concepto: m.concepto,
      monto: Decimal.parse(m.monto),
      creadaEn: m.creadaEn,
      mensajeError: m.mensajeError,
    );
  }

  @override
  Future<int> encolarClientePendiente(NuevoClientePendiente cliente) async {
    final pendiente = ClientePendienteIsar()
      ..nombre = cliente.nombre
      ..telefono = cliente.telefono
      ..nit = cliente.nit
      ..limiteCredito = cliente.limiteCredito?.toString()
      ..creadaEn = cliente.creadaEn;
    return _isar.writeTxn(() => _isar.clientePendienteIsars.put(pendiente));
  }

  @override
  Future<List<ClientePendienteLocal>> listarClientesPendientes() async {
    final pendientes = await _isar.clientePendienteIsars
        .where()
        .filter()
        .mensajeErrorIsNull()
        .and()
        .clienteServidorIdIsNull()
        .sortByCreadaEn()
        .findAll();
    return pendientes.map(_aPlanoCliente).toList();
  }

  @override
  Future<void> marcarClientePendienteConError(int id, String mensaje) async {
    final cliente = await _isar.clientePendienteIsars.get(id);
    if (cliente == null) return;
    cliente.mensajeError = mensaje;
    await _isar.writeTxn(() => _isar.clientePendienteIsars.put(cliente));
  }

  @override
  Future<void> marcarClientePendienteSincronizado(
    int id,
    int clienteServidorId,
  ) async {
    final cliente = await _isar.clientePendienteIsars.get(id);
    if (cliente == null) return;
    cliente.clienteServidorId = clienteServidorId;
    await _isar.writeTxn(() => _isar.clientePendienteIsars.put(cliente));
  }

  @override
  Future<ClientePendienteLocal?> obtenerClientePendiente(int id) async {
    final cliente = await _isar.clientePendienteIsars.get(id);
    return cliente == null ? null : _aPlanoCliente(cliente);
  }

  @override
  Future<List<ClientePendienteLocal>> listarClientesPendientesConError() async {
    final pendientes = await _isar.clientePendienteIsars
        .where()
        .filter()
        .mensajeErrorIsNotNull()
        .sortByCreadaEn()
        .findAll();
    return pendientes.map(_aPlanoCliente).toList();
  }

  @override
  Future<void> reintentarClientePendiente(int id) async {
    final cliente = await _isar.clientePendienteIsars.get(id);
    if (cliente == null) return;
    cliente.mensajeError = null;
    await _isar.writeTxn(() => _isar.clientePendienteIsars.put(cliente));
  }

  @override
  Future<void> eliminarClientePendiente(int id) async {
    await _isar.writeTxn(() => _isar.clientePendienteIsars.delete(id));
  }

  /// Excluye los ya sincronizados (`clienteServidorId` no nulo) — esas filas
  /// se conservan solo como mapeo para ventas que aún las referencian (ver
  /// `marcarClientePendienteSincronizado`), no cuentan como "pendiente".
  @override
  Future<int> contarClientesPendientes() =>
      _isar.clientePendienteIsars.filter().clienteServidorIdIsNull().count();

  @override
  Future<void> limpiarClientesPendientesSincronizadosSinReferencia() async {
    final referenciados = await _isar.ventaPendienteIsars
        .filter()
        .clientePendienteLocalIdIsNotNull()
        .findAll();
    final idsReferenciados = referenciados
        .map((v) => v.clientePendienteLocalId!)
        .toSet();

    final sincronizados = await _isar.clientePendienteIsars
        .filter()
        .clienteServidorIdIsNotNull()
        .findAll();
    final idsABorrar = sincronizados
        .map((c) => c.id)
        .where((id) => !idsReferenciados.contains(id))
        .toList();
    if (idsABorrar.isEmpty) return;

    await _isar.writeTxn(
      () => _isar.clientePendienteIsars.deleteAll(idsABorrar),
    );
  }

  @override
  Future<void> limpiarTodo() async {
    await _isar.writeTxn(() async {
      await _isar.productoCatalogoIsars.clear();
      await _isar.ventaPendienteIsars.clear();
      await _isar.movimientoCajaPendienteIsars.clear();
      await _isar.clientePendienteIsars.clear();
    });
  }

  ClientePendienteLocal _aPlanoCliente(ClientePendienteIsar c) {
    return ClientePendienteLocal(
      id: c.id,
      nombre: c.nombre,
      telefono: c.telefono,
      nit: c.nit,
      limiteCredito: c.limiteCredito != null
          ? Decimal.parse(c.limiteCredito!)
          : null,
      creadaEn: c.creadaEn,
      mensajeError: c.mensajeError,
      clienteServidorId: c.clienteServidorId,
    );
  }

  VentaPendienteLocal _aPlano(VentaPendienteIsar v) {
    return VentaPendienteLocal(
      id: v.id,
      correlationId: v.correlationId,
      tiendaId: v.tiendaId,
      clienteId: v.clienteId,
      clientePendienteLocalId: v.clientePendienteLocalId,
      lineas: v.lineas
          .map(
            (l) => LineaCarrito(
              productoId: l.productoId,
              nombre: l.nombre,
              precioUnitario: Decimal.parse(l.precioUnitario),
              cantidad: int.parse(l.cantidad),
            ),
          )
          .toList(),
      metodoPago: v.metodoPago,
      montoACobrar: v.montoACobrar != null
          ? Decimal.parse(v.montoACobrar!)
          : null,
      creadaEn: v.creadaEn,
      mensajeError: v.mensajeError,
    );
  }
}
