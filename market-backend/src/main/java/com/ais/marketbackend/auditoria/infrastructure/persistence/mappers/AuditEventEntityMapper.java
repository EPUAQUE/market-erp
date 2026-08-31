package com.ais.marketbackend.auditoria.infrastructure.persistence.mappers;

import com.ais.marketbackend.auditoria.domain.model.AuditEvent;
import com.ais.marketbackend.auditoria.infrastructure.persistence.entities.AuditEventEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEventEntityMapper {

    AuditEvent toDomain(AuditEventEntity entity);

    AuditEventEntity toEntity(AuditEvent domain);
}
