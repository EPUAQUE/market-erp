package com.ais.marketbackend.seguridad.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

/**
 * {@code passwordTemporal} solo existe en esta respuesta — nunca se persiste en
 * texto plano ni se registra en auditoría (ver
 * {@code UsuarioServiceImpl.restablecerPassword}). Quien llama es responsable de
 * entregarla al usuario por un canal separado y no loguearla.
 */
@Value
@Builder
public class RestablecerPasswordResponse {

    String passwordTemporal;
}
