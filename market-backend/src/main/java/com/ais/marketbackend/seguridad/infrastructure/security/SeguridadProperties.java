package com.ais.marketbackend.seguridad.infrastructure.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code @Validated}: fuerza a Spring a rechazar el arranque si algún valor de estas
 * propiedades queda vacío tras resolver placeholders — complementa (no reemplaza) a
 * los {@code ${VAR}} sin default de {@code application-prod.yml}, que solo cubren el
 * caso "variable no definida en absoluto"; esto además cubre "definida pero vacía".
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SeguridadProperties(
        @NotNull @Valid Jwt jwt,
        RefreshToken refreshToken,
        Argon2 argon2,
        PasswordPolicy passwordPolicy,
        RateLimit rateLimit,
        Network network,
        @NotNull @Valid Cors cors) {

    public record Jwt(@NotBlank String issuer, @NotBlank String audience, Duration accessTokenTtl,
                       Duration clockSkew, @NotBlank String activeKid, @NotEmpty List<@Valid Key> keys) {
    }

    public record Key(
            @NotBlank String kid, @NotBlank String privateKeyLocation, @NotBlank String publicKeyLocation) {
    }

    public record RefreshToken(Duration ttl) {
    }

    public record Argon2(int saltLength, int hashLength, int parallelism, int memoryKib, int iterations) {
    }

    public record PasswordPolicy(int minLength, int maxLength) {
    }

    public record RateLimit(Login login) {
    }

    public record Login(int capacity, int refillTokens, Duration refillPeriod) {
    }

    public record Network(List<String> trustedProxies) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins, List<String> allowedMethods, List<String> allowedHeaders) {
    }
}
