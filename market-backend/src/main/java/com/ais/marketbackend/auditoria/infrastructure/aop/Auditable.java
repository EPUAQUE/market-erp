package com.ais.marketbackend.auditoria.infrastructure.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de {@code application/services/impl} como auditable — un evento
 * se registra en {@code audit_event} cada vez que corre, tanto si termina en éxito
 * como en excepción (ver {@link AuditoriaAspect}). Mismo espíritu que
 * {@code @RequiresPermission} + {@code PermissionInterceptor} (a nivel de
 * controller), acá a nivel de método de servicio.
 *
 * <p>{@code tiendaIdParam}/{@code entidadIdParam} referencian el NOMBRE real del
 * parámetro del método (requiere {@code -parameters} en el compilador, ya activo en
 * este proyecto — ver {@code pom.xml}), no un índice posicional. Dejar vacío
 * ({@code ""}, el default) cuando el método no tiene ese dato disponible como
 * parámetro (ej. {@code TrasladoServiceImpl.completar(Long id)} no tiene tiendaId).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Vocabulario libre, ej. "VENTA_COMPLETADA" — no reusa {@code TipoEventoAuditoria} a propósito (ese enum es solo de {@code seguridad}). */
    String accion();

    String entidad();

    String tiendaIdParam() default "";

    String entidadIdParam() default "";

    /** Cuando el id de la entidad recién creada solo existe en el valor de retorno (ej. {@code crear}), no como parámetro — se resuelve llamando a {@code .id()} por reflexión sobre el objeto devuelto. */
    boolean entidadIdFromReturn() default false;
}
