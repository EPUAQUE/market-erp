package com.ais.marketbackend.notificaciones.api.mappers;

import com.ais.marketbackend.notificaciones.api.dtos.responses.NotificacionResponse;
import com.ais.marketbackend.notificaciones.application.dtos.NotificacionResumen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificacionApiMapper {

    NotificacionResponse toResponse(NotificacionResumen resumen);
}
