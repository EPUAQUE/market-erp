package com.ais.marketbackend.seguridad.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.seguridad.application.services.impl.AutorizacionTiendaServiceImpl;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.service.ContextoAutenticacion;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AutorizacionTiendaServiceImplTest {

    private ContextoAutenticacion contextoAutenticacion;
    private PermisosEfectivosResolver permisosEfectivosResolver;
    private AutorizacionTiendaServiceImpl autorizacionTiendaService;

    @BeforeEach
    void setUp() {
        contextoAutenticacion = mock(ContextoAutenticacion.class);
        permisosEfectivosResolver = mock(PermisosEfectivosResolver.class);
        autorizacionTiendaService = new AutorizacionTiendaServiceImpl(contextoAutenticacion, permisosEfectivosResolver);
        when(contextoAutenticacion.usuarioIdActual()).thenReturn(7L);
    }

    @Test
    void alcanceGlobalTieneAccesoATodo() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "admin", Set.of(), Set.of(), true));

        assertThat(autorizacionTiendaService.tieneAcceso(999L)).isTrue();
        autorizacionTiendaService.exigirAcceso(999L);
        autorizacionTiendaService.exigirAccesoATodas(List.of(1L, 2L, 999L));
    }

    @Test
    void alcancePorTiendaSoloPermiteSusTiendas() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "encargado", Set.of(), Set.of(1L, 2L), false));

        assertThat(autorizacionTiendaService.tieneAcceso(1L)).isTrue();
        assertThat(autorizacionTiendaService.tieneAcceso(3L)).isFalse();
    }

    @Test
    void exigirAccesoConTiendaFueraDeAlcanceLanzaAccesoDenegado() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "encargado", Set.of(), Set.of(1L), false));

        assertThatThrownBy(() -> autorizacionTiendaService.exigirAcceso(3L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void tiendaIdsPermitidasEsVacioConAlcanceGlobal() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "admin", Set.of(), Set.of(), true));

        assertThat(autorizacionTiendaService.tiendaIdsPermitidas()).isEmpty();
    }

    @Test
    void tiendaIdsPermitidasDevuelveLasTiendasConAlcancePorTienda() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "encargado", Set.of(), Set.of(1L, 2L), false));

        assertThat(autorizacionTiendaService.tiendaIdsPermitidas()).contains(Set.of(1L, 2L));
    }

    @Test
    void exigirAccesoATodasConUnaSolaTiendaFueraDeAlcanceLanzaAccesoDenegado() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "encargado", Set.of(), Set.of(1L), false));

        assertThatThrownBy(() -> autorizacionTiendaService.exigirAccesoATodas(List.of(1L, 3L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void exigirAccesoAGrupoConAlcanceGlobalNoLanza() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "admin", Set.of(), Set.of(), true));

        autorizacionTiendaService.exigirAccesoAGrupo(999L);
    }

    @Test
    void exigirAccesoAGrupoConGrupoAsignadoNoLanza() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "admin_grupo", Set.of(), Set.of(), false, Set.of(5L)));

        autorizacionTiendaService.exigirAccesoAGrupo(5L);
    }

    @Test
    void exigirAccesoAGrupoConGrupoFueraDeAlcanceLanzaAccesoDenegado() {
        when(permisosEfectivosResolver.resolver(7L))
                .thenReturn(new PermisosEfectivos(7L, "admin_grupo", Set.of(), Set.of(), false, Set.of(5L)));

        assertThatThrownBy(() -> autorizacionTiendaService.exigirAccesoAGrupo(9L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
