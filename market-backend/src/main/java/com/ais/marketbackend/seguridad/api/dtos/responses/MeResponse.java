package com.ais.marketbackend.seguridad.api.dtos.responses;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MeResponse {

    String username;
    Set<String> permisos;
    Set<Long> tiendaIds;
    boolean alcanceGlobal;
}
