package com.ais.marketbackend.seguridad.api.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        @Size(max = 256, message = "La contraseña actual no puede superar 256 caracteres")
        String passwordActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(max = 256, message = "La contraseña nueva no puede superar 256 caracteres")
        String passwordNueva) {
}
