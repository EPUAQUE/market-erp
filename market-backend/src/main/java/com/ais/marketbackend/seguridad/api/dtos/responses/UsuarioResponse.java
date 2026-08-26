package com.ais.marketbackend.seguridad.api.dtos.responses;

import com.ais.marketbackend.seguridad.domain.model.EstadoUsuario;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UsuarioResponse {

    Long id;
    String username;
    EstadoUsuario estado;
    String nombre;
    String telefono;
    String correo;
}
