package com.ais.marketbackend.seguridad.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jose.jwk.JWKSet;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Emisión y validación de access tokens JWT (RS256). El algoritmo y las llaves se
 * fijan aquí; nunca se confía en el header {@code alg} del token entrante — ver
 * seguridad-desarrolladores.md §5.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JWKSet jwkSet(SeguridadProperties properties) {
        List<JWK> rsaKeys = properties.jwt().keys().stream()
                .<JWK>map(key -> {
                    PublicKey publicKey = PemKeyReader.readPublicKey(key.publicKeyLocation());
                    PrivateKey privateKey = PemKeyReader.readPrivateKey(key.privateKeyLocation());
                    return new RSAKey.Builder((RSAPublicKey) publicKey)
                            .privateKey((RSAPrivateKey) privateKey)
                            .keyID(key.kid())
                            .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                            .build();
                })
                .toList();
        return new JWKSet(rsaKeys);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JWKSet jwkSet) {
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public NimbusJwtDecoder jwtDecoder(
            JWKSource<SecurityContext> jwkSource, SeguridadProperties properties,
            SecurityVersionValidator securityVersionValidator) {
        JWSVerificationKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(keySelector);

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(jwtProcessor);
        decoder.setJwtValidator(defaultValidators(properties, securityVersionValidator));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> defaultValidators(
            SeguridadProperties properties, SecurityVersionValidator securityVersionValidator) {
        OAuth2TokenValidator<Jwt> timestamp = new JwtTimestampValidator(properties.jwt().clockSkew());
        OAuth2TokenValidator<Jwt> issuer = new JwtIssuerValidator(properties.jwt().issuer());
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                "aud", audiences -> audiences != null && audiences.contains(properties.jwt().audience()));
        return new DelegatingOAuth2TokenValidator<>(timestamp, issuer, audience, securityVersionValidator);
    }
}
