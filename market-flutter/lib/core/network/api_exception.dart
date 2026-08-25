import 'package:dio/dio.dart';

/// Error normalizado — toda excepción que sale de [ApiClient] es esta, nunca
/// un [DioException] crudo, para que la capa de aplicación no dependa de Dio.
class ApiException implements Exception {
  ApiException({
    required this.message,
    this.status,
    this.code,
    this.correlationId,
    this.isNetworkError = false,
  });

  final String message;
  final int? status;
  final String? code;
  final String? correlationId;
  final bool isNetworkError;

  bool get isUnauthorized => status == 401;

  factory ApiException.fromDioException(DioException error) {
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return ApiException(
        message: 'No se pudo conectar con el servidor.',
        isNetworkError: true,
      );
    }
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return ApiException(
        message: data['message'] as String? ?? 'Ocurrió un error inesperado.',
        status: error.response?.statusCode,
        code: data['error'] as String?,
        correlationId: data['correlationId'] as String?,
      );
    }
    return ApiException(
      message: 'Ocurrió un error inesperado.',
      status: error.response?.statusCode,
    );
  }
}
