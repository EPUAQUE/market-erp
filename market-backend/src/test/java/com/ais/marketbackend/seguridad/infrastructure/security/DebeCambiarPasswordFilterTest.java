package com.ais.marketbackend.seguridad.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.ObjectMapper;

class DebeCambiarPasswordFilterTest {

    private final DebeCambiarPasswordFilter filter = new DebeCambiarPasswordFilter(new ObjectMapper());

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bloqueaUnaRutaCualquieraCuandoDebeCambiarPasswordEsVerdadero() throws Exception {
        autenticarComo("ana", true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ventas");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("DEBE_CAMBIAR_PASSWORD");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void permiteCambiarLaPasswordAunqueDebeCambiarPasswordEsteActivo() throws Exception {
        autenticarComo("ana", true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void noBloqueaCuandoDebeCambiarPasswordEsFalso() throws Exception {
        autenticarComo("ana", false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ventas");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private void autenticarComo(String username, boolean debeCambiarPassword) {
        Jwt jwt = new Jwt(
                "jwt-value", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "RS256"), Map.of("sub", username, "debeCambiarPassword", debeCambiarPassword));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(jwt, null));
    }
}
