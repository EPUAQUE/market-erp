package com.ais.marketbackend.seguridad.domain.service;

/**
 * Puerto de dominio para obtener el usuario autenticado de la solicitud actual, sin
 * acoplar el dominio ni la capa de aplicación a Spring Security. La implementación
 * vive en {@code infrastructure.security}.
 */
public interface ContextoAutenticacion {

    Long usuarioIdActual();
}
