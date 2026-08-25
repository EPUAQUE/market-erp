package com.ais.marketbackend.seguridad.api.controllers;

import com.ais.marketbackend.seguridad.api.dtos.requests.LoginRequest;
import com.ais.marketbackend.seguridad.api.dtos.responses.LoginResponse;
import com.ais.marketbackend.seguridad.api.dtos.responses.MeResponse;
import com.ais.marketbackend.seguridad.application.dtos.LoginResult;
import com.ais.marketbackend.seguridad.application.services.interfaces.AuthService;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.domain.exception.AutenticacionFallidaException;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.infrastructure.security.SeguridadProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El refresh token viaja en una cookie {@code HttpOnly, Secure, SameSite=Strict}
 * (nunca en el cuerpo JSON ni accesible por JavaScript). {@code SameSite=Strict}
 * es aquí la defensa CSRF primaria para /refresh y /logout — no se implementa un
 * token CSRF de doble envío en este alcance; documentado como punto de extensión
 * si el frontend llegara a requerir SameSite=Lax/None.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_REFRESH = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final SeguridadProperties properties;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        LoginResult resultado = authService.login(request.username(), request.password(), clienteIp(http));
        return conCookieDeRefresh(resultado);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest http) {
        String refreshToken = leerCookie(http).orElseThrow(AutenticacionFallidaException::new);
        LoginResult resultado = authService.refresh(refreshToken);
        return conCookieDeRefresh(resultado);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        leerCookie(http).ifPresent(authService::logout);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, clearedCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        PermisosEfectivos permisos = usuarioService.obtenerPermisosEfectivosPorUsername(jwt.getSubject());
        return ResponseEntity.ok(MeResponse.builder()
                .username(permisos.username())
                .permisos(permisos.permisos())
                .tiendaIds(permisos.tiendaIds())
                .alcanceGlobal(permisos.alcanceGlobal())
                .build());
    }

    private ResponseEntity<LoginResponse> conCookieDeRefresh(LoginResult resultado) {
        LoginResponse body = LoginResponse.builder()
                .accessToken(resultado.accessToken())
                .tokenType("Bearer")
                .expiresIn(resultado.expiresInSeconds())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, refreshCookie(resultado.refreshToken()).toString())
                .body(body);
    }

    private Optional<String> leerCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_REFRESH.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(COOKIE_REFRESH, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(properties.refreshToken().ttl())
                .build();
    }

    private ResponseCookie clearedCookie() {
        return ResponseCookie.from(COOKIE_REFRESH, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    private String clienteIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
