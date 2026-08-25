package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.repository.PermisoRepository;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Falla el arranque si algún {@code @RequiresPermission} referencia un código que
 * no existe en el catálogo — mejor fallar el despliegue que exponer un endpoint
 * con un permiso mal escrito o nunca dado de alta.
 *
 * <p>{@code @Qualifier("requestMappingHandlerMapping")}: con Actuator en el classpath
 * (Fase 7) hay un segundo bean de este tipo (
 * {@code controllerEndpointHandlerMapping}, para {@code @ControllerEndpoint} de
 * Actuator, que este proyecto no usa) — sin desambiguar, el arranque falla por
 * `NoUniqueBeanDefinitionException`. Detectado por {@code LiquibaseMigrationIT},
 * invisible para la suite de pruebas unitarias con mocks.
 */
@Component
public class RequiresPermissionStartupValidator {

    private final RequestMappingHandlerMapping handlerMapping;
    private final PermisoRepository permisoRepository;

    public RequiresPermissionStartupValidator(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            PermisoRepository permisoRepository) {
        this.handlerMapping = handlerMapping;
        this.permisoRepository = permisoRepository;
    }

    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void validar() {
        Set<String> faltantes = new LinkedHashSet<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            RequiresPermission anotacion = handlerMethod.getMethodAnnotation(RequiresPermission.class);
            if (anotacion == null) {
                anotacion = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
            }
            if (anotacion != null && !permisoRepository.existsByCodigo(anotacion.value())) {
                faltantes.add(anotacion.value() + " (" + handlerMethod.getMethod() + ")");
            }
        }

        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "Códigos de permiso referenciados por @RequiresPermission pero ausentes en el catálogo: "
                            + faltantes);
        }
    }
}
