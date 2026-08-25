package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Aplica {@link RequiresPermission}: exige el permiso declarado y, si el endpoint
 * expone una variable de ruta {@code tiendaId}, el alcance de tienda del usuario.
 * Endpoints sin la anotación quedan autenticados (Spring Security), pero sin
 * autorización de negocio adicional — ver seguridad-desarrolladores.md §2.
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private final UsuarioRepository usuarioRepository;
    private final PermisosEfectivosResolver permisosEfectivosResolver;

    public PermissionInterceptor(
            UsuarioRepository usuarioRepository, PermisosEfectivosResolver permisosEfectivosResolver) {
        this.usuarioRepository = usuarioRepository;
        this.permisosEfectivosResolver = permisosEfectivosResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresPermission anotacion = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (anotacion == null) {
            anotacion = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        }
        if (anotacion == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No autenticado.");
        }

        String username = authentication.getName();
        Long usuarioId = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Usuario no reconocido."))
                .getId();
        PermisosEfectivos permisos = permisosEfectivosResolver.resolver(usuarioId);

        if (!permisos.tienePermiso(anotacion.value())) {
            throw new AccessDeniedException("Permiso requerido: " + anotacion.value());
        }

        Long tiendaId = extraerTiendaId(request);
        if (tiendaId != null && !permisos.puedeAccederATienda(tiendaId)) {
            throw new AccessDeniedException("Tienda fuera de alcance: " + tiendaId);
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private Long extraerTiendaId(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables)) {
            return null;
        }
        Object valor = ((Map<String, String>) variables).get("tiendaId");
        return valor == null ? null : Long.valueOf(valor.toString());
    }
}
