package com.ais.marketbackend.seguridad.application.dtos;

import com.ais.marketbackend.seguridad.domain.model.EstadoUsuario;

public record UsuarioResumen(Long id, String username, EstadoUsuario estado) {
}
