# Market · POS

App de punto de venta (POS) para **Market**, un ERP Retail Multi-Tienda —
tablet-first (Android 10"-12"), usada por los roles **VENDEDOR** y
**ENCARGADO** para la operación diaria de venta/caja dentro de una sola
tienda. Es un cliente aparte de `market-backoffice` (Vue, administración
transversal multi-tienda) — no comparten código, ambos consumen la misma API
REST de `market-backend`.

## Este proyecto es parte de un monorepo

Este repositorio vive dentro de [`market-erp`](https://github.com/EPUAQUE/market-erp)
junto a `market-backend` y `market-backoffice`, como carpetas hermanas:

```
market-erp/
  market-backend/       (Dockerfile, docker-compose.yml, pom.xml, src/, deploy/)
  market-backoffice/
  market-flutter/       (este repo)
```

Clonar el monorepo completo si el objetivo es levantar el sistema completo
(ver `market-backend/deploy/README.md`).

## Qué hace

- Catálogo + carrito + cobro (Efectivo, Tarjeta, Transferencia, Crédito,
  Mixto), con impresión de recibo pendiente de dispositivo real.
- Cola offline: ventas, clientes nuevos y movimientos de caja se encolan
  localmente (Isar) cuando no hay red y se sincronizan solos al reconectar —
  ver "Flujos offline" abajo.
- Caja (apertura/cierre/movimientos), cuentas por cobrar (cobros sueltos,
  anulación), dashboard, escaneo de código de barras.
- Sesión con access token en memoria (nunca persistido) y refresh token
  rotatorio opaco manejado por el backend.

## Stack técnico

| Área          | Tecnología                                                  |
| ------------- | ------------------------------------------------------------ |
| Framework     | Flutter (Dart SDK `^3.10.7`)                                |
| Estado        | Riverpod (`flutter_riverpod`)                                |
| Red           | Dio + `dio_cookie_manager`/`cookie_jar` (refresh token en cookie) |
| Ruteo         | go_router                                                    |
| Almacenamiento local | Isar Community (cola offline — solo Android/iOS, no web) |
| Otros         | `flutter_secure_storage` · `decimal` (dinero, nunca floats) · `mobile_scanner` (código de barras) · `connectivity_plus` |

Sin backend propio ni mocks — habla directo con `market-backend` vía
`API_BASE_URL` (ver "Configuración" abajo).

## Requisitos

- Flutter SDK compatible con Dart `^3.10.7`.
- Un backend `market-backend` corriendo y alcanzable desde el
  dispositivo/emulador (ver `market-backend/deploy/README.md` para Docker
  Compose, o `mvn spring-boot:run` con el perfil `local`).

## Configuración

La URL del backend se pasa en tiempo de compilación, nunca hardcodeada
(`lib/core/config/environment.dart`, vía `String.fromEnvironment`):

```bash
flutter run --dart-define=API_BASE_URL=http://localhost:8080          # web/desktop
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080           # emulador Android (alias del host)
flutter run --dart-define=API_BASE_URL=http://<ip-real>:8080          # dispositivo físico en la misma red
```

Sin `--dart-define`, cae al default `http://localhost:8080` (solo sirve para
web/desktop).

## Build de producción (APK)

```bash
flutter build apk --release --dart-define=API_BASE_URL=https://<dominio-o-ip-real-del-servidor>
```

Antes de compilar contra un servidor sin TLS (el deploy de `market-backend`
hoy no tiene HTTPS — ver `market-backend/deploy/README.md`, "Pendiente:
TLS/reverse proxy"), editar el dominio/IP placeholder en
`android/app/src/main/res/xml/network_security_config.xml` para que coincida
con el servidor real — Android bloquea tráfico HTTP por defecto (API 28+) si
el dominio no está explícitamente permitido ahí. Revertir esa excepción en
cuanto el deploy tenga TLS.

## Flujos offline

`SyncEngineNotifier` (`lib/core/sync/sync_engine.dart`) drena 3 colas
independientes (ventas, clientes nuevos, movimientos de caja) al reconectar.
Un fallo de **red** detiene esa cola para reintentar en la próxima
reconexión; un fallo de **negocio** (producto ya no vendible, venta con
conflicto real, etc.) marca el ítem y no se reintenta solo — queda visible y
accionable (reintentar o descartar) en la pantalla "Pendientes con error",
alcanzable tocando el indicador de conectividad en la barra superior del POS.

Este mecanismo solo funciona en Android/iOS — en web no hay almacenamiento
local (`LocalStore.disponible == false`), así que estas tres acciones
requieren conexión.

## Estado de verificación en dispositivo real

Gran parte de este proyecto se desarrolló y verificó en Chrome (web) contra
el backend real, con partes verificadas también en un emulador Android real
cuando estuvo disponible. La sesión de desarrollo más reciente tuvo
inestabilidad recurrente del emulador (caídas silenciosas por presión de
memoria del host) — hay funcionalidad **implementada y con `flutter
analyze`/`flutter test` en verde, pero sin verificación visual en un
dispositivo/emulador real todavía**: los 2 fixes de overflow de
`pos_screen.dart`, el scroll con teclado software en `LoginScreen`, la cola
offline drenando contra un conflicto real de principio a fin, y la excepción
de cleartext/permiso de red del build de producción (sí verificados con un
`flutter build apk --release` real, pero no instalados en un dispositivo).
Ver `CLAUDE.md` para el detalle completo, sección por sección.

## Comandos

```bash
flutter pub get           # Dependencias
flutter analyze           # Lint/type-check estático
flutter test              # Pruebas (unitarias; solo hay 1 widget test hoy)
flutter build apk         # Build Android (ver "Build de producción" arriba)
flutter build web         # Build web
```

## Documentación

`CLAUDE.md` en este mismo repo es la referencia detallada de arquitectura,
decisiones y gaps conocidos — mantenida actualizada fase a fase durante el
desarrollo, mucho más granular que este README.
