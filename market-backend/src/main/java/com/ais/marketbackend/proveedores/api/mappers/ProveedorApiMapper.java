package com.ais.marketbackend.proveedores.api.mappers;

import com.ais.marketbackend.proveedores.api.dtos.responses.ProveedorResponse;
import com.ais.marketbackend.proveedores.application.dtos.ProveedorResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProveedorApiMapper {

    ProveedorResponse toResponse(ProveedorResumen resumen);
}
