package com.ais.marketbackend.seguridad.api.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 100, message = "El usuario no puede superar 100 caracteres")
        String username) {
}
