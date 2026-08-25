package com.ais.marketbackend.clientes.infrastructure.persistence.mappers;

import com.ais.marketbackend.clientes.domain.model.Cliente;
import com.ais.marketbackend.clientes.infrastructure.persistence.entities.ClienteEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClienteEntityMapper {

    Cliente toDomain(ClienteEntity entity);

    ClienteEntity toEntity(Cliente domain);
}
