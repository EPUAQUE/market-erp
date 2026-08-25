import 'package:dio/dio.dart';

/// No-op fuera de web — Android/iOS no necesitan `withCredentials`, ahí el
/// refresh token viajará vía un `CookieJar` persistente (pendiente, ver
/// CLAUDE.md) en vez de depender del navegador.
void enableWithCredentials(Dio dio) {}
