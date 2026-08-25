package com.ais.marketbackend.fel.infrastructure.persistence.mappers;

import com.ais.marketbackend.fel.domain.model.DocumentoFel;
import com.ais.marketbackend.fel.infrastructure.persistence.entities.DocumentoFelEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentoFelEntityMapper {

    DocumentoFel toDomain(DocumentoFelEntity entity);

    DocumentoFelEntity toEntity(DocumentoFel domain);
}
