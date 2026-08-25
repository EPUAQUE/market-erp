package com.ais.marketbackend.marcas.api.mappers;

import com.ais.marketbackend.marcas.api.dtos.responses.MarcaResponse;
import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MarcaApiMapper {

    MarcaResponse toResponse(MarcaResumen resumen);
}
