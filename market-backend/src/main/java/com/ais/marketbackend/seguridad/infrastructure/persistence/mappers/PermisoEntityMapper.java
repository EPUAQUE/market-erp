package com.ais.marketbackend.seguridad.infrastructure.persistence.mappers;

import com.ais.marketbackend.seguridad.domain.model.Permiso;
import com.ais.marketbackend.seguridad.infrastructure.persistence.entities.PermisoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermisoEntityMapper {

    Permiso toDomain(PermisoEntity entity);

    PermisoEntity toEntity(Permiso domain);
}
