import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:market_pos/core/network/api_exception.dart';

DioException _dioError({
  DioExceptionType type = DioExceptionType.badResponse,
  int? statusCode,
  dynamic data,
}) {
  final requestOptions = RequestOptions(path: '/api/v1/ventas');
  return DioException(
    requestOptions: requestOptions,
    type: type,
    response: statusCode == null
        ? null
        : Response(
            requestOptions: requestOptions,
            statusCode: statusCode,
            data: data,
          ),
  );
}

void main() {
  group('ApiException.fromDioException — clasificación de red', () {
    for (final type in [
      DioExceptionType.connectionError,
      DioExceptionType.connectionTimeout,
      DioExceptionType.receiveTimeout,
    ]) {
      test('$type se marca como isNetworkError, sin status', () {
        final excepcion = ApiException.fromDioException(_dioError(type: type));
        expect(excepcion.isNetworkError, isTrue);
        expect(excepcion.status, isNull);
        expect(excepcion.message, 'No se pudo conectar con el servidor.');
      });
    }

    test('sendTimeout no se clasifica como error de red', () {
      final excepcion = ApiException.fromDioException(
        _dioError(type: DioExceptionType.sendTimeout),
      );
      expect(excepcion.isNetworkError, isFalse);
    });
  });

  group('ApiException.fromDioException — respuesta del backend', () {
    test('usa message/error/correlationId del cuerpo cuando es un Map', () {
      final excepcion = ApiException.fromDioException(
        _dioError(
          statusCode: 409,
          data: {
            'message': 'La cuenta ya está cobrada.',
            'error': 'ESTADO_CUENTA_POR_COBRAR_INVALIDO',
            'correlationId': 'corr-123',
          },
        ),
      );
      expect(excepcion.message, 'La cuenta ya está cobrada.');
      expect(excepcion.status, 409);
      expect(excepcion.code, 'ESTADO_CUENTA_POR_COBRAR_INVALIDO');
      expect(excepcion.correlationId, 'corr-123');
      expect(excepcion.isNetworkError, isFalse);
    });

    test('cae a un mensaje genérico cuando el cuerpo no es un Map', () {
      final excepcion = ApiException.fromDioException(
        _dioError(statusCode: 500, data: 'texto plano'),
      );
      expect(excepcion.message, 'Ocurrió un error inesperado.');
      expect(excepcion.status, 500);
      expect(excepcion.code, isNull);
    });

    test('cae a un mensaje genérico cuando no hay response en absoluto', () {
      final excepcion = ApiException.fromDioException(_dioError());
      expect(excepcion.message, 'Ocurrió un error inesperado.');
      expect(excepcion.status, isNull);
    });
  });

  group('isUnauthorized', () {
    test('es true solo con status 401', () {
      expect(ApiException(message: 'x', status: 401).isUnauthorized, isTrue);
      expect(ApiException(message: 'x', status: 403).isUnauthorized, isFalse);
      expect(ApiException(message: 'x').isUnauthorized, isFalse);
    });
  });
}
