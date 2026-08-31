package com.ais.marketbackend.cuentasporcobrar.application.services.interfaces;

import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
import com.ais.marketbackend.shared.domain.Pagina;
import java.math.BigDecimal;
import java.util.List;

public interface CuentaPorCobrarService {

    /** Usado por otros módulos (Ventas) al completar una venta — no se expone como creación manual vía API. */
    CuentaPorCobrarResumen crear(Long ventaId, Long clienteId, Long tiendaId, BigDecimal montoOriginal);

    CuentaPorCobrarResumen registrarCobro(Long tiendaId, Long id, BigDecimal monto, MetodoPago metodoPago);

    CuentaPorCobrarResumen anular(Long tiendaId, Long id);

    CuentaPorCobrarResumen obtener(Long tiendaId, Long id);

    /**
     * Fase 11 (PLAN_MEJORAS.md): reemplaza el patrón O(n) de pedir
     * {@code listarPorTienda} completo y filtrar por {@code ventaId} en el
     * cliente. Lanza {@code ResourceNotFoundException} si la venta no tiene
     * cuenta por cobrar (caso normal, ej. venta al contado) — mismo criterio
     * que {@code CajaService.obtenerAbierta}.
     */
    CuentaPorCobrarResumen obtenerPorVenta(Long tiendaId, Long ventaId);

    /** Sin paginar — uso interno (ej. agregados del dashboard). El endpoint público usa la variante paginada. */
    List<CuentaPorCobrarResumen> listarPorTienda(Long tiendaId);

    Pagina<CuentaPorCobrarResumen> listarPorTienda(Long tiendaId, int pagina, int tamano);
}
