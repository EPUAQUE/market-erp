package com.ais.marketbackend.seguridad.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import com.ais.marketbackend.seguridad.application.services.impl.UsuarioServiceImpl;
import com.ais.marketbackend.seguridad.domain.exception.UsuarioDuplicadoException;
import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.repository.RolRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioTiendaRepository;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import com.ais.marketbackend.seguridad.domain.service.SecurityAuditPublisher;
import com.ais.marketbackend.seguridad.infrastructure.security.SeguridadProperties;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServiceImplTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioTiendaRepository usuarioTiendaRepository;
    private RolRepository rolRepository;
    private PasswordEncoder passwordEncoder;
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioTiendaRepository = mock(UsuarioTiendaRepository.class);
        rolRepository = mock(RolRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        PermisosEfectivosResolver resolver = mock(PermisosEfectivosResolver.class);
        SecurityAuditPublisher auditPublisher = mock(SecurityAuditPublisher.class);

        SeguridadProperties properties = new SeguridadProperties(
                null, null, null,
                new SeguridadProperties.PasswordPolicy(12, 64),
                null, null, null);

        usuarioService = new UsuarioServiceImpl(
                usuarioRepository, usuarioTiendaRepository, rolRepository, passwordEncoder, resolver,
                auditPublisher, properties);
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
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(5L)).thenReturn(Optional.of(rol));

        usuarioService.asignarTienda(1L, 10L, 5L);

        verify(usuarioTiendaRepository).save(any());
    }

    @Test
    void asignarTiendaConRolInexistenteLanzaNoEncontrado() {
        Usuario usuario = Usuario.nuevo("ana", "hash");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarTienda(1L, 10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void asignarTiendaConUsuarioInexistenteLanzaNoEncontrado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.asignarTienda(1L, 10L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
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
}
