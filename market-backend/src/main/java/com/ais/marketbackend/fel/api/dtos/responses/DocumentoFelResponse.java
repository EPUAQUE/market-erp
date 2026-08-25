package com.ais.marketbackend.fel.api.dtos.responses;

import com.ais.marketbackend.fel.domain.model.EstadoDocumentoFel;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentoFelResponse {

    Long id;
    Long ventaId;
    Long tiendaId;
    String serie;
    long numero;
    String uuid;
    EstadoDocumentoFel estado;
    Instant fechaEmision;
    Instant fechaCertificacion;
    String motivoAnulacion;
    String mensajeError;
}
