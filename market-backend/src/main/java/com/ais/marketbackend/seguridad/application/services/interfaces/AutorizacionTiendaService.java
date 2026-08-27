package com.ais.marketbackend.seguridad.application.services.interfaces;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Puerto de aplicación para exigir el alcance de tienda del usuario autenticado desde
 * dentro de un servicio de aplicación, no solo desde el interceptor HTTP — cubre el
 * caso de invocar el servicio directamente (otro módulo, un job) sin pasar por un
 * controller anotado con {@code tiendaId} en la ruta.
 */
public interface AutorizacionTiendaService {

    /** Lanza {@link org.springframework.security.access.AccessDeniedException} si la tienda está fuera de alcance. */
    void exigirAcceso(Long tiendaId);

    /** Igual que {@link #exigirAcceso(Long)} pero para varias tiendas (p. ej. origen y destino de un traslado). */
    void exigirAccesoATodas(Collection<Long> tiendaIds);

    /**
     * Exige acceso a un grupo de tiendas completo (dashboard agregado por grupo, no
     * por tienda individual). Lanza {@link org.springframework.security.access.AccessDeniedException}
     * si el grupo está fuera de alcance.
     */
    void exigirAccesoAGrupo(Long grupoId);

    /** Para filtrar listados: no lanza, solo indica si el usuario puede acceder a esa tienda. */
    boolean tieneAcceso(Long tiendaId);

    /**
     * Para construir consultas paginadas y filtradas por tienda a nivel de base de
     * datos (filtrar en memoria después de paginar produce páginas incompletas).
     * Vacío = alcance global, sin restricción. Presente = solo esas tiendas.
     */
    Optional<Set<Long>> tiendaIdsPermitidas();
}
