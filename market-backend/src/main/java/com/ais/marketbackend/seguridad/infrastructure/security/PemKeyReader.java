package com.ais.marketbackend.seguridad.infrastructure.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/** Lee llaves RSA en PEM (PKCS#8 privada, X.509 pública) desde cualquier ubicación soportada por Spring. */
final class PemKeyReader {

    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

    private PemKeyReader() {
    }

    static PrivateKey readPrivateKey(String location) {
        try {
            byte[] der = stripPem(readAll(location));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo leer la llave privada JWT en " + location, e);
        }
    }

    static PublicKey readPublicKey(String location) {
        try {
            byte[] der = stripPem(readAll(location));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(der));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo leer la llave pública JWT en " + location, e);
        }
    }

    private static String readAll(String location) throws IOException {
        Resource resource = RESOURCE_LOADER.getResource(location);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] stripPem(String pem) {
        String cleaned = pem
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }
}
