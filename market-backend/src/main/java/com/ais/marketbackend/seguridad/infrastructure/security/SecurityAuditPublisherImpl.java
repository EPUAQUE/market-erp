package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.auditoria.domain.model.AuditEvent;
import com.ais.marketbackend.auditoria.domain.model.ResultadoAuditoria;
import com.ais.marketbackend.auditoria.domain.service.AuditoriaRegistrador;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.service.SecurityAuditPublisher;
import com.ais.marketbackend.seguridad.domain.service.TipoEventoAuditoria;
import com.ais.marketbackend.shared.infrastructure.alertas.AlertaEmailService;
import com.ais.marketbackend.shared.infrastructure.web.CorrelationIdFilter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Implementación de referencia: escribe al logger {@code SECURITY_AUDIT} (como
 * siempre) y, desde Fase 7 (PLAN_MEJORAS.md), también persiste en {@code
 * audit_event} vía {@link AuditoriaRegistrador} y dispara una alerta por correo
 * para los dos tipos de severidad alta. Un solo método, cuatro efectos — los 14
 * call sites existentes en {@code AuthServiceImpl}/{@code UsuarioServiceImpl} no
 * cambiaron en nada para lograr esto.
 *
 * <p><b>Resolución de actor</b>: si hay un {@code Authentication} autenticado en
 * {@code SecurityContextHolder} (caso de {@code UsuarioServiceImpl} — todos sus
 * call sites corren detrás de {@code @RequiresPermission}, un admin ya autenticado
 * actuando sobre OTRO usuario), se usa ese. Si no (caso de {@code AuthServiceImpl}
 * — login/refresh/logout corren antes de que exista una sesión), se recurre al
 * único formato que los 8 call sites de ese archivo ya usan de forma consistente
 * ({@code "usuarioId=" + id}) — ahí el sujeto del evento Y el actor son la misma
 * persona, a diferencia de las asignaciones de {@code UsuarioServiceImpl}.
 *
 * <p>Nunca registra contraseñas, hashes ni tokens completos — solo el detalle ya
 * sanitizado que recibe.
 */
@Component
public class SecurityAuditPublisherImpl implements SecurityAuditPublisher {

    private static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Pattern USUARIO_ID_EN_DETALLE = Pattern.compile("usuarioId=(\\d+)");
    private static final Set<TipoEventoAuditoria> TIPOS_QUE_ALERTAN =
            EnumSet.of(TipoEventoAuditoria.REFRESH_REUTILIZADO, TipoEventoAuditoria.RATE_LIMIT_ALCANZADO);

    private final MeterRegistry meterRegistry;
    private final AuditoriaRegistrador auditoriaRegistrador;
    private final UsuarioRepository usuarioRepository;
    private final AlertaEmailService alertaEmailService;

    public SecurityAuditPublisherImpl(
            MeterRegistry meterRegistry, AuditoriaRegistrador auditoriaRegistrador,
            UsuarioRepository usuarioRepository, AlertaEmailService alertaEmailService) {
        this.meterRegistry = meterRegistry;
        this.auditoriaRegistrador = auditoriaRegistrador;
        this.usuarioRepository = usuarioRepository;
        this.alertaEmailService = alertaEmailService;
    }

    @Override
    public void publicar(TipoEventoAuditoria tipo, String correlationId, String detalleSanitizado) {
        SECURITY_AUDIT.info("evento={} correlationId={} detalle={}", tipo, correlationId, detalleSanitizado);
        meterRegistry.counter("market.security.evento", "tipo", tipo.name()).increment();

        String correlationIdReal = correlationIdDeMdcOFallback(correlationId);
        registrarEnAuditoria(tipo, correlationIdReal, detalleSanitizado);

        if (TIPOS_QUE_ALERTAN.contains(tipo)) {
            alertaEmailService.enviar(
                    "Alerta de seguridad: " + tipo,
                    "Tipo: " + tipo + "\nCorrelationId: " + correlationIdReal + "\nDetalle: " + detalleSanitizado);
        }
    }

    private String correlationIdDeMdcOFallback(String correlationIdPasado) {
        String deMdc = MDC.get(CorrelationIdFilter.MDC_KEY);
        return (deMdc != null && !deMdc.isBlank()) ? deMdc : correlationIdPasado;
    }

    private void registrarEnAuditoria(TipoEventoAuditoria tipo, String correlationId, String detalle) {
        Long actorId = null;
        String actorUsername = null;

        // AnonymousAuthenticationToken (login/refresh, sin JWT válido todavía) reporta
        // isAuthenticated()==true igual que una autenticación real — sin excluirlo acá,
        // LOGIN_EXITOSO terminaba con actorUsername="anonymousUser" en vez del usuario
        // real (detectado corriendo el backend local y revisando /api/v1/auditoria).
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            actorUsername = authentication.getName();
            actorId = usuarioRepository.findByUsername(actorUsername).map(Usuario::getId).orElse(null);
        } else {
            Matcher matcher = USUARIO_ID_EN_DETALLE.matcher(detalle == null ? "" : detalle);
            if (matcher.find()) {
                actorId = Long.valueOf(matcher.group(1));
                actorUsername = usuarioRepository.findById(actorId).map(Usuario::getUsername).orElse(null);
            }
        }

        ResultadoAuditoria resultado = tipo.name().endsWith("_FALLIDO") || tipo.name().endsWith("_REUTILIZADO")
                || tipo == TipoEventoAuditoria.RATE_LIMIT_ALCANZADO
                ? ResultadoAuditoria.FALLO : ResultadoAuditoria.EXITO;

        AuditEvent evento = AuditEvent.nuevo(
                actorId, actorUsername, null, tipo.name(), "SEGURIDAD", actorId != null ? actorId.toString() : null,
                resultado, correlationId, detalle);
        auditoriaRegistrador.registrar(evento);
    }
}
