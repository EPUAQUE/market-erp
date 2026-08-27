package com.ais.marketbackend.seguridad.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.model.Permiso;
import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.model.UsuarioGrupoTienda;
import com.ais.marketbackend.seguridad.domain.model.UsuarioTienda;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioGrupoTiendaRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioTiendaRepository;
import com.ais.marketbackend.tiendas.domain.repository.TiendaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PermisosEfectivosResolverImplTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioTiendaRepository usuarioTiendaRepository;
    private UsuarioGrupoTiendaRepository usuarioGrupoTiendaRepository;
    private TiendaRepository tiendaRepository;
    private PermisosEfectivosResolverImpl resolver;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioTiendaRepository = mock(UsuarioTiendaRepository.class);
        usuarioGrupoTiendaRepository = mock(UsuarioGrupoTiendaRepository.class);
        tiendaRepository = mock(TiendaRepository.class);
        resolver = new PermisosEfectivosResolverImpl(
                usuarioRepository, usuarioTiendaRepository, usuarioGrupoTiendaRepository, tiendaRepository);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(Usuario.nuevo("ana", "hash")));
    }

    @Test
    void usuarioSoloConAsignacionDeGrupoHeredaLasTiendasDelGrupo() {
        Rol rolGrupo = new Rol(9L, "ADMIN_GRUPO", false, Set.of(new Permiso(1L, "VENTAS_VER", "Ver ventas")));
        when(usuarioTiendaRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(usuarioGrupoTiendaRepository.findByUsuarioId(1L)).thenReturn(
                List.of(new UsuarioGrupoTienda(1L, 1L, 5L, rolGrupo)));
        when(tiendaRepository.listarIdsPorGrupo(5L)).thenReturn(List.of(10L, 11L));

        PermisosEfectivos permisos = resolver.resolver(1L);

        assertThat(permisos.alcanceGlobal()).isFalse();
        assertThat(permisos.grupoIds()).containsExactly(5L);
        assertThat(permisos.tiendaIds()).containsExactlyInAnyOrder(10L, 11L);
        assertThat(permisos.tienePermiso("VENTAS_VER")).isTrue();
        assertThat(permisos.puedeAccederATienda(10L)).isTrue();
        assertThat(permisos.puedeAccederAGrupo(5L)).isTrue();
    }

    @Test
    void usuarioConAsignacionDeTiendaYDeGrupoUneAmbasFuentes() {
        Rol rolTienda = new Rol(5L, "CAJERO", false, Set.of(new Permiso(2L, "CAJA_VER", "Ver caja")));
        Rol rolGrupo = new Rol(9L, "ADMIN_GRUPO", false, Set.of(new Permiso(1L, "VENTAS_VER", "Ver ventas")));
        when(usuarioTiendaRepository.findByUsuarioId(1L)).thenReturn(
                List.of(new UsuarioTienda(1L, 1L, 20L, rolTienda)));
        when(usuarioGrupoTiendaRepository.findByUsuarioId(1L)).thenReturn(
                List.of(new UsuarioGrupoTienda(2L, 1L, 5L, rolGrupo)));
        when(tiendaRepository.listarIdsPorGrupo(5L)).thenReturn(List.of(10L, 11L));

        PermisosEfectivos permisos = resolver.resolver(1L);

        assertThat(permisos.tiendaIds()).containsExactlyInAnyOrder(10L, 11L, 20L);
        assertThat(permisos.tienePermiso("CAJA_VER")).isTrue();
        assertThat(permisos.tienePermiso("VENTAS_VER")).isTrue();
    }

    @Test
    void sinNingunaAsignacionNoTieneAlcance() {
        when(usuarioTiendaRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(usuarioGrupoTiendaRepository.findByUsuarioId(1L)).thenReturn(List.of());

        PermisosEfectivos permisos = resolver.resolver(1L);

        assertThat(permisos.alcanceGlobal()).isFalse();
        assertThat(permisos.tiendaIds()).isEmpty();
        assertThat(permisos.grupoIds()).isEmpty();
    }
}
