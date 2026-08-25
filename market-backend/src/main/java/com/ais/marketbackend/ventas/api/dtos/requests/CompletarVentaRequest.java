package com.ais.marketbackend.ventas.api.dtos.requests;

import jakarta.validation.Valid;
import java.util.List;

/**
 * {@code pagos} es opcional — solo lo manda el cliente para una venta {@code MIXTO}
 * (ver {@code VentaService.completar}); para cualquier otro método de pago el
 * servidor resuelve el desglose por sí mismo y este campo, si viene, se ignora.
 */
public record CompletarVentaRequest(List<@Valid PagoInmediatoRequest> pagos) {
}
