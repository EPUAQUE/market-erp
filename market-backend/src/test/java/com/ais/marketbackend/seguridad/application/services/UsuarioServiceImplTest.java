package com.ais.marketbackend.seguridad.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.grupostienda.domain.model.EstadoGrupoTienda;
import com.ais.marketbackend.grupostienda.domain.model.GrupoTienda;
import com.ais.marketbackend.grupostienda.domain.repository.GrupoTiendaRepository;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import com.ais.marketbackend.seguridad.application.services.impl.UsuarioServiceImpl;
import com.ais.marketbackend.seguridad.domain.exception.AsignacionMixtaNoPermitidaException;
import com.ais.marketbackend.seguridad.domain.exception.UsuarioDuplicadoException;
import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.model.UsuarioGrupoTienda;
import com.ais.marketbackend.seguridad.domain.repository.RefreshTokenRepository;
import com.ais.marketbackend.seguridad.domain.repository.RolRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioGrupoTiendaRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioTiendaRepository;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import com.ais.marketbackend.seguridad.domain.service.SecurityAuditPublisher;
import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.seguridad.infrastructure.security.SeguridadProperties;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.tiendas.domain.model.Tienda;
import com.ais.marketbackend.tiendas.domain.repository.TiendaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServiceImplTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioTiendaRepository usuarioTiendaRepository;
    private UsuarioGrupoTiendaRepository usuarioGrupoTiendaRepository;
    private RolRepository rolRepository;
    private TiendaRepository tiendaRepository;
    private GrupoTiendaRepository grupoTiendaRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private AutorizacionTiendaService autorizacionTiendaService;
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioTiendaRepository = mock(UsuarioTiendaRepository.class);
        usuarioGrupoTiendaRepository = mock(UsuarioGrupoTiendaRepository.class);
        rolRepository = mock(RolRepository.class);
        tiendaRepository = mock(TiendaRepository.class);
        grupoTiendaRepository = mock(GrupoTiendaRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        PermisosEfectivosResolver resolver = mock(PermisosEfectivosResolver.class);
        SecurityAuditPublisher auditPublisher = mock(SecurityAuditPublisher.class);
        autorizacionTiendaService = mock(AutorizacionTiendaService.class);

        SeguridadProperties properties = new SeguridadProperties(
                null, null, null,
                new SeguridadProperties.PasswordPolicy(12, 64),
                null, null, null);

        usuarioService = new UsuarioServiceImpl(
                usuarioRepository, usuarioTiendaRepository, usuarioGrupoTiendaRepository, rolRepository,
                tiendaRepository, grupoTiendaRepository, refreshTokenRepository, passwordEncoder, resolver,
                auditPublisher, properties, autorizacionTiendaService);

        when(usuarioGrupoTiendaRepository.findByUsuarioId(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(List.of());
        when(usuarioTiendaRepository.findByUsuarioId(org.mockito.ArgumentMatchers.anyLong())).thenReturn(List.of());
        when(autorizacionTiendaService.tiendaIdsPermitidas()).thenReturn(Optional.empty());
        when(autorizacionTiendaService.grupoIdsPermitidas()).thenReturn(Optional.empty());
    }

    @Test
    void crearUsuarioNuevoLoPersisteConPasswordCodificada() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(false);
        when(passwordEncoder.encode("clave-larga-segura")).thenReturn("hash-codificado");
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResumen resumen = usuarioService.crear(
                "Ana", "clave-larga-segura", "Ana Pérez", "12345678", "ana@example.com");

        assertThat(resumen.username()).isEqualTo("ana");
        verify(usuarioRepository).save(any());
    }

    @Test
    void crearUsuarioDuplicadoLanzaExcepcion() {
        when(usuarioRepository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(
                "ana", "clave-larga-segura", "Ana Pérez", "12345678", "ana@example.com"))
                .isInstanceOf(UsuarioDuplicadoException.class);
    }

    @Test
    void crearUsuarioConPasswordCortaLanzaExcepcionDePolitica() {
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.crear(
                "ana", "corta", "Ana Pérez", "12345678", "ana@example.com"))
                .isInstanceOf(com.ais.marketbackend.seguridad.domain.exception.PoliticaContrasenaException.class);
    }

    @Test
    void asignarTiendaConRolExistenteFunciona() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        Rol rol = new Rol(5L, "CAJERO", false, Set.of());
        Tienda tienda = Tienda.nueva("NORTE", "Tienda Norte", null, null, null, 1L);
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(5L)).thenReturn(Optional.of(rol));
        when(tiendaRepository.findById(10L)).thenReturn(Optional.of(tienda));

        usuarioService.asignarTienda(1L, 10L, 5L);

        verify(usuarioTiendaRepository).save(any());
    }

    @Test
    void asignarTiendaSistemaNoExigeAutorizacionDelLlamador() {
        // AdminUserSeeder llama esto al arrancar la app, sin ningún usuario
        // autenticado en SecurityContextHolder — a diferencia de asignarTienda, no
        // debe consultar en absoluto autorizacionTiendaService.
        Usuario usuario = Usuario.nuevo("admin", "hash");
        Rol rolAdmin = new Rol(1L, "ADMIN", true, Set.of());
        Tienda tienda = Tienda.nueva("CENTRAL", "Tienda Central", null, null, null, 1L);
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin));
        when(tiendaRepository.findById(1L)).thenReturn(Optional.of(tienda));

        usuarioService.asignarTiendaSistema(1L, 1L, 1L);

        verify(usuarioTiendaRepository).save(any());
        verify(autorizacionTiendaService, org.mockito.Mockito.never()).exigirAcceso(any());
        verify(autorizacionTiendaService, org.mockito.Mockito.never()).tiendaIdsPermitidas();
    }

    @Test
    void asignarTiendaConRolInexistenteLanzaNoEncontrado() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarTienda(1L, 10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void asignarTiendaConUsuarioInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarTienda(1L, 10L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void asignarTiendaConTiendaInexistenteLanzaNoEncontrado() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        Rol rol = new Rol(5L, "CAJERO", false, Set.of());
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(5L)).thenReturn(Optional.of(rol));
        when(tiendaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarTienda(1L, 99L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void asignarTiendaConGrupoYaAsignadoAlUsuarioLanzaAsignacionMixta() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        Rol rol = new Rol(5L, "CAJERO", false, Set.of());
        Tienda tienda = Tienda.nueva("NORTE", "Tienda Norte", null, null, null, 1L);
        Rol rolGrupo = new Rol(9L, "ADMIN_GRUPO", false, Set.of());
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(5L)).thenReturn(Optional.of(rol));
        when(tiendaRepository.findById(10L)).thenReturn(Optional.of(tienda));
        when(usuarioGrupoTiendaRepository.findByUsuarioId(1L)).thenReturn(
                List.of(new UsuarioGrupoTienda(1L, 1L, 1L, rolGrupo)));

        assertThatThrownBy(() -> usuarioService.asignarTienda(1L, 10L, 5L))
                .isInstanceOf(AsignacionMixtaNoPermitidaException.class);
        verify(usuarioTiendaRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void listarTiendasDevuelveLasAsignacionesDelUsuario() {
        Rol rol = new Rol(5L, "CAJERO", false, Set.of());
        com.ais.marketbackend.seguridad.domain.model.UsuarioTienda asignacion =
                new com.ais.marketbackend.seguridad.domain.model.UsuarioTienda(1L, 1L, 10L, rol);
        when(usuarioTiendaRepository.findByUsuarioId(1L)).thenReturn(List.of(asignacion));

        List<com.ais.marketbackend.seguridad.application.dtos.UsuarioTiendaResumen> resultado =
                usuarioService.listarTiendas(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).tiendaId()).isEqualTo(10L);
        assertThat(resultado.get(0).rolNombre()).isEqualTo("CAJERO");
    }

    @Test
    void asignarGrupoConRolExistenteFunciona() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        Rol rol = new Rol(9L, "ADMIN_GRUPO", false, Set.of());
        GrupoTienda grupo = new GrupoTienda(1L, "PRINCIPAL", "Grupo Principal", EstadoGrupoTienda.ACTIVO);
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(9L)).thenReturn(Optional.of(rol));
        when(grupoTiendaRepository.findById(1L)).thenReturn(Optional.of(grupo));
        when(tiendaRepository.listarIdsPorGrupo(1L)).thenReturn(List.of(10L, 11L));

        usuarioService.asignarGrupo(1L, 1L, 9L);

        verify(usuarioGrupoTiendaRepository).save(any());
    }

    @Test
    void asignarGrupoConGrupoInexistenteLanzaNoEncontrado() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        Rol rol = new Rol(9L, "ADMIN_GRUPO", false, Set.of());
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(9L)).thenReturn(Optional.of(rol));
        when(grupoTiendaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarGrupo(1L, 99L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void asignarGrupoConTiendaIndividualDeEseGrupoYaAsignadaLanzaAsignacionMixta() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        Rol rol = new Rol(9L, "ADMIN_GRUPO", false, Set.of());
        Rol rolTienda = new Rol(5L, "CAJERO", false, Set.of());
        GrupoTienda grupo = new GrupoTienda(1L, "PRINCIPAL", "Grupo Principal", EstadoGrupoTienda.ACTIVO);
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(9L)).thenReturn(Optional.of(rol));
        when(grupoTiendaRepository.findById(1L)).thenReturn(Optional.of(grupo));
        when(tiendaRepository.listarIdsPorGrupo(1L)).thenReturn(List.of(10L, 11L));
        when(usuarioTiendaRepository.findByUsuarioId(1L)).thenReturn(
                List.of(new com.ais.marketbackend.seguridad.domain.model.UsuarioTienda(1L, 1L, 10L, rolTienda)));

        assertThatThrownBy(() -> usuarioService.asignarGrupo(1L, 1L, 9L))
                .isInstanceOf(AsignacionMixtaNoPermitidaException.class);
        verify(usuarioGrupoTiendaRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void listarGruposDevuelveLasAsignacionesDelUsuario() {
        Rol rol = new Rol(9L, "ADMIN_GRUPO", false, Set.of());
        UsuarioGrupoTienda asignacion = new UsuarioGrupoTienda(1L, 1L, 1L, rol);
        when(usuarioGrupoTiendaRepository.findByUsuarioId(1L)).thenReturn(List.of(asignacion));

        List<com.ais.marketbackend.seguridad.application.dtos.UsuarioGrupoTiendaResumen> resultado =
                usuarioService.listarGrupos(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).grupoTiendaId()).isEqualTo(1L);
        assertThat(resultado.get(0).rolNombre()).isEqualTo("ADMIN_GRUPO");
    }

    @Test
    void listarConAlcanceGlobalDevuelveTodosLosUsuarios() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        assertThat(usuarioService.listar()).hasSize(1);
    }

    @Test
    void listarConAlcanceLimitadoFiltraPorTiendaOGrupoDelSolicitante() {
        Usuario dentroDeAlcance = new Usuario(
                1L, "ana", "hash", com.ais.marketbackend.seguridad.domain.model.EstadoUsuario.ACTIVO, 0L,
                null, null, null, false);
        Usuario fueraDeAlcance = new Usuario(
                2L, "beto", "hash", com.ais.marketbackend.seguridad.domain.model.EstadoUsuario.ACTIVO, 0L,
                null, null, null, false);
        when(usuarioRepository.findAll()).thenReturn(List.of(dentroDeAlcance, fueraDeAlcance));
        when(autorizacionTiendaService.tiendaIdsPermitidas()).thenReturn(Optional.of(Set.of(10L)));
        when(autorizacionTiendaService.grupoIdsPermitidas()).thenReturn(Optional.of(Set.of()));
        when(usuarioTiendaRepository.listarUsuarioIdsPorTiendas(Set.of(10L))).thenReturn(List.of(1L));

        List<UsuarioResumen> resultado = usuarioService.listar();

        assertThat(resultado).extracting(UsuarioResumen::id).containsExactly(1L);
    }

    @Test
    void asignarTiendaFueraDeAlcanceDelSolicitanteLanzaAccessDenied() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        Rol rol = new Rol(5L, "CAJERO", false, Set.of());
        Tienda tienda = Tienda.nueva("NORTE", "Tienda Norte", null, null, null, 1L);
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(5L)).thenReturn(Optional.of(rol));
        when(tiendaRepository.findById(10L)).thenReturn(Optional.of(tienda));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("fuera de alcance"))
                .when(autorizacionTiendaService).exigirAcceso(10L);

        assertThatThrownBy(() -> usuarioService.asignarTienda(1L, 10L, 5L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(usuarioTiendaRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void asignarTiendaConRolDeAlcanceGlobalYSolicitanteNoGlobalLanzaAccessDenied() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        Rol rolGlobal = new Rol(1L, "ADMIN", true, Set.of());
        Tienda tienda = Tienda.nueva("NORTE", "Tienda Norte", null, null, null, 1L);
        when(usuarioRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolGlobal));
        when(tiendaRepository.findById(10L)).thenReturn(Optional.of(tienda));
        when(autorizacionTiendaService.tiendaIdsPermitidas()).thenReturn(Optional.of(Set.of(10L)));

        assertThatThrownBy(() -> usuarioService.asignarTienda(1L, 10L, 1L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(usuarioTiendaRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void cambiarMiPasswordConLaActualCorrectaLaActualizaYRevocaSesiones() {
        Usuario usuario = Usuario.nuevo("ana", "hash-viejo");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("actual123456", "hash-viejo")).thenReturn(true);
        when(passwordEncoder.encode("nueva1234567")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.cambiarMiPassword(1L, "actual123456", "nueva1234567");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(usuario.isDebeCambiarPassword()).isFalse();
        verify(usuarioRepository).save(usuario);
        verify(refreshTokenRepository).revocarTodosDeUsuario(1L);
    }

    @Test
    void cambiarMiPasswordConLaActualIncorrectaLanzaExcepcionYNoMutaNada() {
        Usuario usuario = Usuario.nuevo("ana", "hash-viejo");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala-actual", "hash-viejo")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.cambiarMiPassword(1L, "mala-actual", "nueva1234567"))
                .isInstanceOf(com.ais.marketbackend.seguridad.domain.exception.PasswordActualInvalidaException.class);
        verify(usuarioRepository, org.mockito.Mockito.never()).save(any());
        verify(refreshTokenRepository, org.mockito.Mockito.never()).revocarTodosDeUsuario(any());
    }

    @Test
    void cambiarMiPasswordConNuevaQueViolaLaPoliticaLanzaExcepcion() {
        Usuario usuario = Usuario.nuevo("ana", "hash-viejo");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("actual123456", "hash-viejo")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarMiPassword(1L, "actual123456", "corta"))
                .isInstanceOf(com.ais.marketbackend.seguridad.domain.exception.PoliticaContrasenaException.class);
        verify(usuarioRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void restablecerPasswordGeneraUnaTemporalYMarcaLaCuenta() {
        Usuario usuario = Usuario.nuevo("ana", "hash-viejo");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-temporal");
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String passwordTemporal = usuarioService.restablecerPassword(1L);

        assertThat(passwordTemporal).hasSize(20);
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-temporal");
        assertThat(usuario.isDebeCambiarPassword()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(refreshTokenRepository).revocarTodosDeUsuario(1L);
    }

    @Test
    void restablecerPasswordConUsuarioInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.restablecerPassword(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void revocarSesionesSubeVersionYRevocaRefreshTokensSinTocarPassword() {
        Usuario usuario = Usuario.nuevo("ana", "hash-viejo");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.revocarSesiones(1L);

        assertThat(usuario.getVersionSeguridad()).isEqualTo(1L);
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-viejo");
        verify(usuarioRepository).save(usuario);
        verify(refreshTokenRepository).revocarTodosDeUsuario(1L);
    }

    @Test
    void revocarSesionesConUsuarioInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.revocarSesiones(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
