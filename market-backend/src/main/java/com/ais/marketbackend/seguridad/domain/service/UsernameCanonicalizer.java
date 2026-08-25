package com.ais.marketbackend.seguridad.domain.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Única función de canonicalización de username, aplicada igual al alta y al
 * login: normaliza Unicode (NFKC), recorta espacios y pasa a minúsculas. Cambiarla
 * afecta logins existentes — no editar sin migrar los `username` ya almacenados.
 */
public final class UsernameCanonicalizer {

    private UsernameCanonicalizer() {
    }

    public static String canonicalizar(String username) {
        if (username == null) {
            return null;
        }
        String normalizado = Normalizer.normalize(username.trim(), Normalizer.Form.NFKC);
        return normalizado.toLowerCase(Locale.ROOT);
    }
}
