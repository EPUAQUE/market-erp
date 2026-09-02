package com.ais.marketbackend.fel.infrastructure.certificador;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condición de registro para {@link DevCertificadorFelAdapter}: activo fuera de
 * {@code prod} sin condición, y dentro de {@code prod} solo si
 * {@code app.fel.requerido-real=false} (bandera temporal — ver
 * {@link FelProdSafetyGuard} y docs/plan-mejoras.md Fase 1). Reemplaza el
 * {@code @Profile("!prod")} original: ese no puede expresar "o bien no es prod,
 * o es prod pero con la bandera puesta", que sí necesita esta condición.
 */
class FelSimuladoEnProdCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return true;
        }
        return !environment.getProperty("app.fel.requerido-real", Boolean.class, true);
    }
}
