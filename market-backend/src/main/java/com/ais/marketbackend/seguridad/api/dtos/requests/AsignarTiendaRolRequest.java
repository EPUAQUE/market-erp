package com.ais.marketbackend.seguridad.api.dtos.requests;

import jakarta.validation.constraints.NotNull;

public record AsignarTiendaRolRequest(
        @NotNull(message = "La tienda es obligatoria") Long tiendaId,
        @NotNull(message = "El rol es obligatorio") Long rolId) {
}
