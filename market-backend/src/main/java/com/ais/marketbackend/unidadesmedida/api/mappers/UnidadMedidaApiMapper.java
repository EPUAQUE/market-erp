package com.ais.marketbackend.unidadesmedida.api.mappers;

import com.ais.marketbackend.unidadesmedida.api.dtos.responses.UnidadMedidaResponse;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UnidadMedidaApiMapper {

    UnidadMedidaResponse toResponse(UnidadMedidaResumen resumen);
}
