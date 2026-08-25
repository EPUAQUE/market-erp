package com.ais.marketbackend.tiendas.infrastructure.persistence.mappers;

import com.ais.marketbackend.tiendas.domain.model.Tienda;
import com.ais.marketbackend.tiendas.infrastructure.persistence.entities.TiendaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TiendaEntityMapper {

    Tienda toDomain(TiendaEntity entity);

    TiendaEntity toEntity(Tienda domain);
}
