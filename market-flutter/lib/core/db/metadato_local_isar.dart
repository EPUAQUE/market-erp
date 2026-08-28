import 'package:isar_community/isar.dart';

part 'metadato_local_isar.g.dart';

/// Una sola fila (`id` fijo en 0) que registra con qué versión de
/// `local_schema_version.dart` se escribió la base Isar de este
/// dispositivo — nunca se expone fuera de `local_store_io.dart`.
@collection
class MetadatoLocalIsar {
  Id id = 0;

  late int esquemaVersion;
}
