package com.ais.marketbackend.productos.api.dtos.responses;

import java.util.List;

public record ImportacionProductosResponse(
        int totalFilas, int creados, int omitidos, List<ImportacionFilaErrorResponse> errores) {
}
