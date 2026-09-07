import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../data/inventario_grupo_api.dart';

final inventarioGrupoApiProvider = Provider<InventarioGrupoApi>(
  (ref) => InventarioGrupoApi(ApiClient.instance),
);
