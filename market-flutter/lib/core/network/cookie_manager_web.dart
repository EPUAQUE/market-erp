import 'package:dio/dio.dart';

/// No-op en web — el navegador ya guarda y reenvía la cookie de refresh por
/// su cuenta (ver `with_credentials.dart`), Dio no necesita gestionarla acá.
Interceptor? crearCookieManager() => null;

Future<void> limpiarCookies() async {}
