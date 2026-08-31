package com.ais.marketbackend.shared.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fase 7 (PLAN_MEJORAS.md): antes de esto, el correlationId solo existía en el
 * cuerpo de una respuesta de error ({@code GlobalExceptionHandler}), generado ahí
 * mismo — nunca llegaba a los logs de una request exitosa ni se propagaba de punta
 * a punta. Corre ANTES que todo lo demás (registrado como
 * {@code FilterRegistrationBean} con {@code Ordered.HIGHEST_PRECEDENCE} en
 * {@code SecurityConfig}, fuera de la cadena de Spring Security) para que incluso un
 * 401/429 de autenticación/rate-limit tenga uno.
 *
 * <p>Header de entrada: {@code X-Correlation-Id} (ya en la allowlist CORS, ver
 * {@code app.security.cors.allowed-headers}). Se agrega también como header de
 * SALIDA en toda respuesta (antes solo en errores) y se pone en {@code MDC} para
 * que {@code %X{correlationId}} (ver {@code application.yml}) aparezca en cada
 * línea de log de la request, y para que {@code SecurityAuditPublisherImpl}/
 * {@code AuditoriaAspect} lo lean de ahí en vez de generar cada uno el suyo.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
