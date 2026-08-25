package com.ais.marketbackend.marcas.infrastructure.persistence.mappers;

import com.ais.marketbackend.marcas.domain.model.Marca;
import com.ais.marketbackend.marcas.infrastructure.persistence.entities.MarcaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MarcaEntityMapper {

    Marca toDomain(MarcaEntity entity);

    MarcaEntity toEntity(Marca domain);
}
