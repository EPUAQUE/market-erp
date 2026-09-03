package com.ais.marketbackend.seguridad.api.dtos.responses;

/**
 * Respuesta siempre idéntica, exista o no el usuario, tenga o no correo cargado, y
 * esté o no activo — ver {@code AuthController.forgotPassword}. Nunca debe variar
 * su forma según el caso real, para no habilitar enumeración de usuarios.
 */
public record ForgotPasswordResponse(String mensaje) {
}
