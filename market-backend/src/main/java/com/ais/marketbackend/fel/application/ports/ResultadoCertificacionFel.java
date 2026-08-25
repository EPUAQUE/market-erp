package com.ais.marketbackend.fel.application.ports;

import java.time.Instant;

public record ResultadoCertificacionFel(String uuid, Instant fechaCertificacion) {
}
