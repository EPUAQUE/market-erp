package com.ais.marketbackend.seguridad.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generación y hash del refresh token opaco. Usa solo JDK estándar (no es un
 * detalle de framework): SecureRandom para generar 256 bits de entropía, SHA-256
 * para el hash almacenado en servidor. SHA-256 simple es suficiente aquí —a
 * diferencia de una contraseña, el valor ya tiene alta entropía y no es adivinable
 * por fuerza bruta a partir del hash; usar Argon2id aquí solo añadiría costo sin
 * beneficio de seguridad real.
 */
public final class RefreshTokenCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private RefreshTokenCrypto() {
    }

    public static String generarOpaco() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    public static String hash(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en este JDK", e);
        }
    }
}
