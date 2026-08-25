package com.ais.marketbackend.productos.infrastructure.persistence.mappers;

import com.ais.marketbackend.productos.domain.model.ProductoTienda;
import com.ais.marketbackend.productos.infrastructure.persistence.entities.ProductoTiendaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductoTiendaEntityMapper {

    ProductoTienda toDomain(ProductoTiendaEntity entity);

    ProductoTiendaEntity toEntity(ProductoTienda domain);
}
