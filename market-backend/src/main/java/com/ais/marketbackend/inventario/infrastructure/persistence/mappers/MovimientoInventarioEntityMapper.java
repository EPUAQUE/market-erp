package com.ais.marketbackend.inventario.infrastructure.persistence.mappers;

import com.ais.marketbackend.inventario.domain.model.MovimientoInventario;
import com.ais.marketbackend.inventario.infrastructure.persistence.entities.MovimientoInventarioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovimientoInventarioEntityMapper {

    @Mapping(target = "tipo", source = "tipoMovimiento")
    MovimientoInventario toDomain(MovimientoInventarioEntity entity);

    @Mapping(target = "tipoMovimiento", source = "tipo")
    MovimientoInventarioEntity toEntity(MovimientoInventario domain);
}
