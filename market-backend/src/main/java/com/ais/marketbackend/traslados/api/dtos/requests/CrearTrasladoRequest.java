package com.ais.marketbackend.traslados.api.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CrearTrasladoRequest(
        @NotNull(message = "La tienda de origen es obligatoria") Long tiendaOrigenId,

        @NotNull(message = "La tienda de destino es obligatoria") Long tiendaDestinoId,

        @NotEmpty(message = "El traslado debe tener al menos una línea")
        List<@Valid LineaTrasladoRequest> lineas) {
}
