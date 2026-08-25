package com.ais.marketbackend.seguridad.api.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 100, message = "El usuario no puede superar 100 caracteres")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 256, message = "La contraseña no puede superar 256 caracteres")
        String password) {
}
