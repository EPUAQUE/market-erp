package com.ais.marketbackend.seguridad.api.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ais.marketbackend.seguridad.api.dtos.responses.MeResponse;
import com.ais.marketbackend.seguridad.application.dtos.LoginResult;
import com.ais.marketbackend.seguridad.application.services.interfaces.AuthService;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.domain.exception.AutenticacionFallidaException;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.infrastructure.security.SeguridadProperties;
import com.ais.marketbackend.shared.exceptions.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    private AuthService authService;
    private UsuarioService usuarioService;
    private AuthController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        usuarioService = mock(UsuarioService.class);
        SeguridadProperties properties = new SeguridadProperties(
                null,
                new SeguridadProperties.RefreshToken(Duration.ofDays(30)),
                null, null, null, null, null);

        controller = new AuthController(authService, usuarioService, properties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new SimpleMeterRegistry()))
                .build();
    }

    @Test
    void loginExitosoDevuelveAccessTokenYCookieDeRefresh() throws Exception {
        when(authService.login(anyString(), anyString(), anyString()))
                .thenReturn(new LoginResult("jwt-access", "refresh-plano", 600));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"clave-larga-segura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void loginConCredencialesInvalidasDevuelve401Generico() throws Exception {
        when(authService.login(anyString(), anyString(), anyString()))
                .thenThrow(new AutenticacionFallidaException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"clave-incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void loginConCuerpoInvalidoDevuelve400DeValidacion() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void refreshSinCookieDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshConCookieValidaRotaElToken() throws Exception {
        when(authService.refresh(anyString()))
                .thenReturn(new LoginResult("jwt-nuevo", "refresh-nuevo", 600));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-viejo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-nuevo"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void logoutSinCookieNoFalla() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk());
    }

    @Test
    void meDevuelvePermisosDelUsuarioAutenticado() {
        when(usuarioService.obtenerPermisosEfectivosPorUsername("ana"))
                .thenReturn(new PermisosEfectivos(1L, "ana", Set.of("VENTAS_VER"), Set.of(1L), false));
        Jwt jwt = new Jwt(
                "jwt-value", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "RS256"), Map.of("sub", "ana"));

        MeResponse response = controller.me(jwt).getBody();

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("ana");
        assertThat(response.getPermisos()).containsExactly("VENTAS_VER");
        assertThat(response.getTiendaIds()).containsExactly(1L);
        assertThat(response.isAlcanceGlobal()).isFalse();
    }
}
