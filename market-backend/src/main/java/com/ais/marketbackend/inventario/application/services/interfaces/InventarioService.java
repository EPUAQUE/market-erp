package com.ais.marketbackend.inventario.application.services.interfaces;

import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.dtos.MovimientoInventarioResumen;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.shared.domain.Pagina;
import java.math.BigDecimal;
import java.util.List;

/**
 * Único punto de entrada para mutar inventario. Futuros módulos (Compras, Ventas,
 * Traslados) deben llamar a {@code registrarMovimiento} — nunca escribir
 * directamente en la tabla {@code inventario}.
 */
public interface InventarioService {

    InventarioResumen registrarMovimiento(
            Long tiendaId, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario, TipoMovimiento tipoMovimiento);

    InventarioResumen obtener(Long tiendaId, Long productoId);

    /** Sin paginar — uso interno (ej. agregados del dashboard). El endpoint público usa la variante paginada. */
    List<InventarioResumen> listarPorTienda(Long tiendaId);

    Pagina<InventarioResumen> listarPorTienda(Long tiendaId, int pagina, int tamano);

    /** Sin paginar — uso interno (ej. agregados del dashboard). El endpoint público usa la variante paginada. */
    List<MovimientoInventarioResumen> listarMovimientos(Long tiendaId, Long productoId);

    Pagina<MovimientoInventarioResumen> listarMovimientos(Long tiendaId, Long productoId, int pagina, int tamano);
}
