package com.ais.marketbackend.cuentasporpagar.application.services.interfaces;

import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import java.math.BigDecimal;
import java.util.List;

public interface CuentaPorPagarService {

    /** Usado por otros módulos (Compras) al recibir una compra — no se expone como creación manual vía API. */
    CuentaPorPagarResumen crear(Long compraId, Long proveedorId, Long tiendaId, BigDecimal montoOriginal);

    CuentaPorPagarResumen registrarPago(Long tiendaId, Long id, BigDecimal monto);

    CuentaPorPagarResumen anular(Long tiendaId, Long id);

    CuentaPorPagarResumen obtener(Long tiendaId, Long id);

    List<CuentaPorPagarResumen> listarPorTienda(Long tiendaId);
}
