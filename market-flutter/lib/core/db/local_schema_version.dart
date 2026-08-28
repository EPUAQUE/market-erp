/// Versión del esquema local (Isar) — súbela cada vez que un cambio de
/// esquema NO sea aditivo: renombrar un campo, quitarlo, cambiarle el tipo,
/// o cualquier cosa que Isar no pueda reconciliar sola con los datos ya
/// escritos por una versión anterior de la app. Un campo nuevo *nullable* en
/// una colección existente, o una colección `@collection` enteramente nueva,
/// son cambios aditivos — Isar los migra solo, sin que haga falta subir esto.
///
/// `MetadatoLocalIsar` (una sola fila, `local_store_io.dart`) guarda con qué
/// versión se escribió la base Isar de este dispositivo. Al abrir la app, si
/// la versión guardada no coincide con esta:
/// - sin nada realmente pendiente (ninguna venta/cliente/movimiento de caja
///   sin sincronizar) → se limpia todo el mirror local y se arranca limpio;
///   no hay ninguna venta real que perder, el catálogo se vuelve a
///   descargar solo (network-first, ver CLAUDE.md).
/// - con algo pendiente → NUNCA se borra solo (perdería ventas/clientes/
///   movimientos reales sin sincronizar). Si el cambio de esta versión
///   necesita algo más que lo que Isar ya migra automáticamente, ese paso
///   se agrega explícitamente en `_aplicarMigracionSiHaceFalta`
///   (`local_store_io.dart`) antes de subir el número — no existe un
///   mecanismo genérico que lo resuelva solo, porque no puede: depende de
///   qué cambió exactamente.
const int esquemaLocalVersionActual = 1;
