package com.ais.marketbackend.seguridad.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Fase 4 (PLAN_MEJORAS.md): "probar rotación de llaves JWT manteniendo temporalmente
 * validación de la anterior". El mecanismo ya existía en {@link JwtConfig} — un
 * {@code JWKSet} con varias llaves (una activa para firmar nuevos tokens vía
 * {@code activeKid}, todas presentes para validar) — solo faltaba una prueba real de
 * que efectivamente funciona. No usa Spring context ni PEM en disco: construye las
 * llaves RSA en memoria y ejercita directamente {@code NimbusJwtEncoder}/
 * {@code NimbusJwtDecoder}, exactamente el mismo mecanismo que arma {@link JwtConfig}.
 */
class JwtRotacionTest {

    private static final String KID_A = "kid-a-2026";
    private static final String KID_B = "kid-b-2026";

    private static RSAKey rsaKeyA;
    private static RSAKey rsaKeyB;

    @BeforeAll
    static void generarLlaves() throws Exception {
        rsaKeyA = generarRsaKey(KID_A);
        rsaKeyB = generarRsaKey(KID_B);
    }

    /**
     * Durante la rotación: se agrega la llave B (nueva, activa para firmar) sin
     * quitar la llave A todavía — un token firmado con la A (emitido antes de
     * rotar) debe seguir validando, y uno firmado con la B (ya rotado) también.
     */
    @Test
    void duranteLaRotacionAmbasLlavesValidan() {
        JWKSource<SecurityContext> jwkSourceCompleto = new ImmutableJWKSet<>(new JWKSet(List.of(rsaKeyA, rsaKeyB)));
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSourceCompleto);

        String tokenFirmadoConA = emitirToken(encoder, KID_A);
        String tokenFirmadoConB = emitirToken(encoder, KID_B);

        NimbusJwtDecoder decoderDuranteRotacion = construirDecoder(jwkSourceCompleto);

        Jwt jwtA = decoderDuranteRotacion.decode(tokenFirmadoConA);
        Jwt jwtB = decoderDuranteRotacion.decode(tokenFirmadoConB);

        assertThat(jwtA.getHeaders().get("kid")).isEqualTo(KID_A);
        assertThat(jwtB.getHeaders().get("kid")).isEqualTo(KID_B);
    }

    /**
     * Rotación completa: se retira la llave A de la configuración (solo queda la
     * B). Un token viejo firmado con A ahora debe RECHAZARSE — es exactamente el
     * punto en el que una sesión emitida antes de la rotación deja de ser válida,
     * a propósito, una vez que el operador decide que ya pasó suficiente tiempo.
     */
    @Test
    void alRetirarLaLlaveViejaSusTokensDejanDeValidar() {
        JWKSource<SecurityContext> jwkSourceCompleto = new ImmutableJWKSet<>(new JWKSet(List.of(rsaKeyA, rsaKeyB)));
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSourceCompleto);
        String tokenFirmadoConA = emitirToken(encoder, KID_A);
        String tokenFirmadoConB = emitirToken(encoder, KID_B);

        JWKSource<SecurityContext> jwkSourceSoloB = new ImmutableJWKSet<>(new JWKSet(List.of(rsaKeyB)));
        NimbusJwtDecoder decoderTrasRetirarA = construirDecoder(jwkSourceSoloB);

        assertThatThrownBy(() -> decoderTrasRetirarA.decode(tokenFirmadoConA))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);

        Jwt jwtB = decoderTrasRetirarA.decode(tokenFirmadoConB);
        assertThat(jwtB.getHeaders().get("kid")).isEqualTo(KID_B);
    }

    private static String emitirToken(NimbusJwtEncoder encoder, String kid) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(kid).build();
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("market-backend-test")
                .audience(List.of("market-clients-test"))
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(600))
                .subject("usuario-de-prueba")
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Mismo armado que {@link JwtConfig#jwtDecoder}, sin los validadores de negocio (issuer/audience/sver) — acá solo interesa la selección de llave por kid. */
    private static NimbusJwtDecoder construirDecoder(JWKSource<SecurityContext> jwkSource) {
        JWSVerificationKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(com.nimbusds.jose.JWSAlgorithm.RS256, jwkSource);
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(keySelector);
        return new NimbusJwtDecoder(jwtProcessor);
    }

    private static RSAKey generarRsaKey(String kid) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(kid)
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .build();
    }
}
