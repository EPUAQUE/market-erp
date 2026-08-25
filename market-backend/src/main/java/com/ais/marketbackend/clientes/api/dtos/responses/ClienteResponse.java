package com.ais.marketbackend.clientes.api.dtos.responses;

import com.ais.marketbackend.clientes.domain.model.EstadoCliente;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClienteResponse {

    Long id;
    String nit;
    String nombre;
    String direccion;
    String telefono;
    String correo;
    EstadoCliente estado;
    String limiteCredito;
}
