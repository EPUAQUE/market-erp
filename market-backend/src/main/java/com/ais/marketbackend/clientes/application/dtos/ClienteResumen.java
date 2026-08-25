package com.ais.marketbackend.clientes.application.dtos;

import com.ais.marketbackend.clientes.domain.model.EstadoCliente;
import java.math.BigDecimal;

public record ClienteResumen(
        Long id, String nit, String nombre, String direccion, String telefono, String correo,
        EstadoCliente estado, BigDecimal limiteCredito) {
}
