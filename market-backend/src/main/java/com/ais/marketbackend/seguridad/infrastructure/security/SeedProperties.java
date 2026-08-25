package com.ais.marketbackend.seguridad.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(boolean enabled, String adminUsername, String adminPassword) {
}
