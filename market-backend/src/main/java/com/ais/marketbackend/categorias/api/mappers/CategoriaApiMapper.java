package com.ais.marketbackend.categorias.api.mappers;

import com.ais.marketbackend.categorias.api.dtos.responses.CategoriaResponse;
import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoriaApiMapper {

    CategoriaResponse toResponse(CategoriaResumen resumen);
}
