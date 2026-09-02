package com.ais.marketbackend.clientes.api.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CrearClienteRequest(
        @Size(max = 20, message = "El NIT no puede superar 20 caracteres")
        String nit,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String telefono,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        /** Opcional — {@code null} significa que todavía no se le define un límite de crédito. */
        @DecimalMin(value = "0", message = "El límite de crédito no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El límite de crédito no puede tener más de 2 decimales")
        BigDecimal limiteCredito,

        /** Opcional — clave de idempotencia para reintentos seguros (altas offline sin NIT). */
        @Size(max = 100, message = "El correlationId no puede superar 100 caracteres")
        String correlationId) {
}
