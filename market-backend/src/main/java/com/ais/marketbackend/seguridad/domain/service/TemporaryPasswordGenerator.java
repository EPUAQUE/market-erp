package com.ais.marketbackend.seguridad.domain.service;

import java.security.SecureRandom;

/**
 * Genera la contraseña temporal del restablecimiento administrativo
 * (ver {@code UsuarioServiceImpl.restablecerPassword}). Charset sin caracteres
 * ambiguos (sin {@code 0/O}, {@code 1/l/I}) porque un admin puede necesitar
 * dictarla o transcribirla — la política de contraseña de esta app es solo de
 * longitud (ver {@link PoliticaContrasenaValidator}), así que no hace falta forzar
 * mayúsculas/minúsculas/símbolos por separado; 20 caracteres de este charset ya dan
 * entropía muy por encima de cualquier password elegido por un humano.
 */
public final class TemporaryPasswordGenerator {

    private static final String CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LONGITUD = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generar() {
        StringBuilder builder = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            builder.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return builder.toString();
    }
}
