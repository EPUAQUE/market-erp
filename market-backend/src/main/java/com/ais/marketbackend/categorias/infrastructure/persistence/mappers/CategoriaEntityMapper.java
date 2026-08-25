package com.ais.marketbackend.categorias.infrastructure.persistence.mappers;

import com.ais.marketbackend.categorias.domain.model.Categoria;
import com.ais.marketbackend.categorias.infrastructure.persistence.entities.CategoriaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoriaEntityMapper {

    Categoria toDomain(CategoriaEntity entity);

    CategoriaEntity toEntity(Categoria domain);
}
