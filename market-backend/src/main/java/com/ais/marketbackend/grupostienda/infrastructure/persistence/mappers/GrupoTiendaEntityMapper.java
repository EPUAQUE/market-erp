package com.ais.marketbackend.grupostienda.infrastructure.persistence.mappers;

import com.ais.marketbackend.grupostienda.domain.model.GrupoTienda;
import com.ais.marketbackend.grupostienda.infrastructure.persistence.entities.GrupoTiendaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GrupoTiendaEntityMapper {

    GrupoTienda toDomain(GrupoTiendaEntity entity);

    GrupoTiendaEntity toEntity(GrupoTienda domain);
}
