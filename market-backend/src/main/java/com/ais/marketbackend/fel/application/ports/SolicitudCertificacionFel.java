package com.ais.marketbackend.fel.application.ports;

import java.math.BigDecimal;
import java.time.Instant;

public record SolicitudCertificacionFel(
        Long tiendaId, String serie, long numero, Long clienteId, BigDecimal total, Instant fecha) {
}
