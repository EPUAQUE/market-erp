package com.ais.marketbackend.caja.application.services.interfaces;

import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.shared.domain.Pagina;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CajaService {

    CajaSesionResumen abrir(Long tiendaId, BigDecimal montoInicial);

    CajaSesionResumen registrarMovimiento(Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto);

    CajaSesionResumen cerrar(Long tiendaId, BigDecimal montoFinalContado);

    CajaSesionResumen obtenerAbierta(Long tiendaId);

    CajaSesionResumen obtener(Long tiendaId, Long id);

    /** Sin paginar — uso interno (ej. agregados del dashboard). El endpoint público usa la variante paginada. */
    List<CajaSesionResumen> listarPorTienda(Long tiendaId);

    Pagina<CajaSesionResumen> listarPorTienda(Long tiendaId, int pagina, int tamano);

    /**
     * Usado por otros módulos (Cuentas por Cobrar/Pagar) para reflejar un cobro o
     * pago en efectivo — no falla si la tienda no tiene una caja abierta, ese
     * caso simplemente no registra movimiento (la tienda podría no operar caja
     * diaria todavía).
     */
    Optional<CajaSesionResumen> registrarMovimientoSiHayAbierta(
            Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto);

    /**
     * Usado por Ventas para bloquear el completar() de una venta que mueve
     * efectivo/tarjeta/transferencia — a diferencia de
     * {@code registrarMovimientoSiHayAbierta}, aquí la ausencia de caja abierta
     * sí debe impedir la operación (ver {@code CajaNoAbiertaException}).
     */
    boolean hayAbiertaPorTienda(Long tiendaId);
}
