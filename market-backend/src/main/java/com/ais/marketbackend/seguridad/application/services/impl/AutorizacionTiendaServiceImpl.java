package com.ais.marketbackend.seguridad.application.services.impl;

import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.service.ContextoAutenticacion;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AutorizacionTiendaServiceImpl implements AutorizacionTiendaService {

    private final ContextoAutenticacion contextoAutenticacion;
    private final PermisosEfectivosResolver permisosEfectivosResolver;

    public AutorizacionTiendaServiceImpl(
            ContextoAutenticacion contextoAutenticacion, PermisosEfectivosResolver permisosEfectivosResolver) {
        this.contextoAutenticacion = contextoAutenticacion;
        this.permisosEfectivosResolver = permisosEfectivosResolver;
    }

    @Override
    public void exigirAcceso(Long tiendaId) {
        if (!tieneAcceso(tiendaId)) {
            throw new AccessDeniedException("Tienda fuera de alcance: " + tiendaId);
        }
    }

    @Override
    public void exigirAccesoATodas(Collection<Long> tiendaIds) {
        tiendaIds.forEach(this::exigirAcceso);
    }

    @Override
    public void exigirAccesoAGrupo(Long grupoId) {
        PermisosEfectivos permisos = permisosEfectivosResolver.resolver(contextoAutenticacion.usuarioIdActual());
        if (!permisos.puedeAccederAGrupo(grupoId)) {
            throw new AccessDeniedException("Grupo de tiendas fuera de alcance: " + grupoId);
        }
    }

    @Override
    public boolean tieneAcceso(Long tiendaId) {
        PermisosEfectivos permisos = permisosEfectivosResolver.resolver(contextoAutenticacion.usuarioIdActual());
        return permisos.puedeAccederATienda(tiendaId);
    }

    @Override
    public Optional<Set<Long>> tiendaIdsPermitidas() {
        PermisosEfectivos permisos = permisosEfectivosResolver.resolver(contextoAutenticacion.usuarioIdActual());
        return permisos.alcanceGlobal() ? Optional.empty() : Optional.of(permisos.tiendaIds());
    }
}
