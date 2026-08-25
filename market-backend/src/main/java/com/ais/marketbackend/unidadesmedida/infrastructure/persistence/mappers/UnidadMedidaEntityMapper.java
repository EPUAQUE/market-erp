package com.ais.marketbackend.unidadesmedida.infrastructure.persistence.mappers;

import com.ais.marketbackend.unidadesmedida.domain.model.UnidadMedida;
import com.ais.marketbackend.unidadesmedida.infrastructure.persistence.entities.UnidadMedidaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UnidadMedidaEntityMapper {

    UnidadMedida toDomain(UnidadMedidaEntity entity);

    UnidadMedidaEntity toEntity(UnidadMedida domain);
}
