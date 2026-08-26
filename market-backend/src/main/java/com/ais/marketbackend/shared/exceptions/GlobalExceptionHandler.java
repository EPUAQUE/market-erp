package com.ais.marketbackend.shared.exceptions;

import com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException;
import com.ais.marketbackend.shared.responses.ApiErrorResponse;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String METRICA_CONFLICTO = "market.business_exception";

    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(RateLimitExcedidoException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExcedidoException ex, HttpServletRequest request) {
        ApiErrorResponse body = errorBody(ex.httpStatus(), ex.errorCode(), ex.getMessage(), request);
        return ResponseEntity.status(ex.httpStatus())
                .header("Retry-After", String.valueOf(ex.getRetryAfter().toSeconds()))
                .body(body);
    }

    /**
     * Único punto de traducción de {@link BusinessException} a HTTP — también el único
     * punto reusado para incrementar {@code market.business_exception} por
     * {@code errorCode}, pedido en la Fase 7 para conflictos de stock
     * ({@code STOCK_INSUFICIENTE}, Fase 4) e idempotencia
     * ({@code CORRELATION_ID_REUTILIZADO}, Fase 6) sin instrumentar cada excepción por
     * separado.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        meterRegistry.counter(METRICA_CONFLICTO, "codigo", ex.errorCode()).increment();
        ApiErrorResponse body = errorBody(ex.httpStatus(), ex.errorCode(), ex.getMessage(), request);
        return ResponseEntity.status(ex.httpStatus()).body(body);
    }

    /** Cubre las denegaciones multi-tienda de {@code AutorizacionTiendaServiceImpl} (Fase 2). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(HttpServletRequest request) {
        meterRegistry.counter(METRICA_CONFLICTO, "codigo", "ACCESS_DENIED").increment();
        ApiErrorResponse body = errorBody(
                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "No tiene permiso para esta operación.", request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiErrorResponse body = ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Datos de entrada inválidos.",
                request.getRequestURI(), correlationId(request), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Sin esto, cualquier ruta inexistente cae en el {@code @ExceptionHandler(Exception.class)}
     * genérico y responde 500 en vez de 404 — encontrado al probar un endpoint de
     * Actuator deliberadamente no expuesto (Fase 7): la ausencia de ruta se veía como
     * un "error interno", contaminando logs/métricas de error real.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(HttpServletRequest request) {
        ApiErrorResponse body = errorBody(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recurso no encontrado.", request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(HttpServletRequest request) {
        ApiErrorResponse body = errorBody(
                HttpStatus.BAD_REQUEST, "IMAGEN_INVALIDA", "El archivo excede el tamaño máximo permitido (5MB).",
                request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpServletRequest request) {
        ApiErrorResponse body = errorBody(
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cuerpo de la solicitud ilegible o mal formado.", request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        ApiErrorResponse body = errorBody(
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrió un error inesperado.", request);
        log.error("Error no controlado en {} [correlationId={}]", request.getRequestURI(), body.correlationId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ApiErrorResponse errorBody(HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        return ApiErrorResponse.of(status.value(), errorCode, message, request.getRequestURI(), correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-Id");
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }
}
