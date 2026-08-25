package com.ais.marketbackend.seguridad.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    /**
     * Parámetros mínimos de referencia OWASP vigente (m=19456 KiB, t=2, p=1).
     * Requiere prueba de rendimiento en hardware de producción antes de subir el
     * costo — ver seguridad-desarrolladores.md §4.
     */
    @Bean
    public PasswordEncoder passwordEncoder(SeguridadProperties properties) {
        SeguridadProperties.Argon2 argon2 = properties.argon2();
        return new Argon2PasswordEncoder(
                argon2.saltLength(), argon2.hashLength(), argon2.parallelism(), argon2.memoryKib(), argon2.iterations());
    }
}
