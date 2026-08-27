package com.ais.marketbackend.seguridad.api.dtos.requests;

import jakarta.validation.constraints.NotNull;

public record AsignarGrupoRolRequest(
        @NotNull(message = "El grupo de tiendas es obligatorio") Long grupoTiendaId,
        @NotNull(message = "El rol es obligatorio") Long rolId) {
}
