package com.ais.marketbackend.clientes.api.mappers;

import com.ais.marketbackend.clientes.api.dtos.responses.ClienteResponse;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** {@code limiteCredito} necesita formatear BigDecimal a String con toPlainString() — el resto lo mapea MapStruct. */
@Mapper(componentModel = "spring")
public interface ClienteApiMapper {

    @Mapping(target = "limiteCredito", expression = "java(toPlainString(resumen.limiteCredito()))")
    ClienteResponse toResponse(ClienteResumen resumen);

    default String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
