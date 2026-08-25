package com.ais.marketbackend.productos.api.mappers;

import com.ais.marketbackend.productos.api.dtos.responses.ProductoResponse;
import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductoApiMapper {

    ProductoResponse toResponse(ProductoResumen resumen);
}
