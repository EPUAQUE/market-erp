package com.ais.marketbackend.seguridad.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Única excepción para toda falla de autenticación (usuario inexistente, contraseña
 * incorrecta, cuenta inactiva/bloqueada, refresh token inválido/expirado/reutilizado).
 * Deliberadamente genérica: nunca debe crearse una subclase con mensaje distinto por
 * causa, para no filtrar por respuesta ni por tiempo qué falló exactamente.
 */
public class AutenticacionFallidaException extends BusinessException {

    public AutenticacionFallidaException() {
        super("Credenciales inválidas o sesión no válida.");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    @Override
    public String errorCode() {
        return "AUTHENTICATION_FAILED";
    }
}
