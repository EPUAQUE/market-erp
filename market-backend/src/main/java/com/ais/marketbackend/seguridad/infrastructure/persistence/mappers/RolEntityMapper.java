package com.ais.marketbackend.seguridad.infrastructure.persistence.mappers;

import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.RolEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = PermisoEntityMapper.class)
public interface RolEntityMapper {

    Rol toDomain(RolEntity entity);

    RolEntity toEntity(Rol domain);
}
