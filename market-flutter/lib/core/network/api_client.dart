import 'package:dio/dio.dart';
import '../config/environment.dart';
import 'api_exception.dart';
import 'cookie_manager_selector.dart' as cookie_manager;
import 'token_service.dart';
import 'with_credentials.dart';

typedef UnauthorizedCallback = void Function();

/// Cliente HTTP único de la app. Refleja las convenciones del `ApiClient` del
/// backoffice: auth por `requiresAuth` (no por URL), un solo refresh en
/// vuelo compartido entre 401 concurrentes, errores siempre normalizados a
/// [ApiException].
///
/// El refresh token nunca lo toca este código directamente: viaja en una
/// cookie HttpOnly/Secure/SameSite=Strict. En web la maneja el propio
/// navegador (`with_credentials.dart`); en Android/iOS nativo, un
/// `PersistCookieJar` respaldado por almacenamiento seguro
/// (`cookie_manager_io.dart`) vía `CookieManager` de `dio_cookie_manager`.
class ApiClient {
  ApiClient._(this.dio, this._refreshDio);

  static final ApiClient instance = _build();

  final Dio dio;
  final Dio _refreshDio;

  UnauthorizedCallback? onUnauthorized;
  Future<bool>? _refreshFuture;

  static ApiClient _build() {
    final options = BaseOptions(
      baseUrl: Environment.apiBaseUrl,
      connectTimeout: Environment.apiTimeout,
      receiveTimeout: Environment.apiTimeout,
    );
    final dio = Dio(options);
    final refreshDio = Dio(options);
    enableWithCredentials(dio);
    enableWithCredentials(refreshDio);

    // Mismo jar en ambos (ver cookie_manager_io.dart) — el de refresh
    // necesita mandar y recibir la cookie tanto como el principal.
    final cookieManager = cookie_manager.crearCookieManager();
    if (cookieManager != null) {
      dio.interceptors.add(cookieManager);
      refreshDio.interceptors.add(cookieManager);
    }

    final client = ApiClient._(dio, refreshDio);
    dio.interceptors.add(client._authInterceptor());
    return client;
  }

  /// Limpia la cookie de refresh persistida — llamar siempre en logout, para
  /// que un token ya revocado server-side no siga cacheado en el
  /// dispositivo. No-op en web (el navegador controla su propia cookie).
  Future<void> clearCookies() => cookie_manager.limpiarCookies();

  Interceptor _authInterceptor() {
    return InterceptorsWrapper(
      onRequest: (options, handler) {
        final requiresAuth = options.extra['requiresAuth'] != false;
        if (requiresAuth) {
          final token = TokenService.instance.accessToken;
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
        }
        handler.next(options);
      },
      onError: (error, handler) async {
        final requiresAuth =
            error.requestOptions.extra['requiresAuth'] != false;
        final isAuthEndpoint = error.requestOptions.path.contains('/auth/');
        if (error.response?.statusCode == 401 &&
            requiresAuth &&
            !isAuthEndpoint) {
          final refreshed = await _refresh();
          if (refreshed) {
            try {
              final retryOptions = error.requestOptions;
              retryOptions.headers['Authorization'] =
                  'Bearer ${TokenService.instance.accessToken}';
              final response = await dio.fetch(retryOptions);
              return handler.resolve(response);
            } on DioException catch (retryError) {
              return handler.next(retryError);
            }
          }
          TokenService.instance.clear();
          onUnauthorized?.call();
        }
        handler.next(error);
      },
    );
  }

  Future<bool> _refresh() {
    final inFlight = _refreshFuture;
    if (inFlight != null) return inFlight;
    final future = _doRefresh();
    _refreshFuture = future;
    return future.whenComplete(() => _refreshFuture = null);
  }

  Future<bool> _doRefresh() async {
    try {
      final response = await _refreshDio.post<Map<String, dynamic>>(
        '/api/v1/auth/refresh',
      );
      final token = response.data?['accessToken'] as String?;
      if (token == null) return false;
      TokenService.instance.set(token);
      return true;
    } on DioException {
      return false;
    }
  }

  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? query,
    required T Function(dynamic data) parser,
    bool requiresAuth = true,
  }) async {
    try {
      final response = await dio.get<dynamic>(
        path,
        queryParameters: query,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );
      return parser(response.data);
    } on DioException catch (error) {
      throw ApiException.fromDioException(error);
    }
  }

  Future<T> post<T>(
    String path, {
    dynamic data,
    required T Function(dynamic data) parser,
    bool requiresAuth = true,
  }) async {
    try {
      final response = await dio.post<dynamic>(
        path,
        data: data,
        options: Options(extra: {'requiresAuth': requiresAuth}),
      );
      return parser(response.data);
    } on DioException catch (error) {
      throw ApiException.fromDioException(error);
    }
  }
}
