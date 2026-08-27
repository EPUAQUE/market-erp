package com.ais.marketbackend.grupostienda.api.mappers;

import com.ais.marketbackend.grupostienda.api.dtos.responses.GrupoTiendaResponse;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GrupoTiendaApiMapper {

    GrupoTiendaResponse toResponse(GrupoTiendaResumen resumen);
}
