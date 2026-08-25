package com.ais.marketbackend.tiendas.api.mappers;

import com.ais.marketbackend.tiendas.api.dtos.responses.TiendaResponse;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TiendaApiMapper {

    TiendaResponse toResponse(TiendaResumen resumen);
}
