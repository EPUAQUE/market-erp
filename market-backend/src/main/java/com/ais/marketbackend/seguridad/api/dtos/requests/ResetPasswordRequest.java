package com.ais.marketbackend.seguridad.api.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "El token es obligatorio")
        @Size(max = 512, message = "El token no puede superar 512 caracteres")
        String token,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(max = 256, message = "La contraseña nueva no puede superar 256 caracteres")
        String nuevaPassword) {
}
