package com.ais.marketbackend.notificaciones.infrastructure.persistence.mappers;

import com.ais.marketbackend.notificaciones.domain.model.Notificacion;
import com.ais.marketbackend.notificaciones.infrastructure.persistence.entities.NotificacionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificacionEntityMapper {

    Notificacion toDomain(NotificacionEntity entity);

    NotificacionEntity toEntity(Notificacion domain);
}
