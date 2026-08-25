package com.ais.marketbackend.seguridad.infrastructure.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige el código de permiso plano indicado (ver ARCHITECTURE.md §6). Si el
 * endpoint opera sobre una tienda concreta, el path debe declarar una variable
 * {@code tiendaId}: {@link PermissionInterceptor} valida también el alcance de
 * tienda del usuario autenticado, no solo el permiso.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    String value();
}
