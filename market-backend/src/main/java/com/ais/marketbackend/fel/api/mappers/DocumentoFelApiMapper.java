package com.ais.marketbackend.fel.api.mappers;

import com.ais.marketbackend.fel.api.dtos.responses.DocumentoFelResponse;
import com.ais.marketbackend.fel.application.dtos.DocumentoFelResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentoFelApiMapper {

    DocumentoFelResponse toResponse(DocumentoFelResumen resumen);
}
