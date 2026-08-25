package com.ais.marketbackend.productos.api.mappers;

import com.ais.marketbackend.productos.api.dtos.responses.ProductoTiendaResponse;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct): necesita formatear {@code BigDecimal} a
 * {@code String} con {@code toPlainString()}, algo que el generador de MapStruct
 * no hace por defecto sin una expresión custom — más simple escribirlo a mano.
 */
@Component
public class ProductoTiendaApiMapper {

    public ProductoTiendaResponse toResponse(ProductoTiendaResumen resumen) {
        return ProductoTiendaResponse.builder()
                .id(resumen.id())
                .productoId(resumen.productoId())
                .tiendaId(resumen.tiendaId())
                .precioVenta(toPlainString(resumen.precioVenta()))
                .stockMinimo(toPlainString(resumen.stockMinimo()))
                .stockMaximo(toPlainString(resumen.stockMaximo()))
                .permitirVenta(resumen.permitirVenta())
                .permitirIngreso(resumen.permitirIngreso())
                .activo(resumen.activo())
                .build();
    }

    private String toPlainString(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
