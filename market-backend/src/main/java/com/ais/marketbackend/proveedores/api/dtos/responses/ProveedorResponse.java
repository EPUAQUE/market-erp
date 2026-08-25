package com.ais.marketbackend.proveedores.api.dtos.responses;

import com.ais.marketbackend.proveedores.domain.model.EstadoProveedor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProveedorResponse {

    Long id;
    String nit;
    String nombre;
    String direccion;
    String telefono;
    String correo;
    EstadoProveedor estado;
}
