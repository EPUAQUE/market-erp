package com.ais.marketbackend.unidadesmedida.api.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearUnidadMedidaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre,

        @NotBlank(message = "La abreviación es obligatoria")
        @Size(max = 10, message = "La abreviación no puede superar 10 caracteres")
        String abreviacion) {
}
