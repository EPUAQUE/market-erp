package com.ais.marketbackend.seguridad.domain.service;

/**
 * Puerto de envío del correo de "olvidé mi contraseña" — mismo patrón que
 * {@link AccessTokenIssuer}/{@link SecurityAuditPublisher}: el dominio/aplicación
 * solo conoce esta interfaz, la implementación (JavaMailSender + URL del frontend)
 * vive en infraestructura. No debe propagar excepciones: un fallo de envío no debe
 * romper el flujo que lo disparó ni, sobre todo, permitir distinguir por código de
 * error si el correo salió o no (ver AuthController.forgotPassword, respuesta
 * siempre genérica).
 */
public interface PasswordResetMailSender {

    void enviar(String correoDestino, String tokenPlano);
}
