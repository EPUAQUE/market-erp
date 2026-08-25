package com.ais.marketbackend.fel.api.dtos.requests;

import jakarta.validation.constraints.NotBlank;

public record AnularDocumentoFelRequest(
        @NotBlank(message = "El motivo es obligatorio")
        String motivo) {
}
