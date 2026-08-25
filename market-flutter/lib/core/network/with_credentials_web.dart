import 'package:dio/browser.dart';
import 'package:dio/dio.dart';

/// El refresh token viaja en una cookie HttpOnly/Secure/SameSite=Strict (ver
/// `AuthController` en market-backend) — en web es el propio navegador quien
/// la guarda y reenvía, siempre que el adapter mande `withCredentials`.
void enableWithCredentials(Dio dio) {
  final adapter = dio.httpClientAdapter;
  if (adapter is BrowserHttpClientAdapter) {
    adapter.withCredentials = true;
  }
}
