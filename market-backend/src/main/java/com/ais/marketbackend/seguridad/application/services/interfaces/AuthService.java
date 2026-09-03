package com.ais.marketbackend.seguridad.application.services.interfaces;

import com.ais.marketbackend.seguridad.application.dtos.LoginResult;

public interface AuthService {

    LoginResult login(String username, String passwordPlano, String claveIp);

    LoginResult refresh(String refreshTokenPlano);

    void logout(String refreshTokenPlano);

    /**
     * Flujo público "olvidé mi contraseña". Nunca lanza excepción por usuario
     * inexistente/sin correo/inactivo — ver Javadoc de la implementación — para que
     * {@code AuthController.forgotPassword} siempre responda igual.
     */
    void solicitarRestablecimiento(String username, String claveIp);

    /**
     * Canjea el token emitido por {@link #solicitarRestablecimiento} y aplica la
     * nueva contraseña. Lanza {@code TokenResetInvalidoException} genérica si el
     * token no existe, ya fue usado o expiró.
     */
    void restablecerPassword(String tokenPlano, String passwordNueva);
}
