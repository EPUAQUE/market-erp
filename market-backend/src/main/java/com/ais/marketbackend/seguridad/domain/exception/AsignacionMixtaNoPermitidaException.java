package com.ais.marketbackend.seguridad.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Un usuario no puede tener a la vez una asignación de grupo completo
 * ({@code usuario_grupo_tienda}) y una asignación de tienda individual
 * ({@code usuario_tienda}) para una tienda de ese mismo grupo — ambigüedad de rol
 * evitada por diseño, no solo por convención.
 */
public class AsignacionMixtaNoPermitidaException extends BusinessException {

    public AsignacionMixtaNoPermitidaException(String mensaje) {
        super(mensaje);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "ASIGNACION_MIXTA_NO_PERMITIDA";
    }
}
