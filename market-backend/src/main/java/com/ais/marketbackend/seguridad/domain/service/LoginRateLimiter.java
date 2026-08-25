package com.ais.marketbackend.seguridad.domain.service;

import com.ais.marketbackend.seguridad.domain.exception.RateLimitExcedidoException;

/**
 * Límite de intentos de login por IP y por hash del username canónico. Debe lanzar
 * {@link RateLimitExcedidoException} sin revelar si el usuario existe.
 */
public interface LoginRateLimiter {

    void verificarPermitido(String claveIp, String usernameHash) throws RateLimitExcedidoException;
}
