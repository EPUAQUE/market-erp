package com.ais.marketbackend.fel.application.dtos;

import com.ais.marketbackend.fel.domain.model.EstadoDocumentoFel;
import java.time.Instant;

public record DocumentoFelResumen(
        Long id, Long ventaId, Long tiendaId, String serie, long numero, String uuid, EstadoDocumentoFel estado,
        Instant fechaEmision, Instant fechaCertificacion, String motivoAnulacion, String mensajeError) {
}
