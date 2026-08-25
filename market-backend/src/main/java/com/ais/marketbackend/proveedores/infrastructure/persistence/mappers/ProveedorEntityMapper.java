package com.ais.marketbackend.proveedores.infrastructure.persistence.mappers;

import com.ais.marketbackend.proveedores.domain.model.Proveedor;
import com.ais.marketbackend.proveedores.infrastructure.persistence.entities.ProveedorEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProveedorEntityMapper {

    Proveedor toDomain(ProveedorEntity entity);

    ProveedorEntity toEntity(Proveedor domain);
}
