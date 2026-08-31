package com.ais.marketbackend.auditoria.infrastructure.aop;

import com.ais.marketbackend.auditoria.domain.model.AuditEvent;
import com.ais.marketbackend.auditoria.domain.model.ResultadoAuditoria;
import com.ais.marketbackend.auditoria.domain.service.AuditoriaRegistrador;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Un solo aspecto para todos los métodos {@code @Auditable} — evita repetir
 * {@code auditoriaRegistrador.registrar(...)} a mano en cada uno de los ~20 métodos
 * cubiertos (ver Fase 7, PLAN_MEJORAS.md). Resuelve actor de
 * {@code SecurityContextHolder} (siempre hay uno autenticado acá — todo método
 * cubierto está detrás de {@code @RequiresPermission} en su controller),
 * tienda/entidadId por nombre real de parámetro, y {@code correlationId} de MDC
 * ({@code CorrelationIdFilter} ya lo pone ahí en cada request).
 *
 * <p><b>No es estrictamente atómico con la operación de negocio</b>: este aspecto
 * corre alrededor del proxy transaccional del método (orden por defecto de Spring
 * para aspectos sin {@code @Order} explícito), así que {@code registrar(...)} se
 * ejecuta en su PROPIA transacción, inmediatamente después de que la transacción de
 * negocio ya comiteó (caso éxito) o ya hizo rollback (caso fallo — y ahí sí es
 * exactamente el comportamiento deseado: el evento de auditoría de un fallo debe
 * sobrevivir aunque el cambio de negocio se revierta). El único riesgo real es una
 * falla de infraestructura entre ambos commits en el caso éxito, dejando una
 * operación de negocio sin su fila de auditoría — un escenario raro, y de todos
 * modos mejor que no tener auditoría. {@code SecurityAuditPublisherImpl}, que se
 * llama explícitamente desde DENTRO del método de negocio (no vía este aspecto), sí
 * escribe en la misma transacción.
 */
@Aspect
@Component
public class AuditoriaAspect {

    private final AuditoriaRegistrador auditoriaRegistrador;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaAspect(AuditoriaRegistrador auditoriaRegistrador, UsuarioRepository usuarioRepository) {
        this.auditoriaRegistrador = auditoriaRegistrador;
        this.usuarioRepository = usuarioRepository;
    }

    @Around("@annotation(auditable)")
    public Object auditar(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        Long tiendaId = extraerLong(auditable.tiendaIdParam(), paramNames, args);
        String entidadIdDeParametro = extraerString(auditable.entidadIdParam(), paramNames, args);
        String correlationId = MDC.get("correlationId");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // AnonymousAuthenticationToken reporta isAuthenticated()==true igual que una
        // autenticación real — excluido acá aunque en la práctica todo método
        // @Auditable ya está detrás de @RequiresPermission (nunca debería llegar
        // anónimo), por consistencia con SecurityAuditPublisherImpl (ver su Javadoc).
        String actorUsername = (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken))
                ? authentication.getName() : null;
        Long actorId = actorUsername != null
                ? usuarioRepository.findByUsername(actorUsername).map(Usuario::getId).orElse(null) : null;

        try {
            Object resultado = joinPoint.proceed();
            String entidadId = entidadIdDeParametro != null
                    ? entidadIdDeParametro
                    : (auditable.entidadIdFromReturn() ? extraerIdDeRetorno(resultado) : null);
            registrar(auditable, tiendaId, entidadId, ResultadoAuditoria.EXITO, actorId, actorUsername,
                    correlationId, null);
            return resultado;
        } catch (Throwable ex) {
            registrar(auditable, tiendaId, entidadIdDeParametro, ResultadoAuditoria.FALLO, actorId, actorUsername,
                    correlationId, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }

    private void registrar(
            Auditable auditable, Long tiendaId, String entidadId, ResultadoAuditoria resultado, Long actorId,
            String actorUsername, String correlationId, String detalle) {
        AuditEvent evento = AuditEvent.nuevo(
                actorId, actorUsername, tiendaId, auditable.accion(), auditable.entidad(), entidadId, resultado,
                correlationId, detalle);
        auditoriaRegistrador.registrar(evento);
    }

    private Long extraerLong(String paramName, String[] paramNames, Object[] args) {
        Object valor = extraerPorNombre(paramName, paramNames, args);
        return valor instanceof Long l ? l : null;
    }

    private String extraerString(String paramName, String[] paramNames, Object[] args) {
        Object valor = extraerPorNombre(paramName, paramNames, args);
        return valor != null ? valor.toString() : null;
    }

    private Object extraerPorNombre(String paramName, String[] paramNames, Object[] args) {
        if (paramName == null || paramName.isBlank()) {
            return null;
        }
        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i])) {
                return args[i];
            }
        }
        return null;
    }

    /** Todos los *Resumen de este proyecto son records con accessor {@code id()} — ver convención en ARCHITECTURE.md. */
    private String extraerIdDeRetorno(Object resultado) {
        if (resultado == null) {
            return null;
        }
        try {
            Method metodoId = resultado.getClass().getMethod("id");
            Object valor = metodoId.invoke(resultado);
            return valor != null ? valor.toString() : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
