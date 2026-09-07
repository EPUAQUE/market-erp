package com.ais.marketbackend.inventario.api.mappers;

import com.ais.marketbackend.compras.application.services.interfaces.CompraService;
import com.ais.marketbackend.inventario.api.dtos.responses.ExistenciaTiendaResponse;
import com.ais.marketbackend.inventario.api.dtos.responses.IngresoTiendaResponse;
import com.ais.marketbackend.inventario.api.dtos.responses.InventarioResponse;
import com.ais.marketbackend.inventario.api.dtos.responses.MovimientoInventarioResponse;
import com.ais.marketbackend.inventario.application.dtos.ExistenciaTiendaResumen;
import com.ais.marketbackend.inventario.application.dtos.IngresoTiendaResumen;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.dtos.MovimientoInventarioResumen;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.proveedores.application.services.interfaces.ProveedorService;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct) — necesita formatear BigDecimal a String con
 * toPlainString(). {@code compraService}/{@code proveedorService} resuelven el
 * nombre del proveedor solo para el kardex de un movimiento COMPRA — viven aquí
 * (capa API) y no en {@code InventarioServiceImpl} porque Compras ya depende de
 * Inventario; inyectarlos ahí crearía un ciclo de beans.
 */
@Component
public class InventarioApiMapper {

    private final CompraService compraService;
    private final ProveedorService proveedorService;

    public InventarioApiMapper(CompraService compraService, ProveedorService proveedorService) {
        this.compraService = compraService;
        this.proveedorService = proveedorService;
    }

    public InventarioResponse toResponse(InventarioResumen resumen) {
        return InventarioResponse.builder()
                .id(resumen.id())
                .tiendaId(resumen.tiendaId())
                .productoId(resumen.productoId())
                .existenciaActual(toPlainString(resumen.existenciaActual()))
                .costoPromedioActual(toPlainString(resumen.costoPromedioActual()))
                .build();
    }

    public ExistenciaTiendaResponse toResponse(ExistenciaTiendaResumen resumen) {
        return ExistenciaTiendaResponse.builder()
                .tiendaId(resumen.tiendaId())
                .tiendaNombre(resumen.tiendaNombre())
                .existenciaActual(toPlainString(resumen.existenciaActual()))
                .build();
    }

    public MovimientoInventarioResponse toResponse(MovimientoInventarioResumen resumen) {
        String proveedorNombre = resumen.tipoMovimiento() == TipoMovimiento.COMPRA
                ? resolverProveedorNombre(resumen.tiendaId(), resumen.origenId())
                : null;
        return MovimientoInventarioResponse.builder()
                .id(resumen.id())
                .fecha(resumen.fecha())
                .tiendaId(resumen.tiendaId())
                .productoId(resumen.productoId())
                .cantidad(toPlainString(resumen.cantidad()))
                .costoUnitario(toPlainString(resumen.costoUnitario()))
                .tipoMovimiento(resumen.tipoMovimiento().name())
                .proveedorNombre(proveedorNombre)
                .build();
    }

    /** {@code resumen.compraId()} es siempre un movimiento COMPRA (ver {@code InventarioServiceImpl.listarUltimosIngresosPorGrupo}). */
    public IngresoTiendaResponse toResponse(IngresoTiendaResumen resumen) {
        return IngresoTiendaResponse.builder()
                .tiendaId(resumen.tiendaId())
                .tiendaNombre(resumen.tiendaNombre())
                .fecha(resumen.fecha())
                .cantidad(toPlainString(resumen.cantidad()))
                .costoUnitario(toPlainString(resumen.costoUnitario()))
                .proveedorNombre(resolverProveedorNombre(resumen.tiendaId(), resumen.compraId()))
                .build();
    }

    /** Solo resuelve si hay un origen (id de compra) conocido — cualquier otro caso queda en null. */
    private String resolverProveedorNombre(Long tiendaId, Long compraId) {
        if (compraId == null) {
            return null;
        }
        try {
            Long proveedorId = compraService.obtener(tiendaId, compraId).proveedorId();
            return proveedorService.obtener(proveedorId).map(p -> p.nombre()).orElse(null);
        } catch (ResourceNotFoundException compraOProveedorYaNoExiste) {
            return null;
        }
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
