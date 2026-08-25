package com.ais.marketbackend.seguridad.application.services.interfaces;

import com.ais.marketbackend.seguridad.application.dtos.LoginResult;

public interface AuthService {

    LoginResult login(String username, String passwordPlano, String claveIp);

    LoginResult refresh(String refreshTokenPlano);

    void logout(String refreshTokenPlano);
}
