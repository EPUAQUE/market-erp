package com.ais.marketbackend.productos.infrastructure.persistence.mappers;

import com.ais.marketbackend.productos.domain.model.Producto;
import com.ais.marketbackend.productos.infrastructure.persistence.entities.ProductoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductoEntityMapper {

    Producto toDomain(ProductoEntity entity);

    ProductoEntity toEntity(Producto domain);
}
