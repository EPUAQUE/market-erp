package com.ais.marketbackend.seguridad.api.mappers;

import com.ais.marketbackend.seguridad.api.dtos.responses.UsuarioResponse;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioApiMapper {

    UsuarioResponse toResponse(UsuarioResumen resumen);
}
