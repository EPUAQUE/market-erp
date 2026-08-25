import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'secure_cookie_storage.dart';

/// Un solo jar compartido entre el Dio principal y el de refresh — ambos
/// deben ver la misma cookie `refresh_token` (uno la envía en cada request,
/// el otro la manda al endpoint de refresh y guarda la que rote el backend).
final PersistCookieJar _cookieJar = PersistCookieJar(
  storage: SecureCookieStorage(),
);

/// Android/iOS nativo — Dio no gestiona cookies por su cuenta (a diferencia
/// del navegador en web, ver `with_credentials.dart`). Esto es lo que hacía
/// que el refresh token nunca funcionara en nativo (ver CLAUDE.md, gap ya
/// cerrado).
Interceptor? crearCookieManager() => CookieManager(_cookieJar);

/// Se llama en logout — sin esto, un refresh token revocado server-side
/// seguiría cacheado en el almacén seguro del dispositivo.
Future<void> limpiarCookies() => _cookieJar.deleteAll();
