package com.ais.marketbackend.seguridad.application.dtos;

public record LoginResult(String accessToken, String refreshToken, long expiresInSeconds) {
}
