package com.ais.marketbackend.caja.application.services.interfaces;

import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.shared.domain.Pagina;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CajaService {

    CajaSesionResumen abrir(Long tiendaId, BigDecimal montoInicial);

    /**
     * {@code correlationId} es opcional. Un reintento con el mismo monto inicial bajo
     * la misma clave (tienda, correlationId) devuelve la sesión ya creada tal cual en
     * vez de fallar con {@code CajaSesionAbiertaException}; el mismo correlationId con
     * un monto distinto lanza {@code CorrelationIdReutilizadoException} (409).
     */
    CajaSesionResumen abrir(Long tiendaId, BigDecimal montoInicial, String correlationId);

    CajaSesionResumen registrarMovimiento(Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto);

    /**
     * {@code correlationId} es opcional, con la misma semántica de idempotencia que
     * {@link #abrir(Long, BigDecimal, String)}, resuelta contra los movimientos ya
     * registrados en la caja abierta actual.
     */
    CajaSesionResumen registrarMovimiento(
            Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto, String correlationId);

    CajaSesionResumen cerrar(Long tiendaId, BigDecimal montoFinalContado);

    /**
     * {@code correlationId} es opcional, misma semántica de idempotencia — incluso
     * después de que la caja ya haya quedado {@code CERRADA} por el intento anterior.
     */
    CajaSesionResumen cerrar(Long tiendaId, BigDecimal montoFinalContado, String correlationId);

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
