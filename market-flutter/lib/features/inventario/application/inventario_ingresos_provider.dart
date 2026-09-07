import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../data/inventario_ingresos_api.dart';

final inventarioIngresosApiProvider = Provider<InventarioIngresosApi>(
  (ref) => InventarioIngresosApi(ApiClient.instance),
);
