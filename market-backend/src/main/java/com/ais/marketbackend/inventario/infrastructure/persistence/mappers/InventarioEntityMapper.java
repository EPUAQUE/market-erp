package com.ais.marketbackend.inventario.infrastructure.persistence.mappers;

import com.ais.marketbackend.inventario.domain.model.Inventario;
import com.ais.marketbackend.inventario.infrastructure.persistence.entities.InventarioEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventarioEntityMapper {

    Inventario toDomain(InventarioEntity entity);

    InventarioEntity toEntity(Inventario domain);
}
