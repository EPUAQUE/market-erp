package com.ais.marketbackend.shared.exceptions;

import com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException;
import com.ais.marketbackend.seguridad.domain.service.SecurityAuditPublisher;
import com.ais.marketbackend.seguridad.domain.service.TipoEventoAuditoria;
import com.ais.marketbackend.shared.infrastructure.web.CorrelationIdFilter;
import com.ais.marketbackend.shared.responses.ApiErrorResponse;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.ConcurrencyFailureException;
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
    private final SecurityAuditPublisher auditPublisher;

    public GlobalExceptionHandler(MeterRegistry meterRegistry, SecurityAuditPublisher auditPublisher) {
        this.meterRegistry = meterRegistry;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Único punto donde se traduce {@code RateLimitExcedidoException} a HTTP — antes
     * de Fase 7, {@code TipoEventoAuditoria.RATE_LIMIT_ALCANZADO} estaba declarado
     * pero nunca se disparaba (confirmado al auditar el código). Acá, en vez de en
     * {@code InMemoryLoginRateLimiter}, porque ya es el punto centralizado para
     * CUALQUIER endpoint rate-limitado (mismo criterio que {@code handleBusiness} de
     * abajo para {@code market.business_exception}), no solo login.
     */
    @ExceptionHandler(RateLimitExcedidoException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExcedidoException ex, HttpServletRequest request) {
        String correlationId = correlationId(request);
        auditPublisher.publicar(
                TipoEventoAuditoria.RATE_LIMIT_ALCANZADO, correlationId, "path=" + request.getRequestURI());
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

    /**
     * Fase 3 (PLAN_MEJORAS.md): el punto de traducción para conflictos de
     * concurrencia a nivel de infraestructura, no de negocio — a diferencia de
     * {@code BusinessException} (p. ej. {@code EstadoVentaInvalidoException},
     * lanzada cuando la relectura tras un {@code PESSIMISTIC_WRITE} confirma que
     * otra transacción ya cambió el estado), esto cubre el caso en que Postgres
     * mismo aborta la transacción por contención — un deadlock detectado
     * (SQLState 40P01) o una espera de lock que agota el tiempo, ambos
     * traducidos por Spring a subclases de {@code ConcurrencyFailureException}.
     * Sin este handler, cualquiera de esos casos caía en el
     * {@code @ExceptionHandler(Exception.class)} genérico y respondía 500 —
     * indistinguible de un error real para el cliente, que en este caso solo
     * necesita reintentar la misma operación.
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrencyConflict(
            ConcurrencyFailureException ex, HttpServletRequest request) {
        meterRegistry.counter(METRICA_CONFLICTO, "codigo", "CONFLICTO_CONCURRENCIA").increment();
        ApiErrorResponse body = errorBody(
                HttpStatus.CONFLICT, "CONFLICTO_CONCURRENCIA",
                "Otra operación está modificando el mismo recurso en este momento. Intente de nuevo.", request);
        log.warn(
                "Conflicto de concurrencia en {} [correlationId={}]", request.getRequestURI(), body.correlationId(),
                ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
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

    /**
     * {@code CorrelationIdFilter} ya puso uno en MDC para toda request (Fase 7) —
     * leerlo de ahí en vez de regenerar evita que esta respuesta de error tenga un
     * correlationId distinto al que ya quedó en los logs de la misma request. El
     * header/UUID nuevo quedan solo como red de seguridad si por algo el filtro no
     * corrió (no debería pasar en producción).
     */
    private String correlationId(HttpServletRequest request) {
        String deMdc = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (deMdc != null && !deMdc.isBlank()) {
            return deMdc;
        }
        String header = request.getHeader(CorrelationIdFilter.HEADER);
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }
}
