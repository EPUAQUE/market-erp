package com.ais.marketbackend.auditoria.api.mappers;

import com.ais.marketbackend.auditoria.api.dtos.responses.AuditEventResponse;
import com.ais.marketbackend.auditoria.application.dtos.AuditEventResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEventApiMapper {

    AuditEventResponse toResponse(AuditEventResumen resumen);
}
