package com.ais.marketbackend.tiendas.api.dtos.responses;

import com.ais.marketbackend.tiendas.domain.model.EstadoTienda;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TiendaResponse {

    Long id;
    String codigo;
    String nombre;
    String direccion;
    String telefono;
    String correo;
    EstadoTienda estado;
    Long grupoId;
}
